/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.quarkus.logging.Log;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides a mechanism to track secrets
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 *
 * JOSDK TODO: should watch via owner references instead?
 */
public class WatchedSecrets {
    public static final String COMPONENT = "secrets-store";
    public static final String WATCHED_SECRETS_LABEL_VALUE = "watched-secret";
    public static final String STORE_SUFFIX = "-" + COMPONENT;

    private Keycloak keycloak;
    private KubernetesClient client;

    // key is name of the secret
    private final List<Secret> currentSecrets;

    public WatchedSecrets(List<String> desiredWatchedSecretsNames, KubernetesClient client, Keycloak kc, StatefulSet baseDeployment, Context<?> context) {
        this.client = client;
        // instead of being proactive in removing the old store, we'll just let it get cleaned up via the owner reference
        // client.resources(Secret.class).inNamespace(kc.getMetadata().getNamespace()).withName(kc.getMetadata().getName() + STORE_SUFFIX);
        this.keycloak = kc;
        currentSecrets = fetchCurrentSecrets(desiredWatchedSecretsNames, context);
        baseDeployment.getMetadata().getAnnotations().put(Constants.KEYCLOAK_WATCHING_ANNOTATION, desiredWatchedSecretsNames.stream().collect(Collectors.joining(";")));
        // this will trigger a rolling update if it is different
        baseDeployment.getSpec().getTemplate().getMetadata().getAnnotations().put(Constants.KEYCLOAK_WATCHED_SECRET_HASH_ANNOTATION, getSecretHash());
    }

    public void addLabelsToWatchedSecrets() {
        for (Secret secret : currentSecrets) {
            if (!secret.getMetadata().getLabels().containsKey(Constants.KEYCLOAK_COMPONENT_LABEL)) {
                Log.infof("Adding label to Secret \"%s\"", secret.getMetadata().getName());

                client.resource(secret)
                        .edit(s -> new SecretBuilder(s).editMetadata()
                                .addToLabels(Constants.KEYCLOAK_COMPONENT_LABEL, WATCHED_SECRETS_LABEL_VALUE)
                                .endMetadata().build());
            }
        }
    }

    public String getSecretHash() {
        try {
            // using hashes as it's more robust than resource versions that can change e.g. just when adding a label
            var messageDigest = MessageDigest.getInstance("MD5");

            currentSecrets.stream().map(s -> Serialization.asYaml(s.getData()).getBytes(StandardCharsets.UTF_8))
                    .forEach(s -> messageDigest.update(s));

            return new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Secret> fetchCurrentSecrets(List<String> secretsNames, Context<?> context) {
        String namespace = keycloak.getMetadata().getNamespace();
        return secretsNames.stream()
                .map(n -> OperatorManagedResource.fetch(context, Secret.class, n, namespace)
                        .orElseGet(() -> client.secrets().inNamespace(namespace).withName(n).require()))
                .collect(Collectors.toList());
    }

    private static void cleanObsoleteLabelFromSecret(KubernetesClient client, Secret secret) {
        client.secrets().inNamespace(secret.getMetadata().getNamespace()).withName(secret.getMetadata().getName())
                .edit(s -> new SecretBuilder(s)
                        .editMetadata()
                        .removeFromLabels(Constants.KEYCLOAK_COMPONENT_LABEL)
                        .endMetadata()
                        .build()
                );
    }

    public static EventSource getWatchedSecretsEventSource(KubernetesClient client, String namespace) {
        InformerConfiguration<Secret> informerConfiguration = InformerConfiguration
                .from(Secret.class)
                .withLabelSelector(Constants.KEYCLOAK_COMPONENT_LABEL + "=" + WATCHED_SECRETS_LABEL_VALUE)
                .withNamespaces(namespace)
                .withSecondaryToPrimaryMapper(secret -> {
                    // JOSDK TODO - should this handler have operations like lookup or deletes, or should that be deferred to something else

                    // JOSDK TODO - this should be replacable by a primary cache lookup, but don't see a way to get primary resources
                    var statefulSets = client.resources(StatefulSet.class).inNamespace(namespace).withLabels(Constants.DEFAULT_LABELS).list().getItems();
                    // find all CR names that are watching this Secret
                    var ret = statefulSets.stream()
                            // check if any of the statefulsets tracks this secret
                            .filter(ss -> Arrays
                                    .asList(ss.getMetadata().getAnnotations()
                                            .getOrDefault(Constants.KEYCLOAK_WATCHING_ANNOTATION, "").split(";"))
                                    .contains(secret.getMetadata().getName()))
                            .map(ss -> {
                                String crName = ss.getMetadata().getName();
                                return new ResourceID(crName, namespace);
                            })
                            .collect(Collectors.toSet());

                    if (ret.isEmpty()) {
                        Log.infof("No CRs watching \"%s\" Secret, cleaning up labels", secret.getMetadata().getName());
                        cleanObsoleteLabelFromSecret(client, secret);
                        Log.debug("Labels removed");
                    }

                    return ret;
                })
                .withOnUpdateFilter(new MetadataAwareOnUpdateFilter<>())
                .build();

        return new InformerEventSource<>(informerConfiguration, client);
    }
}
