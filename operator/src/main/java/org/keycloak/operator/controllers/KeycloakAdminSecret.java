package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.reconciler.Context;

import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

// JOSDK TODO: should this be treated like the other watched secrets
public class KeycloakAdminSecret extends OperatorManagedResource<Secret, KeycloakStatusAggregator> {

    private final String secretName;

    public KeycloakAdminSecret(KubernetesClient client, Keycloak keycloak) {
        super(client, keycloak);
        this.secretName = KubernetesResourceUtil.sanitizeName(keycloak.getMetadata().getName() + "-initial-admin");
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource(Context<?> context, Secret current, KeycloakStatusAggregator statusAggregator) {
        if (current == null) {
            // not actually watching - and even if we were the cached one may be null after initial creation
            // if another event is processed first
            current = client.secrets().inNamespace(getNamespace()).withName(getName()).get();
            // there is still a very narrow possibility that we'll overwrite an existing secret - but it would
            // have be created by a another actor (multiple operators can be prohibited via leader election)
        }
        return Optional.of(createSecret(Optional.ofNullable(current).map(s -> s.getData().get("password"))));
    }

    private Secret createSecret(Optional<String> password) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(getNamespace())
                .endMetadata()
                .withType("kubernetes.io/basic-auth")
                .addToData("password",
                        password.orElseGet(() -> Base64.getEncoder().encodeToString(
                                UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8))))
                .addToData("username", Base64.getEncoder().encodeToString("admin".getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @Override
    public String getName() { return secretName; }

    @Override
    protected Class<Secret> getType() {
        return Secret.class;
    }

}
