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
import com.docdoku.plm.server.core.admin.WorkspaceBackOptions;
import com.docdoku.plm.server.core.admin.WorkspaceFrontOptions;
import com.docdoku.plm.server.core.common.*;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.security.WorkspaceUserGroupMembership;
import com.docdoku.plm.server.core.security.WorkspaceUserMembership;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.rest.dto.*;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

@RequestScoped
@Api(value = "workspaces", description = "Operations about workspaces")
@Path("workspaces")
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
public class WorkspaceResource {

    @Inject
    private DocumentsResource documents;

    @Inject
    private DocumentBaselinesResource documentBaselines;

    @Inject
    private FolderResource folders;

    @Inject
    private DocumentTemplateResource docTemplates;

    @Inject
    private PartTemplateResource partTemplates;

    @Inject
    private ProductResource products;

    @Inject
    private ProductConfigurationsResource productConfigurations;

    @Inject
    private ProductBaselinesResource productBaselines;

    @Inject
    private ProductInstancesResource productInstancesResource;

    @Inject
    private PartsResource parts;

    @Inject
    private TagResource tags;

    @Inject
    private TaskResource tasks;

    @Inject
    private WorkflowResource workflowInstances;

    @Inject
    private WorkflowModelResource workflowModels;

    @Inject
    private WorkspaceWorkflowResource workspaceWorkflows;

    @Inject
    private ChangeItemsResource changeItems;

    @Inject
    private UserResource users;

    @Inject
    private UserGroupResource groups;

    @Inject
    private RoleResource roles;

    @Inject
    private ModificationNotificationResource notifications;

    @Inject
    private WorkspaceMembershipResource workspaceMemberships;

    @Inject
    private EffectivityResource effectivities;

    @Inject
    private WebhookResource webhooks;

    @Inject
    private IDocumentManagerLocal documentService;

    @Inject
    private IProductManagerLocal productService;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private LOVResource lov;

    @Inject
    private AttributesResource attributes;

    @Inject
    private IWorkspaceManagerLocal workspaceManager;

    private Mapper mapper;

    @Inject
    private IAccountManagerLocal accountManager;

    @Inject
    private IContextManagerLocal contextManager;

    @Inject
    private IIndexerManagerLocal indexerManager;

