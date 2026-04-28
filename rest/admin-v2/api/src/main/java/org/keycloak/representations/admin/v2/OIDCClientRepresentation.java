package org.keycloak.representations.admin.v2;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.keycloak.representations.admin.v2.validation.ClientSecretNotBlank;
import org.keycloak.representations.admin.v2.validation.PutClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class OIDCClientRepresentation extends BaseClientRepresentation {
    public static final String PROTOCOL = "openid-connect";

    public static final String LOGIN_FLOWS_FIELD = "loginFlows";
    public static final String AUTH_FIELD = "auth";
    public static final String WEB_ORIGINS_FIELD = "webOrigins";
    public static final String SERVICE_ACCOUNT_ROLES_FIELD = "serviceAccountRoles";

    public enum Flow {
        STANDARD,
        IMPLICIT,
        DIRECT_GRANT,
        SERVICE_ACCOUNT,
        TOKEN_EXCHANGE,
        DEVICE,
        CIBA
    }
    
    public OIDCClientRepresentation() {
        
    }
    
    public OIDCClientRepresentation(FieldMapper mapper) {
        super(mapper);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Login flows that are enabled for this client")
    public Set<Flow> getLoginFlows() {
        return this.mapper.get(LOGIN_FLOWS_FIELD);
    }

    public void setLoginFlows(Set<Flow> loginFlows) {
        this.mapper.set(LOGIN_FLOWS_FIELD, loginFlows);
    }

    @JsonMerge
    @Valid
    @JsonPropertyDescription("Authentication configuration for this client")
    public Auth getAuth() {
        return this.mapper.get(AUTH_FIELD);
    }

    public void setAuth(Auth auth) {
        this.mapper.set(AUTH_FIELD, auth);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Web origins that are allowed to make requests to this client")
    public Set<@NotBlank String> getWebOrigins() {
        return this.mapper.get(WEB_ORIGINS_FIELD);
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.mapper.set(WEB_ORIGINS_FIELD, webOrigins);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Roles assigned to the service account")
    public Set<@NotBlank String> getServiceAccountRoles() {
        return this.mapper.get(SERVICE_ACCOUNT_ROLES_FIELD);
    }

    public void setServiceAccountRoles(Set<String> serviceAccountRoles) {
        this.mapper.set(SERVICE_ACCOUNT_ROLES_FIELD, serviceAccountRoles);
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @ClientSecretNotBlank(groups = PutClient.class)
    public static class Auth extends BaseRepresentation {

        @JsonPropertyDescription("Which authentication method is used for this client")
        private String method;

        @JsonPropertyDescription("Secret used to authenticate this client with Secret authentication")
        private String secret;

        @JsonPropertyDescription("Public key used to authenticate this client with Signed JWT authentication")
        private String certificate;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

    }

}
