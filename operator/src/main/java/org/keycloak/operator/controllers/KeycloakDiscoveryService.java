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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;

import java.util.Optional;

public class KeycloakDiscoveryService extends OperatorManagedResource<Service, KeycloakStatusAggregator> {

    private Service existingService;

    public KeycloakDiscoveryService(KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
    }

    private ServiceSpec getServiceSpec() {
      return new ServiceSpecBuilder()
              .addNewPort()
              .withPort(Constants.KEYCLOAK_DISCOVERY_SERVICE_PORT)
              .endPort()
              .withSelector(Constants.DEFAULT_LABELS)
              .withClusterIP("None")
              .build();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource(Context<?> context, Service current, KeycloakStatusAggregator statusBuilder) {
        this.existingService = current;
        updateStatus(statusBuilder);
        return Optional.of(newService());
    }

    private Service newService() {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(getName())
                .withNamespace(getNamespace())
                .endMetadata()
                .withSpec(getServiceSpec())
                .build();
        return service;
    }

    public void updateStatus(KeycloakStatusAggregator status) {
        if (existingService == null) {
            status.addNotReadyMessage("No existing Discovery Service found, waiting for creating a new one");
            return;
        }
    }

    @Override
    public String getName() {
        return cr.getMetadata().getName() + Constants.KEYCLOAK_DISCOVERY_SERVICE_SUFFIX;
    }

    @Override
    protected Class<Service> getType() {
        return Service.class;
    }
}
