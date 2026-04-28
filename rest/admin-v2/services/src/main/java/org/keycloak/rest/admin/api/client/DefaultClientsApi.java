package org.keycloak.rest.admin.api.client;

import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

public class DefaultClientsApi implements ClientsApi {
    
    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final RealmModel realm;
    private final ClientService clientService;

    // v1 resources
    private final RealmAdminResource realmAdminResource;

    public DefaultClientsApi(@Nonnull KeycloakSession session,
                             @Nonnull RealmModel realm,
                             @Nonnull AdminPermissionEvaluator permissions,
                             // remove v1 resource once we are not attached to API v1
                             @Nonnull RealmAdminResource realmAdminResource) {
        this.session = session;
        this.realm = realm;
        this.permissions = permissions;
        this.realmAdminResource = realmAdminResource;
        this.clientService = new DefaultClientService(session, realm, permissions, realmAdminResource);
    }

    @GET
    @Override
    public Stream<BaseClientRepresentation> getClients() {
        // TODO: accept a set of fields to emit
        Set<String> fields = Set.of("clientId");
        // TODO: validate that the fields are acceptable - overlaps with query validation logic
        //  we need a static or generated set of fields mapped to accessors / setters
        
        // demonstrates a "catch all" approach
        // filter via jackson serialization - this could be controversial so it may only be for rapid prototyping, it also seems only to understand for top-level fields
        //ObjectMapperResolver.setPropertyFilter(SimpleBeanPropertyFilter.filterOutAllExcept(fields));
         
        // pass the fields to the server, then expand the RepModelMapper.fromModel to accept the fields / accessors
        
        return clientService.getClients(realm, null, null, null);
    }

    @POST
    @Override
    public Response createClient(@Valid BaseClientRepresentation client) {
        return Response.status(Response.Status.CREATED)
                .entity(clientService.createClient(realm, client))
                .build();
    }

    @Path("{id}")
    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        return new DefaultClientApi(session, realm, clientId, permissions, realmAdminResource);
    }

}
