/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite;

import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class VaseksTest extends AbstractKeycloakTest {
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {}

    @Test
    public void test() throws Exception {
        final int realmCount = 50;
        for (int i = 0; i < realmCount; i++) {
            RealmRepresentation realm = RealmBuilder.create().name("realm-" + i).build();
            adminClient.realms().create(realm);

            log.info("Pre-restart realms: " + adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().stop();
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().start();
            reconnectAdminClient();
            List<RealmRepresentation> realms = adminClient.realms().findAll();
            log.info("Post-restart realms: " + realms.stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
            assertEquals(i + 2, realms.size());
        }
    }
}
