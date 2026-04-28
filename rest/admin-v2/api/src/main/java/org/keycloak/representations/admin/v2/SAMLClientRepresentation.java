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

package org.keycloak.representations.admin.v2;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Representation of a SAML client.
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Schema(description = "SAML Client configuration")
public class SAMLClientRepresentation extends BaseClientRepresentation {
    public static final String PROTOCOL = "saml";

    public static final String NAME_ID_FORMAT_FIELD = "nameIdFormat";
    public static final String FORCE_NAME_ID_FORMAT_FIELD = "forceNameIdFormat";
    public static final String INCLUDE_AUTHN_STATEMENT_FIELD = "includeAuthnStatement";
    public static final String SIGN_DOCUMENTS_FIELD = "signDocuments";
    public static final String SIGN_ASSERTIONS_FIELD = "signAssertions";
    public static final String CLIENT_SIGNATURE_REQUIRED_FIELD = "clientSignatureRequired";
    public static final String FORCE_POST_BINDING_FIELD = "forcePostBinding";
    public static final String FRONT_CHANNEL_LOGOUT_FIELD = "frontChannelLogout";
    public static final String SIGNATURE_ALGORITHM_FIELD = "signatureAlgorithm";
    public static final String SIGNATURE_CANONICALIZATION_METHOD_FIELD = "signatureCanonicalizationMethod";
    public static final String SIGNING_CERTIFICATE_FIELD = "signingCertificate";
    public static final String ALLOW_ECP_FLOW_FIELD = "allowEcpFlow";

    public SAMLClientRepresentation() {

    }

    public SAMLClientRepresentation(FieldMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @JsonPropertyDescription("Name ID format to use for the subject (e.g., 'username', 'email', 'transient', 'persistent')")
    public String getNameIdFormat() {
        return this.mapper.get(NAME_ID_FORMAT_FIELD);
    }

    public void setNameIdFormat(String nameIdFormat) {
        this.mapper.set(NAME_ID_FORMAT_FIELD, nameIdFormat);
    }

    @JsonPropertyDescription("Force the specified Name ID format even if the client requests a different one")
    public Boolean getForceNameIdFormat() {
        return this.mapper.get(FORCE_NAME_ID_FORMAT_FIELD);
    }

    public void setForceNameIdFormat(Boolean forceNameIdFormat) {
        this.mapper.set(FORCE_NAME_ID_FORMAT_FIELD, forceNameIdFormat);
    }

    @JsonPropertyDescription("Include AuthnStatement in the SAML response")
    public Boolean getIncludeAuthnStatement() {
        return this.mapper.get(INCLUDE_AUTHN_STATEMENT_FIELD);
    }

    public void setIncludeAuthnStatement(Boolean includeAuthnStatement) {
        this.mapper.set(INCLUDE_AUTHN_STATEMENT_FIELD, includeAuthnStatement);
    }

    @JsonPropertyDescription("Sign SAML documents on the server side")
    public Boolean getSignDocuments() {
        return this.mapper.get(SIGN_DOCUMENTS_FIELD);
    }

    public void setSignDocuments(Boolean signDocuments) {
        this.mapper.set(SIGN_DOCUMENTS_FIELD, signDocuments);
    }

    @JsonPropertyDescription("Sign SAML assertions")
    public Boolean getSignAssertions() {
        return this.mapper.get(SIGN_ASSERTIONS_FIELD);
    }

    public void setSignAssertions(Boolean signAssertions) {
        this.mapper.set(SIGN_ASSERTIONS_FIELD, signAssertions);
    }

    @JsonPropertyDescription("Require client to sign SAML requests")
    public Boolean getClientSignatureRequired() {
        return this.mapper.get(CLIENT_SIGNATURE_REQUIRED_FIELD);
    }

    public void setClientSignatureRequired(Boolean clientSignatureRequired) {
        this.mapper.set(CLIENT_SIGNATURE_REQUIRED_FIELD, clientSignatureRequired);
    }

    @JsonPropertyDescription("Force POST binding for SAML responses")
    public Boolean getForcePostBinding() {
        return this.mapper.get(FORCE_POST_BINDING_FIELD);
    }

    public void setForcePostBinding(Boolean forcePostBinding) {
        this.mapper.set(FORCE_POST_BINDING_FIELD, forcePostBinding);
    }

    @JsonPropertyDescription("Use front-channel logout (browser redirect)")
    public Boolean getFrontChannelLogout() {
        return this.mapper.get(FRONT_CHANNEL_LOGOUT_FIELD);
    }

    public void setFrontChannelLogout(Boolean frontChannelLogout) {
        this.mapper.set(FRONT_CHANNEL_LOGOUT_FIELD, frontChannelLogout);
    }

    @JsonPropertyDescription("Signature algorithm for signing SAML documents (e.g., 'RSA_SHA256', 'RSA_SHA512')")
    public String getSignatureAlgorithm() {
        return this.mapper.get(SIGNATURE_ALGORITHM_FIELD);
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.mapper.set(SIGNATURE_ALGORITHM_FIELD, signatureAlgorithm);
    }

    @JsonPropertyDescription("Canonicalization method for XML signatures")
    public String getSignatureCanonicalizationMethod() {
        return this.mapper.get(SIGNATURE_CANONICALIZATION_METHOD_FIELD);
    }

    public void setSignatureCanonicalizationMethod(String signatureCanonicalizationMethod) {
        this.mapper.set(SIGNATURE_CANONICALIZATION_METHOD_FIELD, signatureCanonicalizationMethod);
    }

    @JsonPropertyDescription("X.509 certificate for signing (PEM format, without headers)")
    public String getSigningCertificate() {
        return this.mapper.get(SIGNING_CERTIFICATE_FIELD);
    }

    public void setSigningCertificate(String signingCertificate) {
        this.mapper.set(SIGNING_CERTIFICATE_FIELD, signingCertificate);
    }

    @JsonPropertyDescription("Allow ECP (Enhanced Client or Proxy) flow")
    public Boolean getAllowEcpFlow() {
        return this.mapper.get(ALLOW_ECP_FLOW_FIELD);
    }

    public void setAllowEcpFlow(Boolean allowEcpFlow) {
        this.mapper.set(ALLOW_ECP_FLOW_FIELD, allowEcpFlow);
    }

}
