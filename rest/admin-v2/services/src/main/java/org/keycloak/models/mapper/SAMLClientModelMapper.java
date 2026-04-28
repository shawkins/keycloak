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

package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.FieldMapper;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

/**
 * Mapper for SAML clients between model and representation.
 */
public class SAMLClientModelMapper extends BaseClientModelMapper {

    // SAML attribute keys
    private static final String SAML_NAME_ID_FORMAT = "saml_name_id_format";
    private static final String SAML_FORCE_NAME_ID_FORMAT = "saml_force_name_id_format";
    private static final String SAML_AUTHN_STATEMENT = "saml.authnstatement";
    private static final String SAML_SERVER_SIGNATURE = "saml.server.signature";
    private static final String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    private static final String SAML_CLIENT_SIGNATURE = "saml.client.signature";
    private static final String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    private static final String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    private static final String SAML_SIGNATURE_CANONICALIZATION = "saml_signature_canonicalization_method";
    private static final String SAML_SIGNING_CERTIFICATE = "saml.signing.certificate";
    private static final String SAML_ALLOW_ECP_FLOW = "saml.allow.ecp.flow";

    @Override
    protected SAMLClientRepresentation createClientRepresentation(FieldMapper mapper) {
        return new SAMLClientRepresentation(mapper);
    }
    
    protected void addBooleanAttributeMapping(String name, String attribute) {
        this.addMapping(name, model -> getBooleanAttribute(model, attribute), (model, value) -> setBooleanAttributeIfNotNull(model, attribute, value));
    }
    
    protected void addAttributeMapping(String name, String attribute) {
        this.addMapping(name, model -> model.getAttribute(attribute), (model, value) -> setAttributeIfNotNull(model, attribute, value));
    }
    
    public SAMLClientModelMapper() {
        // TODO: protocol may need to be a proper field
        addMapping(BaseClientRepresentation.DISCRIMINATOR_FIELD, null, (model, protocol) -> model.setProtocol((String)protocol));
        
        // Name ID settings
        addAttributeMapping(SAMLClientRepresentation.NAME_ID_FORMAT_FIELD, SAML_NAME_ID_FORMAT);
        addBooleanAttributeMapping(SAMLClientRepresentation.FORCE_NAME_ID_FORMAT_FIELD, SAML_FORCE_NAME_ID_FORMAT);
        
        // Signature settings
        addBooleanAttributeMapping(SAMLClientRepresentation.INCLUDE_AUTHN_STATEMENT_FIELD, SAML_AUTHN_STATEMENT);
        addBooleanAttributeMapping(SAMLClientRepresentation.SIGN_DOCUMENTS_FIELD, SAML_SERVER_SIGNATURE);
        addBooleanAttributeMapping(SAMLClientRepresentation.SIGN_ASSERTIONS_FIELD, SAML_ASSERTION_SIGNATURE);
        addBooleanAttributeMapping(SAMLClientRepresentation.CLIENT_SIGNATURE_REQUIRED_FIELD, SAML_CLIENT_SIGNATURE);
        addAttributeMapping(SAMLClientRepresentation.SIGNATURE_ALGORITHM_FIELD, SAML_SIGNATURE_ALGORITHM);
        addAttributeMapping(SAMLClientRepresentation.SIGNATURE_CANONICALIZATION_METHOD_FIELD, SAML_SIGNATURE_CANONICALIZATION);
        addAttributeMapping(SAMLClientRepresentation.SIGNING_CERTIFICATE_FIELD, SAML_SIGNING_CERTIFICATE);

        // Binding and logout settings
        addBooleanAttributeMapping(SAMLClientRepresentation.FORCE_POST_BINDING_FIELD, SAML_FORCE_POST_BINDING);
        // TODO: mapping from 3 value to 2 value boolean can be confusing from a patching perspective
        addMapping(SAMLClientRepresentation.FRONT_CHANNEL_LOGOUT_FIELD, ClientModel::isFrontchannelLogout, (model, logout) -> model.setFrontchannelLogout(Boolean.TRUE.equals(logout)));

        // ECP flow
        addBooleanAttributeMapping(SAMLClientRepresentation.ALLOW_ECP_FLOW_FIELD, SAML_ALLOW_ECP_FLOW);
    }

    private Boolean getBooleanAttribute(ClientModel model, String key) {
        String value = model.getAttribute(key);
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    private void setAttributeIfNotNull(ClientModel model, String key, String value) {
        if (value != null) {
            model.setAttribute(key, value);
        }
    }

    private void setBooleanAttributeIfNotNull(ClientModel model, String key, Boolean value) {
        if (value != null) {
            model.setAttribute(key, value.toString());
        }
    }

}
