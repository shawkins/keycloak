package org.keycloak.tests.welcomepage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.WelcomePage;
import org.keycloak.theme.ResourceLoader;
import org.openqa.selenium.WebDriver;

@KeycloakIntegrationTest(config = TraversalTest.Config.class)
public class TraversalTest {

    // force the creation of a new server
    static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("http-access-log-enabled", "true");
        }
    }

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    WelcomePage welcomePage;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testTraversal() throws ClientProtocolException, IOException {
        welcomePage.navigateTo();
        String body = driver.getPageSource();

        Pattern p = Pattern.compile("\\/resources\\/([^/]*)\\/");
        var m = p.matcher(body);
        m.find();
        String version = m.group(1);

        String resourcePath = "/resources/%s/common/keycloak/vendor/patternfly-v5/patternfly.min.css".formatted(version);
        int found = getResourcePath(resourcePath);
        int notFound1 = getResourcePath("/resources/"+version+"/common/keycloak/..%255C..%255C..%255C..%255Cnone-security-profile.json");
        int notFound2 = getResourcePath("/resources/"+version+"/common/keycloak/..%252F..%252F..%252F..%252Fnone-security-profile.json");
        int notFound3 = getResourcePath("/resources/"+version+"/common/keycloak/..%5C..%5C..%5C..%5Cnone-security-profile.json");
        int notFound4 = getResourcePath("/resources/"+version+"/common/keycloak/..%2F..%2F..%2F..%2Fnone-security-profile.json");

        String root = "theme/keycloak/common/resources/";
        String resource = "vendor/patternfly-v5/patternfly.min.css";

        try {
            System.out.println(ResourceLoader.getResourceAsStream(root, resource));
        } catch (IOException e) {
        }

        resource = "f/../\\none-security-profile.json";
        try {
            System.out.println(ResourceLoader.getResourceAsStream(root, resource));
        } catch (IOException e) {
        }

        resource = "f/..\\//\\../none-security-profile.json";
        try {
            System.out.println(ResourceLoader.getResourceAsStream(root, resource));
        } catch (IOException e) {
        }

        assertTrue(found == 200 && notFound1 == 404 && notFound2 == 404 && notFound3 == 404 && notFound4 == 404, found + " " + notFound1 + " " + notFound2 + " " + notFound3 + " " + notFound4);
    }

    private int getResourcePath(String resourcePath) throws IOException, ClientProtocolException {
        HttpGet httpGet = new HttpGet(keycloakUrls.getBaseUrl().toString() + resourcePath);
        try (CloseableHttpResponse getResponse = httpClient.execute(httpGet)) {
            return getResponse.getStatusLine().getStatusCode();
        }
    }

    public static void main(String[] args) {
        System.out.println(Path.of("/root/" + "\\\\/../none-security-profile.json").normalize());

        System.out.println(Path.of("/root/\\/..\\none-security-profile.json".replace("\\", "/")).normalize());
    }

}
