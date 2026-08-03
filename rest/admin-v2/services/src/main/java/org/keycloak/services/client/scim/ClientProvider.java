package org.keycloak.services.client.scim;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.admin.v2.AdminEventV2Builder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.spi.BaseResourceTypeProvider;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.services.ServiceException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientUnregisterContext;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import jakarta.ws.rs.core.Response;

/**
 * Minimal {@link ScimResourceTypeProvider} adapter
 */
public class ClientProvider extends BaseResourceTypeProvider<ClientModel, BaseClientRepresentation> {
    
    private final AdminEventBuilder adminEventBuilder;
    private final AdminPermissionEvaluator permissions;
    private final RealmModel realm;

    public ClientProvider(KeycloakSession session, RealmModel realm) {
        super(session);
        this.realm = realm;
        var authInfo = AdminRoot.authenticateRealmAdminRequest(session);
        this.permissions = AdminPermissions.evaluator(session, realm, authInfo);
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection()).resource(ResourceType.CLIENT);
    }

    @Override
    public Long count(SearchRequest searchRequest) {
        // not used for v2
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String getSchema() {
        // not used for v2
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Model> List<ModelSchema<M, BaseClientRepresentation>> getSchemas() {
        // not used for v2
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected void populate(ClientModel model, BaseClientRepresentation resource) {
        // used to retain values from the current model onto the resource during an update
        // not used by v2 - all logic is in onUpdate instead
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public Class<BaseClientRepresentation> getResourceType() {
        return BaseClientRepresentation.class;
    }

    @Override
    public String getId(BaseClientRepresentation resource) {
        return resource.getClientId();
    }

    @Override
    protected BaseClientRepresentation onCreate(BaseClientRepresentation resource) {
        // TODO
        return resource; // timestamps are being handled at the persistence layer
    }

    @Override
    protected BaseClientRepresentation onUpdate(ClientModel model, BaseClientRepresentation resource) {
        // TODO
        return resource; // timestamps are being handled at the persistence layer
    }
    
    @Override
    protected boolean onDelete(ClientModel model) {
        try {
            session.clientPolicy().triggerOnEvent(new AdminClientUnregisterContext(model, permissions.adminAuth()));
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        var clientRepresentation = createResourceTypeInstance(model, null, null);
        
        if (new ClientManager(new RealmManager(session)).removeClient(realm, model)) {
            fireAdminEvent(OperationType.DELETE, clientRepresentation);
        } else {
            throw new ServiceException("Could not delete client", Response.Status.BAD_REQUEST);
        }
        
        // TODO: we are throwing exceptions rather than just returning false
        return true;
    }

    @Override
    protected Stream<ClientModel> getModels(SearchRequest searchRequest) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientModel getModel(String id) {
        return realm.getClientByClientId(id);
    }
    
    @Override
    protected BaseClientRepresentation createResourceTypeInstance(ClientModel model, List<String> attributes,
            List<String> excludedAttributes) {
        
        
        //return Optional.ofNullable(MAPPERS.getMapper(model.getProtocol())).orElseThrow(() -> new ServiceException("Mapper not found, unsupported client protocol: " + protocol,
        //        Response.Status.BAD_REQUEST));
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
    }

    /**
     * Fires a v2 admin event for client operations (only enabled for testing now to avoid duplicated admin events)
     *
     * @param operationType  the type of operation (CREATE, UPDATE, DELETE)
     * @param representation the v2 representation of the client
     */
    protected void fireAdminEvent(OperationType operationType, BaseClientRepresentation representation) {
        if (Boolean.parseBoolean(System.getProperty("kc.admin-v2.client-service.events.enabled", "false"))) {
            adminEventBuilder
                    .operation(operationType)
                    .resourcePath(session.getContext().getUri())
                    .representation(representation)
                    .success();
        }
    }

}
