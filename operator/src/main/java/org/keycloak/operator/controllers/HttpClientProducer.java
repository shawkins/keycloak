package org.keycloak.operator.controllers;

import javax.net.ssl.TrustManager;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class HttpClientProducer {

    @Produces
    HttpClient getHttpClient() throws Exception {
        HttpClient.Factory factory = HttpClientUtils.getHttpClientFactory();
        HttpClient.Builder builder = factory.newBuilder();
        TrustManager[] trustAll = SSLUtils.trustManagers(null, null, true, null, null);
        builder.sslContext(null, trustAll);
        return builder.build();
    }
    
}
