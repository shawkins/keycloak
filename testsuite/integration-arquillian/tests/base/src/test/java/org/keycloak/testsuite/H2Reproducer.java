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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class H2Reproducer extends AbstractKeycloakTest {
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {}

    @Test
    public void test1() throws Exception {
        int startingRealmCount = adminClient.realms().findAll().size();

        final int realmCount = 1;
        boolean errored = false;
        for (int i = 0; i < realmCount; i++) {
            RealmRepresentation realm = RealmBuilder.create().name("realm-" + i).build();
            adminClient.realms().create(realm);

            var preRealms = adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.toSet());
            //suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().stop();
//            pause(3000);

//            String dbPath = Path.of(System.getProperty("auth.server.home") + "/data/h2/keycloakdb").normalize().toAbsolutePath().toString();
//            try (Connection connection = DriverManager.getConnection("jdbc:h2:" + dbPath, "sa", "password")) {
//                try (Statement statement = connection.createStatement()) {
//                    try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM REALM")) {
//                        resultSet.next();
//                        assertEquals("Realms count in DB between restarts did not match", expectedRealmCount, resultSet.getInt("count"));
//                    }
//                }
//            }

            //suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().start();
            //reconnectAdminClient();
            var postRealms = adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.toSet());
            log.info("Post realms: " + postRealms);
            if (!preRealms.equals(postRealms) || (startingRealmCount + i + 1) != postRealms.size()) {
                if (errored) {
                    throw new AssertionError("Realms do not match " + preRealms + " " + postRealms);
                }
                System.out.println("Realms do not match " + preRealms + " " + postRealms);
                errored = true;
            }
        }
    }

    // @Test
    // Does not reproduce the issue
    public void test2() throws Exception {
        final int realmCount = 50;
        for (int i = 0; i < realmCount; i++) {
            RealmRepresentation realm = RealmBuilder.create().name("realm-" + i).build();
            adminClient.realms().create(realm);
        }

        for (int i = 0; i < 50; i++) {
            log.info("Pre-restart realms: " + adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().stop();
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().start();
            reconnectAdminClient();
            List<RealmRepresentation> realms = adminClient.realms().findAll();
            log.info("Post-restart realms: " + realms.stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
            assertEquals(realmCount + 1, realms.size());
        }
    }
}
