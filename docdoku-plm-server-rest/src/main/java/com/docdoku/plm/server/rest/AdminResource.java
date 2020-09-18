/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2020 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.plm.server.rest;

import io.swagger.annotations.*;
import org.dozer.DozerBeanMapperSingletonWrapper;
import org.dozer.Mapper;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.OAuthProvider;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.rest.dto.AccountDTO;
import com.docdoku.plm.server.rest.dto.OAuthProviderDTO;
import com.docdoku.plm.server.rest.dto.PlatformOptionsDTO;
import com.docdoku.plm.server.rest.dto.WorkspaceDTO;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Morgan Guimard
 */
@RequestScoped
@Api(value = "admin", description = "Admin resources",
        authorizations = {@Authorization(value = "authorization")})
@Path("admin")
@DeclareRoles(UserGroupMapping.ADMIN_ROLE_ID)
@RolesAllowed(UserGroupMapping.ADMIN_ROLE_ID)
public class AdminResource implements Serializable {

    @Inject
    private IWorkspaceManagerLocal workspaceService;

    @Inject
    private IDocumentManagerLocal documentService;

    @Inject
    private IProductManagerLocal productService;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private IWorkspaceManagerLocal workspaceManager;

    @Inject
    private IAccountManagerLocal accountManager;

    @Inject
    private IPlatformOptionsManagerLocal platformOptionsManager;

    @Inject
    private IIndexerManagerLocal indexManager;

    @Inject
    private IOAuthManagerLocal oAuthManager;

    private Mapper mapper;

    public AdminResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @GET
    @Path("disk-usage-stats")
    @ApiOperation(value = "Get disk usage stats",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of disk usage statistics"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getDiskSpaceUsageStats() throws EntityNotFoundException {

        JsonObjectBuilder diskUsage = Json.createObjectBuilder();

        Workspace[] allWorkspaces = userManager.getAdministratedWorkspaces();

        for (Workspace workspace : allWorkspaces) {
            long workspaceDiskUsage = workspaceService.getDiskUsageInWorkspace(workspace.getId());
            diskUsage.add(workspace.getId(), workspaceDiskUsage);
        }

        return diskUsage.build();

    }


    @GET
    @Path("users-stats")
    @ApiOperation(value = "Get users stats",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of user statistics"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getUsersStats()
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        JsonObjectBuilder userStats = Json.createObjectBuilder();

        Workspace[] allWorkspaces = userManager.getAdministratedWorkspaces();

        for (Workspace workspace : allWorkspaces) {
            int userCount = userManager.getUsers(workspace.getId()).length;
            userStats.add(workspace.getId(), userCount);
        }

        return userStats.build();

    }

    @GET
    @Path("documents-stats")
    @ApiOperation(value = "Get documents stats",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of documents statistics"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getDocumentsStats()
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        JsonObjectBuilder docStats = Json.createObjectBuilder();

        Workspace[] allWorkspaces = userManager.getAdministratedWorkspaces();

        for (Workspace workspace : allWorkspaces) {
            int documentsCount = documentService.getDocumentsInWorkspaceCount(workspace.getId());
            docStats.add(workspace.getId(), documentsCount);
        }

        return docStats.build();

    }

    @GET
    @Path("products-stats")
    @ApiOperation(value = "Get products stats",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products statistics"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getProductsStats()
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        JsonObjectBuilder productsStats = Json.createObjectBuilder();

        Workspace[] allWorkspaces = userManager.getAdministratedWorkspaces();

        for (Workspace workspace : allWorkspaces) {
            int productsCount = productService.getConfigurationItems(workspace.getId()).size();
            productsStats.add(workspace.getId(), productsCount);
        }

        return productsStats.build();

    }

    @GET
    @Path("parts-stats")
    @ApiOperation(value = "Get parts stats",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of parts statistics"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getPartsStats()
            throws AccessRightException, EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        JsonObjectBuilder partsStats = Json.createObjectBuilder();

        Workspace[] allWorkspaces = userManager.getAdministratedWorkspaces();

        for (Workspace workspace : allWorkspaces) {
            int productsCount = productService.getPartsInWorkspaceCount(workspace.getId());
            partsStats.add(workspace.getId(), productsCount);
        }

        return partsStats.build();
    }


