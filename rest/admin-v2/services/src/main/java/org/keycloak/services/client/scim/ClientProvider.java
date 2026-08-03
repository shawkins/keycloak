package org.keycloak.services.client.scim;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.spi.BaseResourceTypeProvider;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;

/**
 * Minimal {@link ScimResourceTypeProvider} adapter
 */
public class ClientProvider extends BaseResourceTypeProvider<ClientModel, BaseClientRepresentation> {

    public ClientProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public Long count(SearchRequest searchRequest) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String getSchema() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Model> List<ModelSchema<M, BaseClientRepresentation>> getSchemas() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Class<BaseClientRepresentation> getResourceType() {
        return BaseClientRepresentation.class;
    }

    @Override
    public void close() {
    }

    @Override
    public String getId(BaseClientRepresentation resource) {
        return resource.getClientId();
    }

    @Override
    protected BaseClientRepresentation onCreate(BaseClientRepresentation resource) {
        return resource; // timestamps are being handled at the persistence layer
    }

    @Override
    protected BaseClientRepresentation onUpdate(ClientModel model, BaseClientRepresentation resource) {
        return resource; // timestamps are being handled at the persistence layer
    }

    @Override
    protected boolean onDelete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Stream<ClientModel> getModels(SearchRequest searchRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ClientModel getModel(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
    }

    @Override
    protected void populate(ClientModel model, BaseClientRepresentation resource) {
        throw new UnsupportedOperationException();        
    }

    @Override
    protected BaseClientRepresentation createResourceTypeInstance(ClientModel model, List<String> attributes,
            List<String> excludedAttributes) {
        throw new UnsupportedOperationException();
    }
    
}
