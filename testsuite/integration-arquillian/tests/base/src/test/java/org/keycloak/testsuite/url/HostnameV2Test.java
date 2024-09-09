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

package org.keycloak.testsuite.url;

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.testsuite.arquillian.containers.RemoteContainer;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;

/**
 * This is testing just the V2 implementation of Hostname SPI. It is NOT testing if the Hostname SPI as such is used correctly.
 * It is NOT testing that correct URL types are used at various places in Keycloak.
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class HostnameV2Test extends AbstractKeycloakTest {
    private static final String realmFrontendName = "frontendUrlRealm";
    private static final String realmFrontendUrl = "https://realmFrontend.127.0.0.1.nip.io:445";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation customHostname = RealmBuilder.create().name(realmFrontendName)
                .attribute("frontendUrl", realmFrontendUrl)
                .build();
        testRealms.add(customHostname);
    }

    @Test
    public void testFixedFrontendHostname() {
        String hostname = "127.0.0.1.nip.io";
        String dynamicUrl = getDynamicBaseUrl(hostname);

        updateServerHostnameSettings(hostname, null, false, true);

        testFrontendAndBackendUrls("master", dynamicUrl, dynamicUrl);
        testAdminUrls("master", dynamicUrl, dynamicUrl);
    }

    @Test
    public void testFixedFrontendHostnameUrl() {
        String fixedUrl = "https://127.0.0.1.nip.io:444";

        updateServerHostnameSettings(fixedUrl, null, false, true);

        testFrontendAndBackendUrls("master", fixedUrl, fixedUrl);
        testAdminUrls("master", fixedUrl, fixedUrl);
    }

    @Test
    public void testFixedFrontendAndAdminHostnameUrl() {
        String fixedFrontendUrl = "http://127.0.0.1.nip.io:444";
        String fixedAdminUrl = "https://admin.127.0.0.1.nip.io:445";

        updateServerHostnameSettings(fixedFrontendUrl, fixedAdminUrl, false, true);

        testFrontendAndBackendUrls("master", fixedFrontendUrl, fixedFrontendUrl);
        testAdminUrls("master", fixedFrontendUrl, fixedAdminUrl);
    }

    @Test
    public void testFixedFrontendHostnameUrlWithDefaultPort() {
        String fixedFrontendUrl = "https://127.0.0.1.nip.io";
        String fixedAdminUrl = "https://admin.127.0.0.1.nip.io";

        updateServerHostnameSettings("https://127.0.0.1.nip.io:443", "https://admin.127.0.0.1.nip.io:443", false, true);

        testFrontendAndBackendUrls("master", fixedFrontendUrl, fixedFrontendUrl);
        testAdminUrls("master", fixedFrontendUrl, fixedAdminUrl);
    }

    @Test
    public void testDynamicBackend() {
        String fixedUrl = "https://127.0.0.1.nip.io:444";

        updateServerHostnameSettings(fixedUrl, null, true, true);

        testFrontendAndBackendUrls("master", fixedUrl, AUTH_SERVER_ROOT);
        testAdminUrls("master", fixedUrl, fixedUrl);
    }

    @Test
    public void testDynamicEverything() {
        updateServerHostnameSettings(null, null, false, false);

        testFrontendAndBackendUrls("master", AUTH_SERVER_ROOT, AUTH_SERVER_ROOT);
        testAdminUrls("master", AUTH_SERVER_ROOT, AUTH_SERVER_ROOT);
    }

    @Test
    public void testRealmFrontendUrlWithOtherUrlsSet() {
        String fixedFrontendUrl = "https://127.0.0.1.nip.io:444";
        String fixedAdminUrl = "https://admin.127.0.0.1.nip.io:445";

        updateServerHostnameSettings(fixedFrontendUrl, fixedAdminUrl, true, true);

        testFrontendAndBackendUrls(realmFrontendName, realmFrontendUrl, AUTH_SERVER_ROOT);
        testAdminUrls(realmFrontendName, realmFrontendUrl, fixedAdminUrl);
    }

    @Test
    public void testAdminLocal() throws Exception {
        updateServerHostnameSettings("https://127.0.0.1.nip.io:444", null, false, true);

        // This is a hack. AdminLocal is used only on the Welcome Screen, nowhere else. Welcome Screen by default redirects to Admin Console if admin users exists.
        // So we delete it and later recreate it.
        String adminId = adminClient.realm("master").users().search("admin").get(0).getId();
        try (Response ignore = adminClient.realm("master").users().delete(adminId)) {
            suiteContext.setAdminPasswordUpdated(false);
            try (CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build()) {
                // X-Forwarded-For is needed to trigger the correct message with a link, make Keycloak think we're not accessing it locally
                SimpleHttp get = SimpleHttpDefault.doGet(getDynamicBaseUrl("127.0.0.1.nip.io"), client).header("X-Forwarded-For", "127.0.0.1");

                String welcomePage = get.asString();
                assertThat(welcomePage, containsString("<a href=\"" + getDynamicBaseUrl("localhost") + "/\">"));
            }
        }
        finally {
            updateMasterAdminPassword();
            reconnectAdminClient();
        }
    }

    @Test
    public void testRealmFrontendUrl() {
        updateServerHostnameSettings("127.0.0.1.nip.io", null, false, true);

        testFrontendAndBackendUrls(realmFrontendName, realmFrontendUrl, realmFrontendUrl);
        testAdminUrls(realmFrontendName, realmFrontendUrl, realmFrontendUrl);
    }

    @Test
    public void testStrictMode() {
        testStartupFailure("hostname is not configured; either configure hostname, or set hostname-strict to false",
                null, null, null, true);
    }

//    @Test
//    public void testStrictModeMustBeDisabledWhenHostnameIsSpecified() {
//        testStartupFailure("hostname is configured, hostname-strict must be set to true",
//                "127.0.0.1.nip.io", null, null, false);
//    }

    @Test
    public void testInvalidHostnameUrl() {
        testStartupFailure("Provided hostname is neither a plain hostname or a valid URL",
                "htt://127.0.0.1.nip.io", null, null, true);
    }

    @Test
    public void testInvalidAdminUrl() {
        testStartupFailure("Provided hostname-admin is not a valid URL",
                "127.0.0.1.nip.io", "htt://admin.127.0.0.1.nip.io", null, true);
    }

    @Test
    public void testBackchannelDynamicRequiresHostname() {
        testStartupFailure("hostname-backchannel-dynamic must be set to false when no hostname is provided",
                null, null, true, false);
    }

    @Test
    public void testBackchannelDynamicRequiresFullHostnameUrl() {
        testStartupFailure("hostname-backchannel-dynamic must be set to false if hostname is not provided as full URL",
                "127.0.0.1.nip.io", null, true, true);
    }

    private String getDynamicBaseUrl(String hostname) {
        return AUTH_SERVER_SCHEME + "://" + hostname + ":" + AUTH_SERVER_PORT + "/auth";
    }

    private void testFrontendAndBackendUrls(String realm, String expectedFrontendUrl, String expectedBackendUrl) {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);
        assertEquals(expectedFrontendUrl + "/realms/" + realm, config.getIssuer());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/auth", config.getAuthorizationEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo", config.getUserinfoEndpoint());
    }

    private void testAdminUrls(String realm, String expectedFrontendUrl, String expectedAdminUrl) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String adminIndexPage = SimpleHttpDefault.doGet(AUTH_SERVER_ROOT + "/admin/" + realm + "/console", client).asString();
            assertThat(adminIndexPage, containsString("\"authServerUrl\": \"" + expectedFrontendUrl +"\""));
            assertThat(adminIndexPage, containsString("\"authUrl\": \"" + expectedAdminUrl +"\""));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testStartupFailure(String expectedError, String hostname, String hostnameAdmin, Boolean hostnameBackchannelDynamic, Boolean hostnameStrict) {
        String errorLog = "";
        DeployableContainer<?> container = suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();

        try {
            updateServerHostnameSettings(hostname, hostnameAdmin, hostnameBackchannelDynamic, hostnameStrict);
            Assert.fail("Server didn't fail");
        }
        catch (Exception e) {
            if (container instanceof RemoteContainer) {
                errorLog = ((RemoteContainer) container).getRemoteLog();
            }
            else {
                errorLog = ExceptionUtils.getStackTrace(e);
            }
        }

        // need to start the server back again to perform standard after test cleanup
        resetHostnameSettings();
        try {
            container.stop(); // just to make sure all components are stopped (useful for Undertow)
            container.start();
            reconnectAdminClient();
            //log.infof("Checking %s realm exists after restart, got realm ID", realmFrontendName, adminClient.realm(realmFrontendName).toRepresentation().getId());
            log.info("Post-failure restart realms: " + adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(errorLog, containsString(expectedError));
    }

    private void updateServerHostnameSettings(String hostname, String hostnameAdmin, Boolean hostnameBackchannelDynamic, Boolean hostnameStrict) {
        try {
            log.infof("Checking %s realm exists before restart, got realm ID %s", realmFrontendName, adminClient.realm(realmFrontendName).toRepresentation().getId());
            log.info("Pre-restart realms: " + adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().stop();
            setHostnameOptions(hostname, hostnameAdmin, hostnameBackchannelDynamic, hostnameStrict);
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().start();
            reconnectAdminClient();
            try {
                log.info("Post-restart realms: " + adminClient.realms().findAll().stream().map(r -> r.getId() + ":" + r.getRealm()).collect(Collectors.joining(", ")));
            }
            catch (Exception e) {
                log.error("Could not fetch post-restart realms, failed to reconnect admin client", e);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setHostnameOptions(String hostname, String hostnameAdmin, Boolean hostnameBackchannelDynamic, Boolean hostnameStrict) {
        if (suiteContext.getAuthServerInfo().isQuarkus()) {
            List<String> args = new ArrayList<>();
            if (hostname != null) {
                args.add("--hostname=" + hostname);
            }
            if (hostnameAdmin != null) {
                args.add("--hostname-admin=" + hostnameAdmin);
            }
            if (hostnameBackchannelDynamic != null) {
                args.add("--hostname-backchannel-dynamic=" + hostnameBackchannelDynamic);
            }
            if (hostnameStrict != null) {
                args.add("--hostname-strict=" + hostnameStrict);
            }

            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.setAdditionalBuildArgs(args);
        }
        else {
            setConfigProperty("keycloak.hostname", hostname);
            setConfigProperty("keycloak.hostname-admin", hostnameAdmin);
            setConfigProperty("keycloak.hostname-backchannel-dynamic", hostnameBackchannelDynamic == null ? null : String.valueOf(hostnameBackchannelDynamic));
            setConfigProperty("keycloak.hostname-strict", hostnameStrict == null ? null : String.valueOf(hostnameStrict));
        }
    }

    @After
    public void resetHostnameSettings() {
        if (suiteContext.getAuthServerInfo().isQuarkus()) {
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.resetConfiguration();
        }
        else {
            setHostnameOptions(null, null, null, null);
            setConfigProperty("keycloak.hostname.provider", null);
        }
    }

    private static void setConfigProperty(String name, String value) {
        if (value != null) {
            System.setProperty(name, value);
        }
        else {
            System.clearProperty(name);
        }
    }
}