    @PUT
    @ApiOperation(value = "Synchronize index for given workspace",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Accepted delete operation (asynchronous method)"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("index/{workspaceId}")
    public Response indexWorkspaceData(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException {
        indexManager.indexWorkspaceData(workspaceId);
        return Response.accepted().build();

    }

    @PUT
    @ApiOperation(value = "Synchronize index for all workspaces",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Accepted delete operation (asynchronous method)"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("index-all")
    public Response indexAllWorkspaces() throws AccountNotFoundException {
        indexManager.indexAllWorkspacesData();
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Path("platform-options")
    @ApiOperation(value = "Get platform options",
            response = PlatformOptionsDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PlatformOptions"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformOptionsDTO getPlatformOptions() {
        return mapper.map(platformOptionsManager.getPlatformOptions(), PlatformOptionsDTO.class);
    }

    @PUT
    @Path("platform-options")
    @ApiOperation(value = "Set platform options",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful update of PlatformOptions"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setPlatformOptions(
            @ApiParam("Options to set") PlatformOptionsDTO platformOptionsDTO) {
        platformOptionsManager.setRegistrationStrategy(platformOptionsDTO.getRegistrationStrategy());
        platformOptionsManager.setWorkspaceCreationStrategy(platformOptionsDTO.getWorkspaceCreationStrategy());
        return Response.noContent().build();
    }

    @PUT
    @ApiOperation(value = "Enable or disable workspace",
            response = WorkspaceDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated Workspace"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("workspace/{workspaceId}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDTO enableWorkspace(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "Enabled", required = true) @QueryParam("enabled") boolean enabled)
            throws EntityNotFoundException {
        Workspace workspace = workspaceManager.enableWorkspace(workspaceId, enabled);
        return mapper.map(workspace, WorkspaceDTO.class);
    }

    @PUT
    @ApiOperation(value = "Enable or disable account",
            response = AccountDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated Account"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("accounts/{login}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDTO enableAccount(
            @ApiParam(value = "Workspace id", required = true) @PathParam("login") String login,
            @ApiParam(value = "Enabled", required = true) @QueryParam("enabled") boolean enabled)
            throws EntityNotFoundException, NotAllowedException {
        Account account = accountManager.enableAccount(login, enabled);
        return mapper.map(account, AccountDTO.class);
    }


    @GET
    @Path("accounts")
    @ApiOperation(value = "Get all registered accounts",
            response = AccountDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of Accounts"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountDTO> getAccounts() {
        List<Account> accounts = accountManager.getAccounts();
        List<AccountDTO> accountsDTO = new ArrayList<>();
        for (Account account : accounts) {
            accountsDTO.add(mapper.map(account, AccountDTO.class));
        }
        return accountsDTO;
    }

    @PUT
    @Path("accounts")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated Account"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiOperation(value = "Update account",
            response = AccountDTO.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountDTO updateAccount(
            @ApiParam(required = true, value = "Updated account") AccountDTO accountDTO)
            throws EntityNotFoundException, NotAllowedException {

        Account account = accountManager.updateAccount(
                accountDTO.getLogin(),
                accountDTO.getName(),
                accountDTO.getEmail(),
                accountDTO.getLanguage(),
                accountDTO.getNewPassword(),
                accountDTO.getTimeZone());
        AccountDTO accountDToResult = mapper.map(account, AccountDTO.class);
        accountDToResult.setAdmin(UserGroupMapping.ADMIN_ROLE_ID
                .equals(accountManager.getUserGroupMapping(accountDTO.getLogin()).getGroupName()));
        return accountDToResult;
    }

    @GET
    @Path("providers")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of auth providers"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiOperation(value = "Get detailed providers",
            response = OAuthProviderDTO.class,
            responseContainer = "List")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetailedProviders() {
        List<OAuthProvider> providers = oAuthManager.getProviders();
        List<OAuthProviderDTO> dtos = new ArrayList<>();

        for (OAuthProvider provider : providers) {
            dtos.add(mapper.map(provider, OAuthProviderDTO.class));
        }

        return Response.ok(new GenericEntity<List<OAuthProviderDTO>>(dtos) {
        }).build();
    }

    @GET
    @Path("providers/{id}")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of auth provider"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiOperation(value = "Get detailed provider",
            response = OAuthProviderDTO.class)
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthProviderDTO getDetailedProvider(@ApiParam(value = "Provider id", required = true) @PathParam("id") int providerId)
            throws OAuthProviderNotFoundException {
        OAuthProvider provider = oAuthManager.getProvider(providerId);
        return mapper.map(provider, OAuthProviderDTO.class);
    }

    @POST
    @Path("providers")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful creation of auth provider"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiOperation(value = "Create a new OAuth provider",
            response = OAuthProviderDTO.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProvider(@ApiParam(required = true, value = "Updated account") OAuthProviderDTO providerDTO)
            throws EntityNotFoundException {

        OAuthProvider provider = oAuthManager.createProvider(providerDTO.getName(), providerDTO.isEnabled(), providerDTO.getAuthority(),
                providerDTO.getIssuer(), providerDTO.getClientID(), providerDTO.getJwsAlgorithm(),
                providerDTO.getJwkSetURL(), providerDTO.getRedirectUri(),
                providerDTO.getSecret(), providerDTO.getScope(), providerDTO.getResponseType(),
                providerDTO.getAuthorizationEndpoint());
        return Response.ok().entity(mapper.map(provider, OAuthProviderDTO.class)).build();
    }

    @PUT
    @Path("providers/{id}")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful update of auth provider"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiOperation(value = "Update OAuth provider",
            response = OAuthProviderDTO.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProvider(
            @ApiParam(value = "OAuthProvider id", required = true) @PathParam("id") int id,
            @ApiParam(required = true, value = "Updated provider") OAuthProviderDTO providerDTO)
            throws EntityNotFoundException {

        OAuthProvider provider = oAuthManager.updateProvider(id, providerDTO.getName(), providerDTO.isEnabled(), providerDTO.getAuthority(),
                providerDTO.getIssuer(), providerDTO.getClientID(), providerDTO.getJwsAlgorithm(),
                providerDTO.getJwkSetURL(), providerDTO.getRedirectUri(),
                providerDTO.getSecret(), providerDTO.getScope(), providerDTO.getResponseType(),
                providerDTO.getAuthorizationEndpoint());
        return Response.ok().entity(mapper.map(provider, OAuthProviderDTO.class)).build();
    }

    @DELETE
    @Path("providers/{id}")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful removal of auth provider"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @ApiOperation(value = "Delete OAuth provider", response = Response.class)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeProvider(
            @ApiParam(value = "OAuthProvider id", required = true) @PathParam("id") int id)
            throws EntityNotFoundException {
        oAuthManager.deleteProvider(id);
        return Response.noContent().build();
    }

}
