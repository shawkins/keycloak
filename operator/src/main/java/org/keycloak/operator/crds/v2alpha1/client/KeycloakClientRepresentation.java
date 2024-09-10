package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.representations.idm.ClientRepresentation;

import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
// remove the secret field - TODO: this may not be necessary if a direct vault reference is allowed
@SchemaSwap(originalType = ClientRepresentation.class, fieldName = "secret") 
public class KeycloakClientRepresentation extends ClientRepresentation {
    // TODO: extend from the new representation
    
    private SecretKeySelector clientSecret;
    
    public SecretKeySelector getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(SecretKeySelector clientSecret) {
        this.clientSecret = clientSecret;
    }
}