    public WorkspaceResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @GET
    @ApiOperation(value = "Get workspace list for authenticated user",
            response = WorkspaceListDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of WorkspaceListDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceListDTO getWorkspacesForConnectedUser()
            throws EntityNotFoundException {

        WorkspaceListDTO workspaceListDTO = new WorkspaceListDTO();

        Workspace[] administratedWorkspaces = userManager.getAdministratedWorkspaces();
        Workspace[] allWorkspaces = userManager.getWorkspacesWhereCallerIsActive();

        for (Workspace workspace : administratedWorkspaces) {
            workspaceListDTO.addAdministratedWorkspaces(mapper.map(workspace, WorkspaceDTO.class));
        }
        for (Workspace workspace : allWorkspaces) {
            workspaceListDTO.addAllWorkspaces(mapper.map(workspace, WorkspaceDTO.class));
        }
        return workspaceListDTO;
    }

    @GET
    @ApiOperation(value = "Get detailed workspace list for authenticated user",
            response = WorkspaceDetailsDTO.class,
            responseContainer = "List",
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of WorkspaceDetailsDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/more")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetailedWorkspacesForConnectedUser()
            throws EntityNotFoundException {
        List<WorkspaceDetailsDTO> workspaceListDTO = new ArrayList<>();

        for (Workspace workspace : userManager.getWorkspacesWhereCallerIsActive()) {
            workspaceListDTO.add(mapper.map(workspace, WorkspaceDetailsDTO.class));
        }
        return Response.ok(new GenericEntity<List<WorkspaceDetailsDTO>>((List<WorkspaceDetailsDTO>) workspaceListDTO) {
        }).build();
    }

    @GET
    @ApiOperation(value = "Get online users visible by authenticated user",
            response = UserDTO.class,
            responseContainer = "List",
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of UserDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("reachable-users")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDTO[] getReachableUsersForCaller()
            throws EntityNotFoundException {

        User[] reachableUsers = userManager.getReachableUsers();
        UserDTO[] userDTOs = new UserDTO[reachableUsers.length];

        for (int i = 0; i < reachableUsers.length; i++) {
            userDTOs[i] = mapper.map(reachableUsers[i], UserDTO.class);
        }

        return userDTOs;
    }

    @PUT
    @ApiOperation(value = "Update a workspace",
            response = WorkspaceDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated WorkspaceDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkspace(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Workspace values to update") WorkspaceDTO workspaceDTO)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException {

        Workspace workspace = workspaceManager.updateWorkspace(workspaceId, workspaceDTO.getDescription(), workspaceDTO.isFolderLocked());
        return Response.ok(mapper.map(workspace, WorkspaceDTO.class)).build();
    }

    @PUT
    @Path("/{workspaceId}/index")
    @ApiOperation(value = "Re-Index a workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Accepted indexation (asynchronous method)"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response synchronizeIndexer(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException {
        indexerManager.indexWorkspaceData(workspaceId);
        return Response.accepted().build();
    }

    @DELETE
    @ApiOperation(value = "Delete a workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Accepted deletion (asynchronous method)"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteWorkspace(@ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException {
        workspaceManager.deleteWorkspace(workspaceId);
        return Response.accepted().build();
    }

    @GET
    @ApiOperation(value = "Get user groups in given workspace",
            response = UserGroupDTO.class,
            responseContainer = "List",
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of UserGroupDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/user-group")
    @Produces(MediaType.APPLICATION_JSON)
    public UserGroupDTO[] getUserGroups(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        UserGroup[] userGroups = userManager.getUserGroups(workspaceId);
        UserGroupDTO[] userGroupDTOs = new UserGroupDTO[userGroups.length];
        for (int i = 0; i < userGroups.length; i++) {
            userGroupDTOs[i] = mapper.map(userGroups[i], UserGroupDTO.class);
        }
        return userGroupDTOs;
    }

    @POST
    @ApiOperation(value = "Create a new user group",
            response = UserGroupDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of created UserGroupDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/user-group")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public UserGroupDTO createGroup(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "UserGroup to create") UserGroupDTO userGroupDTO)
            throws EntityAlreadyExistsException, AccessRightException, EntityNotFoundException, CreationException {

        UserGroup userGroup = userManager.createUserGroup(userGroupDTO.getId(), workspaceId);
        return mapper.map(userGroup, UserGroupDTO.class);
    }

    @DELETE
    @ApiOperation(value = "Remove a user group",
            response = UserGroupDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful removal of user from UserGroupDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/user-group/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeGroup(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Group id") @PathParam("groupId") String groupId)
            throws EntityNotFoundException, AccessRightException, EntityConstraintException {

        userManager.removeUserGroups(workspaceId, new String[]{groupId});
        return Response.noContent().build();
    }

    @PUT
    @ApiOperation(value = "Add a user to workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful addition of user in UserGroupDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/add-user")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Group id") @QueryParam("group") String groupId,
            @ApiParam(required = true, value = "User to add") UserDTO userDTO)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException,
            EntityAlreadyExistsException, CreationException {

        if (groupId != null && !groupId.isEmpty()) {
            userManager.addUserInGroup(new UserGroupKey(workspaceId, groupId), userDTO.getLogin());
        } else {
            userManager.addUserInWorkspace(workspaceId, userDTO.getLogin());
        }

        return Response.noContent().build();
    }

    @PUT
    @ApiOperation(value = "Set a new admin for given workspace",
            response = WorkspaceDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated Workspace"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNewAdmin(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "New admin user") UserDTO userDTO)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException, NotAllowedException {

        Workspace workspace = workspaceManager.changeAdmin(workspaceId, userDTO.getLogin());
        return Response.ok(mapper.map(workspace, WorkspaceDTO.class)).build();
    }

    @POST
    @ApiOperation(value = "Create a new workspace",
            response = WorkspaceDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of created Workspace"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDTO createWorkspace(
            @ApiParam(value = "Login for workspace admin", required = false) @QueryParam("userLogin") String userLogin,
            @ApiParam(value = "Workspace to create", required = true) WorkspaceDTO workspaceDTO)
            throws EntityAlreadyExistsException, CreationException, EntityNotFoundException, IOException, NotAllowedException {
        Account account;
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            account = accountManager.getAccount(userLogin);
        } else {
            account = accountManager.getMyAccount();
        }
        Workspace workspace = workspaceManager.createWorkspace(workspaceDTO.getId(), account, workspaceDTO.getDescription(), workspaceDTO.isFolderLocked());

        return mapper.map(workspace, WorkspaceDTO.class);
    }

    @PUT
    @ApiOperation(value = "Set user access in workspace",
            response = UserDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated UserDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/user-access")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setUserAccess(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "User to grant access in workspace", required = true) UserDTO userDTO)
            throws AccessRightException, EntityNotFoundException {

        if (userDTO.getMembership() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        WorkspaceUserMembership workspaceUserMembership = userManager.grantUserAccess(workspaceId, userDTO.getLogin(), userDTO.getMembership() == WorkspaceMembership.READ_ONLY);
        if(workspaceUserMembership == null) {

            return  Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok(mapper.map(workspaceUserMembership.getMember(), UserDTO.class)).build();
    }

    @PUT
    @ApiOperation(value = "Set user group access in workspace",
            response = WorkspaceUserGroupMemberShipDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated WorkspaceUserGroupMemberShipDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/group-access")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setGroupAccess(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "User to grant access in group", required = true) WorkspaceUserGroupMemberShipDTO workspaceUserGroupMemberShipDTO)
            throws AccessRightException, EntityNotFoundException {

        WorkspaceUserGroupMembership membership = userManager.grantGroupAccess(workspaceId, workspaceUserGroupMemberShipDTO.getMemberId(), workspaceUserGroupMemberShipDTO.isReadOnly());
        return Response.ok(mapper.map(membership, WorkspaceUserGroupMemberShipDTO.class)).build();
    }

    @PUT
    @ApiOperation(value = "Remove user from user group",
            response = UserGroupDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated UserGroupDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/remove-from-group/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserFromGroup(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "Group id", required = true) @PathParam("groupId") String groupId,
            @ApiParam(value = "User to remove from group", required = true) UserDTO userDTO)
            throws AccessRightException, EntityNotFoundException {

        UserGroup userGroup = userManager.removeUserFromGroup(new UserGroupKey(workspaceId, groupId), userDTO.getLogin());
        return Response.ok(mapper.map(userGroup, UserGroupDTO.class)).build();
    }

    @PUT
    @ApiOperation(value = "Remove user from workspace",
            response = WorkspaceDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of updated WorkspaceDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/remove-from-workspace")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserFromWorkspace(@ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
                                            @ApiParam(value = "User to remove from workspace", required = true) UserDTO userDTO)
            throws EntityNotFoundException, AccessRightException, EntityConstraintException, UserNotActiveException {
        Workspace workspace = userManager.removeUser(workspaceId, userDTO.getLogin());
        return Response.ok(mapper.map(workspace, WorkspaceDTO.class)).build();
    }

    @PUT
    @ApiOperation(value = "Enable user in workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful enable operation"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/enable-user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response enableUser(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "User to enable", required = true) UserDTO userDTO)
            throws AccessRightException, EntityNotFoundException {

        userManager.activateUser(workspaceId, userDTO.getLogin());
        return Response.noContent().build();
    }

    @PUT
    @ApiOperation(value = "Disable user in workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful disable operation"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/disable-user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disableUser(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "User to disable", required = true) UserDTO userDTO)
            throws AccessRightException, EntityNotFoundException {

        userManager.passivateUser(workspaceId, userDTO.getLogin());
        return Response.noContent().build();
    }

    @PUT
    @ApiOperation(value = "Enable user group in workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful enable operation"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/enable-group")
    @Produces(MediaType.APPLICATION_JSON)
    public Response enableGroup(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "Group to enable", required = true) UserGroupDTO userGroupDTO)
            throws AccessRightException, EntityNotFoundException {

        userManager.activateUserGroup(workspaceId, userGroupDTO.getId());
        return Response.noContent().build();
    }

    @PUT
    @ApiOperation(value = "Disable user group in workspace",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful disable operation"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/disable-group")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disableGroup(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "Group to disable", required = true) UserGroupDTO userGroupDTO)
            throws AccessRightException, EntityNotFoundException {

        userManager.passivateUserGroup(workspaceId, userGroupDTO.getId());
        return Response.noContent().build();
    }

    @GET
    @ApiOperation(value = "Get stats overview for workspace",
            response = StatsOverviewDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of StatsOverviewDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/stats-overview")
    @Produces(MediaType.APPLICATION_JSON)
    public StatsOverviewDTO getStatsOverview(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        StatsOverviewDTO statsOverviewDTO = new StatsOverviewDTO();

        statsOverviewDTO.setDocuments(documentService.getDocumentsInWorkspaceCount(workspaceId));
        statsOverviewDTO.setParts(productService.getPartsInWorkspaceCount(workspaceId));

        statsOverviewDTO.setUsers(userManager.getUsers(workspaceId).length);
        statsOverviewDTO.setProducts(productService.getConfigurationItems(workspaceId).size());

        return statsOverviewDTO;
    }

    @GET
    @ApiOperation(value = "Get disk usage stats for workspace",
            response = DiskUsageSpaceDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of DiskUsageSpaceDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/disk-usage-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public DiskUsageSpaceDTO getDiskSpaceUsageStats(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException {

        DiskUsageSpaceDTO diskUsageSpaceDTO = new DiskUsageSpaceDTO();
        diskUsageSpaceDTO.setDocuments(documentService.getDiskUsageForDocumentsInWorkspace(workspaceId));
        diskUsageSpaceDTO.setParts(productService.getDiskUsageForPartsInWorkspace(workspaceId));
        diskUsageSpaceDTO.setDocumentTemplates(documentService.getDiskUsageForDocumentTemplatesInWorkspace(workspaceId));
        diskUsageSpaceDTO.setPartTemplates(productService.getDiskUsageForPartTemplatesInWorkspace(workspaceId));
        return diskUsageSpaceDTO;
    }

    @GET
    @ApiOperation(value = "Get checked out documents stats for workspace",
            response = CheckedOutStatsResponseDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of documents stats"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/checked-out-documents-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCheckedOutDocumentsStats(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException {

        DocumentRevision[] checkedOutDocumentRevisions = documentService.getAllCheckedOutDocumentRevisions(workspaceId);
        JsonObjectBuilder statsByUserBuilder = Json.createObjectBuilder();

        Map<String, JsonArrayBuilder> userArrays = new HashMap<>();
        for (DocumentRevision documentRevision : checkedOutDocumentRevisions) {

            String userLogin = documentRevision.getCheckOutUser().getLogin();
            JsonArrayBuilder userArray = userArrays.get(userLogin);
            if (userArray == null) {
                userArray = Json.createArrayBuilder();
                userArrays.put(userLogin, userArray);
            }
            userArray.add(Json.createObjectBuilder().add("date", documentRevision.getCheckOutDate().getTime()).build());
        }

        for (Map.Entry<String, JsonArrayBuilder> entry : userArrays.entrySet()) {
            statsByUserBuilder.add(entry.getKey(), entry.getValue().build());
        }

        return Response.ok().entity(statsByUserBuilder.build()).build();

    }

    @GET
    @ApiOperation(value = "Get checked out parts stats for workspace",
            response = CheckedOutStatsResponseDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of parts stats"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/checked-out-parts-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCheckedOutPartsStats(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException {

        PartRevision[] checkedOutPartRevisions = productService.getAllCheckedOutPartRevisions(workspaceId);
        JsonObjectBuilder statsByUserBuilder = Json.createObjectBuilder();

        Map<String, JsonArrayBuilder> userArrays = new HashMap<>();
        for (PartRevision partRevision : checkedOutPartRevisions) {

            String userLogin = partRevision.getCheckOutUser().getLogin();
            JsonArrayBuilder userArray = userArrays.get(userLogin);
            if (userArray == null) {
                userArray = Json.createArrayBuilder();
                userArrays.put(userLogin, userArray);
            }
            userArray.add(Json.createObjectBuilder().add("date", partRevision.getCheckOutDate().getTime()).build());
        }

        for (Map.Entry<String, JsonArrayBuilder> entry : userArrays.entrySet()) {
            statsByUserBuilder.add(entry.getKey(), entry.getValue().build());
        }

        return Response.ok().entity(statsByUserBuilder.build()).build();
    }

    @GET
    @ApiOperation(value = "Get user stats for workspace",
            response = UserStatsDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of users stats"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/users-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public UserStatsDTO getUsersStats(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        WorkspaceUserMembership[] workspaceUserMemberships = userManager.getWorkspaceUserMemberships(workspaceId);
        WorkspaceUserGroupMembership[] workspaceUserGroupMemberships = userManager.getWorkspaceUserGroupMemberships(workspaceId);

        int usersCount = userManager.getUsers(workspaceId).length;
        int activeUsersCount = workspaceUserMemberships.length;
        int inactiveUsersCount = usersCount - activeUsersCount;

        int groupsCount = userManager.getUserGroups(workspaceId).length;
        int activeGroupsCount = workspaceUserGroupMemberships.length;
        int inactiveGroupsCount = groupsCount - activeGroupsCount;

        UserStatsDTO userStatsDTO = new UserStatsDTO();

        userStatsDTO.setUsers(usersCount);
        userStatsDTO.setActiveusers(activeUsersCount);
        userStatsDTO.setInactiveusers(inactiveUsersCount);

        userStatsDTO.setGroups(groupsCount);
        userStatsDTO.setActivegroups(activeGroupsCount);
        userStatsDTO.setInactivegroups(inactiveGroupsCount);

        return userStatsDTO;
    }

    @GET
    @ApiOperation(value = "Get workspace front-end options",
            response = WorkspaceFrontOptionsDTO.class,
            authorizations = {@Authorization(value = "authorization")})
    @Path("/{workspaceId}/front-options")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieve of workspace front options"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public WorkspaceFrontOptionsDTO getWorkspaceFrontOptions(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        WorkspaceFrontOptions workspaceFrontOptions = Optional.ofNullable(workspaceManager.getWorkspaceFrontOptions(workspaceId)).orElse(new WorkspaceFrontOptions());
        return mapper.map(workspaceFrontOptions, WorkspaceFrontOptionsDTO.class);
    }

    @PUT
    @ApiOperation(value = "Update workspace front-end options",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @Path("/{workspaceId}/front-options")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful update of workspace front options"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkspaceFrontOptions(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "Option values", required = true) WorkspaceFrontOptionsDTO workspaceFrontOptionsDTO)
            throws AccessRightException, EntityNotFoundException {

        List<String> partTableColumns = workspaceFrontOptionsDTO.getPartTableColumns();
        List<String> documentTableColumns = workspaceFrontOptionsDTO.getDocumentTableColumns();

        workspaceManager.updateWorkspaceFrontOptions(new WorkspaceFrontOptions(new Workspace(workspaceId), partTableColumns, documentTableColumns));
        return Response.noContent().build();

    }


    @GET
    @ApiOperation(value = "Get workspace back-end options",
            response = WorkspaceBackOptionsDTO.class,
            authorizations = {@Authorization(value = "authorization")}
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of workspace back options"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/back-options")
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceBackOptionsDTO getWorkspaceBackOptions(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {
        WorkspaceBackOptions workspaceBackOptions = workspaceManager.getWorkspaceBackOptions(workspaceId);
        return mapper.map(workspaceBackOptions, WorkspaceBackOptionsDTO.class);
    }

    @PUT
    @ApiOperation(value = "Update workspace back-end options",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")}
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful update of workspace back options"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{workspaceId}/back-options")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkspaceBackOptions(
            @ApiParam(value = "Workspace id", required = true) @PathParam("workspaceId") String workspaceId,
            @ApiParam(value = "Option values", required = true) WorkspaceBackOptionsDTO workspaceBackOptionsDTO
    ) throws EntityNotFoundException, AccessRightException {
        workspaceManager.updateWorkspaceBackOptions(new WorkspaceBackOptions(new Workspace(workspaceId), workspaceBackOptionsDTO.isSendEmails()));
        return Response.noContent().build();
    }

    // Sub resources
    @ApiOperation(value = "DocumentsResource")
    @Path("/{workspaceId}/documents")
    public DocumentsResource documents() {
        return documents;
    }

    @ApiOperation(value = "FolderResource")
    @Path("/{workspaceId}/folders")
    public FolderResource folders() {
        return folders;
    }

    @ApiOperation(value = "DocumentTemplateResource")
    @Path("/{workspaceId}/document-templates")
    public DocumentTemplateResource docTemplates() {
        return docTemplates;
    }

    @ApiOperation(value = "PartTemplateResource")
    @Path("/{workspaceId}/part-templates")
    public PartTemplateResource partTemplates() {
        return partTemplates;
    }

    @ApiOperation(value = "ProductResource")
    @Path("/{workspaceId}/products")
    public ProductResource products() {
        return products;
    }

    @ApiOperation(value = "ProductConfigurationResource")
    @Path("/{workspaceId}/product-configurations")
    public ProductConfigurationsResource productConfigurations() {
        return productConfigurations;
    }

    @ApiOperation(value = "ProductBaselinesResource")
    @Path("/{workspaceId}/product-baselines")
    public ProductBaselinesResource productBaselines() {
        return productBaselines;
    }

    @ApiOperation(value = "Get all ProductInstancesResource")
    @Path("/{workspaceId}/product-instances")
    public ProductInstancesResource getProductInstances() {
        return productInstancesResource;
    }

    @ApiOperation(value = "PartsResource")
    @Path("/{workspaceId}/parts")
    public PartsResource parts() {
        return parts;
    }

    @ApiOperation(value = "TagResource")
    @Path("/{workspaceId}/tags")
    public TagResource tags() {
        return tags;
    }

    @ApiOperation(value = "TaskResource")
    @Path("/{workspaceId}/tasks")
    public TaskResource tasks() {
        return tasks;
    }

    @ApiOperation(value = "ModificationNotificationResource")
    @Path("/{workspaceId}/notifications")
    public ModificationNotificationResource notifications() {
        return notifications;
    }

    @ApiOperation(value = "WorkflowModelResource")
    @Path("/{workspaceId}/workflow-models")
    public WorkflowModelResource workflowModels() {
        return workflowModels;
    }

    @ApiOperation(value = "WorkflowResource")
    @Path("/{workspaceId}/workflow-instances")
    public WorkflowResource workflowsInstances() {
        return workflowInstances;
    }

    @ApiOperation(value = "WorkspaceWorkflowResource")
    @Path("/{workspaceId}/workspace-workflows")
    public WorkspaceWorkflowResource workspaceWorkflows() {
        return workspaceWorkflows;
    }

    @ApiOperation(value = "UserResource")
    @Path("/{workspaceId}/users")
    public UserResource users() {
        return users;
    }

    @ApiOperation(value = "UserGroupResource")
    @Path("/{workspaceId}/groups")
    public UserGroupResource groups() {
        return groups;
    }

    @ApiOperation(value = "RoleResource", hidden = false)
    @Path("/{workspaceId}/roles")
    public RoleResource roles() {
        return roles;
    }

    @ApiOperation(value = "WorkspaceMembershipResource")
    @Path("/{workspaceId}/memberships")
    public WorkspaceMembershipResource workspaceMemberships() {
        return workspaceMemberships;
    }

    @ApiOperation(value = "ChangeItemsResource")
    @Path("/{workspaceId}/changes")
    public ChangeItemsResource changeItems() {
        return changeItems;
    }

    @ApiOperation(value = "DocumentBaselinesResource")
    @Path("/{workspaceId}/document-baselines")
    public DocumentBaselinesResource documentBaselines() {
        return documentBaselines;
    }

    @ApiOperation(value = "LOVResource")
    @Path("/{workspaceId}/lov")
    public LOVResource lov() {
        return lov;
    }

    @ApiOperation(hidden = true, value = "AttributesResource")
    @Path("/{workspaceId}/attributes")
    public AttributesResource attributes() {
        return attributes;
    }

    @ApiOperation(value = "EffectivityResource")
    @Path("/{workspaceId}/effectivities")
    public EffectivityResource effectivities() {
        return effectivities;
    }

    @ApiOperation(value = "WebhookResource")
    @Path("/{workspaceId}/webhooks")
    public WebhookResource webhooks() {
        return webhooks;
    }
}
