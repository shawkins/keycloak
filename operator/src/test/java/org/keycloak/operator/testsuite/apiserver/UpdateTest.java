/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.testsuite.apiserver;

import static org.keycloak.operator.testsuite.utils.CRAssert.eventuallyRecreateUpdateStatus;
import static org.keycloak.operator.testsuite.utils.CRAssert.eventuallyRollingUpdateStatus;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;
import org.keycloak.operator.testsuite.integration.BaseOperatorTest;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.update.UpdateStrategy;

import io.fabric8.kubeapitest.KubeAPIServer;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UpdateTest {

    private static KubeAPIServer kubeApi;
    static KubernetesClient k8sclient;

    @BeforeAll
    static void beforeAll() throws FileNotFoundException {
        kubeApi = new KubeAPIServer();
        kubeApi.start();
        k8sclient = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeApi.getKubeConfigYaml()))
                .build();
        BaseOperatorTest.createCRDs(k8sclient);
    }

    @AfterAll
    static void afterAll() {
        kubeApi.stop();
    }

    @Test
    public void testExplicitStrategy() throws InterruptedException, ExecutionException, TimeoutException {
        var operator = BaseOperatorTest.createOperator(k8sclient);
        operator.start();

        var kc = createInitialDeployment(UpdateStrategy.EXPLICIT);

        var updateCondition = assertUnknownUpdateTypeStatus(kc);

        // fake statefulset controller
        k8sclient.apps().statefulSets().withName(KeycloakDeploymentDependentResource.getName(kc))
                .inform(new ResourceEventHandler<StatefulSet>() {

            @Override
            public void onAdd(StatefulSet obj) {
                updateStatefulSet(obj);
            }

            private void updateStatefulSet(StatefulSet obj) {
                int replicas = obj.getSpec().getReplicas();
                String revision = String.valueOf(Math.abs(obj.getSpec().hashCode()));
                if (!Objects.equals(Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0), replicas)
                        || !Objects.equals(revision, obj.getStatus().getUpdateRevision())
                        || !Objects.equals(obj.getStatus().getCurrentRevision(), obj.getStatus().getUpdateRevision())) {
                    // generate intermediate rolling events
                    int actualReplicas;
                    if (!Objects.equals(revision, obj.getStatus().getUpdateRevision())) {
                        actualReplicas = 0;
                    } else if (replicas == 0) {
                        actualReplicas = Math.max(replicas, Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0) - 1);
                    } else {
                        actualReplicas = Math.min(replicas, Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0) + 1);
                    }
                    obj = k8sclient.getKubernetesSerialization().clone(obj);
                    obj.getStatus().setReplicas(actualReplicas);
                    obj.getStatus().setReadyReplicas(actualReplicas);
                    obj.getStatus().setUpdateRevision(revision);
                    if (actualReplicas == replicas) {
                        obj.getStatus().setCurrentRevision(revision);
                    }
                    obj.getMetadata().setResourceVersion(null);
                    k8sclient.resource(obj).updateStatus();
                }
            }

            @Override
            public void onUpdate(StatefulSet oldObj, StatefulSet newObj) {
                updateStatefulSet(newObj);
            }

            @Override
            public void onDelete(StatefulSet obj, boolean deletedFinalStateUnknown) {

            }

        });

        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        updateCondition = eventuallyRecreateUpdateStatus(k8sclient, kc, "does not match");
        // update configuration, revision is updated
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("cache-embedded-authorization-max-count", "10")));
        kc.getSpec().getUpdateSpec().setRevision("1");
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        updateCondition = eventuallyRecreateUpdateStatus(k8sclient, kc,
                "Explicit strategy configured. Revision (1) does not match (2).");
        // update configuration, revision is updated
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("cache-embedded-authorization-max-count", "11")));
        kc.getSpec().getUpdateSpec().setRevision("2");
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // update configuration, revision is unchanged
        updateCondition = eventuallyRollingUpdateStatus(k8sclient, kc, "Explicit strategy configured. Revision matches.");
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("cache-embedded-authorization-max-count", "12")));
        kc.getSpec().getUpdateSpec().setRevision("2");
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);
    }

    private CompletableFuture<?> assertUnknownUpdateTypeStatus(Keycloak keycloak) {
        return k8sclient.resource(keycloak).informOnCondition(kcs -> {
            if (kcs.isEmpty() || kcs.get(0).getStatus() == null) {
                return false;
            }
            try {
                CRAssert.assertKeycloakStatusCondition(kcs.get(0), KeycloakStatusCondition.UPDATE_TYPE, null);
                return true;
            } catch (AssertionError e) {
                return false;
            }
        });
    }

    private static Keycloak createInitialDeployment(UpdateStrategy updateStrategy) {
        var kc = BaseOperatorTest.getTestKeycloakDeployment(false);
        kc.getSpec().setInstances(2);
        var updateSpec = new UpdateSpec();
        updateSpec.setStrategy(updateStrategy);
        if (updateStrategy == UpdateStrategy.EXPLICIT) {
            updateSpec.setRevision("0");
        }
        kc.getSpec().setUpdateSpec(updateSpec);
        return kc;
    }

    private static <T> void await(CompletableFuture<T> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        future.get(2, TimeUnit.MINUTES);
    }
}
