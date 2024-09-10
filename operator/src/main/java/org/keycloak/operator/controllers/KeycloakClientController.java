/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClient;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientRepresentation;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientRepresentationBuilder;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatus;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.ResourceNotFoundException;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;

@ControllerConfiguration
public class KeycloakClientController
        implements Reconciler<KeycloakClient>, Cleaner<KeycloakClient>, ErrorStatusHandler<KeycloakClient> {
    
    static class KeycloakClientStatusAggregator {
        
        KeycloakClient resource;
        KeycloakClientStatus existingStatus;
        Map<String, KeycloakClientStatusCondition> existingConditions;
        Map<String, KeycloakClientStatusCondition> newConditions;
        
        KeycloakClientStatusAggregator(KeycloakClient resource) {
            this.resource = resource;
            existingStatus = Optional.ofNullable(resource.getStatus()).orElse(new KeycloakClientStatus());
            existingConditions = KeycloakStatusAggregator.getConditionMap(existingStatus.getConditions());
        }
        
        void setCondition(String type, Boolean status, String message) {
            KeycloakClientStatusCondition condition = new KeycloakClientStatusCondition();
            condition.setStatus(status);
            condition.setMessage(message);
            newConditions.put(type, condition); // No aggregation yet
        }
        
        KeycloakClientStatus build() {
            KeycloakClientStatusBuilder statusBuilder = new KeycloakClientStatusBuilder();
            String now = Utils.iso8601Now();
            statusBuilder.withObservedGeneration(resource.getMetadata().getGeneration());
            newConditions.values().forEach(c -> KeycloakStatusAggregator.updateConditionFromExisting(c, existingConditions, now));
            existingConditions.putAll(newConditions);
            statusBuilder.withConditions(new ArrayList<>(existingConditions.values().stream().sorted(Comparator.comparing(KeycloakClientStatusCondition::getType)).toList()));
            return statusBuilder.build();
        }
        
        public KeycloakClientStatus getExistingStatus() {
            return existingStatus;
        }
        
    }

    @Inject
    Config config;
    
    // TODO: for now we're creating only a single client that trusts everything
    // eventually we may need a clients configured per keycloak
    @Inject
    HttpClient httpClient;

    @Override
    public UpdateControl<KeycloakClient> reconcile(KeycloakClient resource, Context<KeycloakClient> context)
            throws Exception {

        String kcName = resource.getSpec().getKeycloakCrName();
        
        // TODO: this should be obtained from an informer instead
        // they can't be shared directly across controllers, so we'd have to inject the
        // KeycloakController
        // and access via a reference to a saved context
        Keycloak keycloak = context.getClient().resources(Keycloak.class)
                .inNamespace(resource.getMetadata().getNamespace()).withName(kcName).require();

        KeycloakClientStatusAggregator statusAggregator = new KeycloakClientStatusAggregator(resource);

        boolean poll = false;
        // create the payload via inlining of the secret
        KeycloakClientRepresentation rep = new KeycloakClientRepresentationBuilder(resource.getSpec().getClient())
                .build();
        rep.setClientSecret(null); // remove operator specific fields
        SecretKeySelector secretSelector = resource.getSpec().getClient().getClientSecret();
        if (secretSelector != null) {
            poll = true;
            
            boolean optional = Boolean.TRUE.equals(secretSelector.getOptional());

            Secret secret = context.getClient().resources(Secret.class)
                    .inNamespace(resource.getMetadata().getNamespace()).withName(secretSelector.getName()).get();

            if (secret == null) {
                if (!optional) {
                    throw new ResourceNotFoundException(String.format("Secret %s/%s not found", resource.getMetadata().getNamespace(), secretSelector.getName()));
                }
            } else {
                String value = secret.getData().get(secretSelector.getKey());

                if (value == null) {
                    if (!optional) {
                        throw new ResourceNotFoundException(String.format("Secret key %s in %s/%s not found", secretSelector.getKey(), resource.getMetadata().getNamespace(), secretSelector.getName()));
                    }   
                } else {
                    rep.setSecret(value);
                }
            }
        }

        String hash = WatchedResources.getHash(List.of(rep), Function.identity());
        
        if (!hash.equals(statusAggregator.getExistingStatus().getHash())) {
            String url = getClientUrl(resource, keycloak); 
               
            var request = httpClient.newHttpRequestBuilder().uri(url)
                    .header(Constants.AUTHORIZATION_HEADER, hash)
                    .method("PUT", "application/json", Serialization.asJson(rep)).build();
            var responseFuture = httpClient.sendAsync(request, String.class);
            
            // if not 200 response, then could throw exception to allow the retry loop
            // however not all errors likely should get retried every 10 seconds
        }

        KeycloakClientStatus status = statusAggregator.build();
        status.setHash(hash);
        statusAggregator.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, false, null);
        UpdateControl<KeycloakClient> updateControl;

        if (status.equals(resource.getStatus())) {
            updateControl = UpdateControl.noUpdate();
        } else {
            resource.setStatus(status);
            updateControl = UpdateControl.updateStatus(resource);
        }

        if (poll) {
            updateControl.rescheduleAfter(config.keycloak().pollIntervalSeconds(), TimeUnit.SECONDS);
        }

        return updateControl;
    }

    private String getClientUrl(KeycloakClient resource, Keycloak keycloak) {
        boolean tlsConfigured = isTlsConfigured(keycloak);
        String protocol = tlsConfigured?"https":"http";
        int port = KeycloakServiceDependentResource.getServicePort(tlsConfigured, keycloak);
        return String.format("%s://%s.%s:%s/admin/realm/%s/clients/%s", protocol,
                KeycloakServiceDependentResource.getServiceName(keycloak),
                resource.getMetadata().getNamespace(), port, URLEncoder.encode(resource.getSpec().getRealm(), StandardCharsets.UTF_8),
                URLEncoder.encode(resource.getSpec().getClient().getClientId(), StandardCharsets.UTF_8));
    }

    /**
     * Uses a finalizer to ensure clients are not orphaned unless a user goes out of
     * their way to do so
     */
    @Override
    public DeleteControl cleanup(KeycloakClient resource, Context<KeycloakClient> context) {
        String kcName = resource.getSpec().getKeycloakCrName();
        
        Keycloak keycloak = context.getClient().resources(Keycloak.class)
                .inNamespace(resource.getMetadata().getNamespace()).withName(kcName).get();
        
        if (keycloak == null) {
            return DeleteControl.defaultDelete();
        }
        
        String url = getClientUrl(resource, keycloak);

        // DELETE url

        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<KeycloakClient> updateErrorStatus(KeycloakClient resource,
            Context<KeycloakClient> context, Exception e) {
        Log.error("--- Error reconciling", e);

        KeycloakClientStatusAggregator status = new KeycloakClientStatusAggregator(resource);
        status.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, true, "Error performing operations:\n" + e.getMessage());
        resource.setStatus(status.build());

        return ErrorStatusUpdateControl.updateStatus(resource).rescheduleAfter(Constants.RETRY_DURATION);
    }
    
}
