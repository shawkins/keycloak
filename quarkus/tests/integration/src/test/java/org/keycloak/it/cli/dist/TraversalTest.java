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

package org.keycloak.it.cli.dist;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.theme.ResourceLoader;

import io.quarkus.test.junit.main.Launch;
import io.restassured.RestAssured;

@WithEnvVars({"KC_CACHE", "local"}) // avoid flakey port conflicts
@DistributionTest(keepAlive = true,
        requestPort = 8080,
        containerExposedPorts = {8080})
@Tag(DistributionTest.WIN)
public class TraversalTest {

    @RawDistOnly(reason = "Containers are immutable")
    @Test
    @Launch({ "start-dev", "--http-access-log-enabled=true" })
    void testResourceTraversal(CLIResult cliResult) {
        RestAssured.urlEncodingEnabled = false;
        String body = when().get("/").body().asString();

        Pattern p = Pattern.compile("\\/resources\\/([^/]*)\\/");
        var m = p.matcher(body);
        m.find();
        String version = m.group(1);

        int found = when().get("/resources/%s/common/keycloak/vendor/patternfly-v5/patternfly.min.css".formatted(version)).getStatusCode();
        int notFound1 = when().get("/resources/"+version+"/common/keycloak/..%255C..%255C..%255C..%255Cnone-security-profile.json").getStatusCode();
        int notFound2 = when().get("/resources/"+version+"/common/keycloak/..%252F..%252F..%252F..%252Fnone-security-profile.json").getStatusCode();
        int notFound3 = when().get("/resources/"+version+"/common/keycloak/..%5C..%5C..%5C..%5Cnone-security-profile.json").getStatusCode();
        int notFound4 = when().get("/resources/"+version+"/common/keycloak/..%2F..%2F..%2F..%2Fnone-security-profile.json").getStatusCode();

        String root = "theme/keycloak/common/resources/";
        String resource = "vendor/patternfly-v5/patternfly.min.css";

        try {
            System.out.println(ResourceLoader.getResourceAsStream(root, resource));
        } catch (IOException e) {
        }

        resource = "../../../../none-security-profile.json";
        try {
            System.out.println(ResourceLoader.getResourceAsStream(root, resource));
        } catch (IOException e) {
        }

        assertTrue(found == 200 && notFound1 == 404 && notFound2 == 404 && notFound3 == 404 && notFound4 == 404, found + " " + notFound1 + " " + notFound2 + " " + notFound3 + " " + notFound4);
    }
}
