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
import com.docdoku.plm.server.core.change.ModificationNotification;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.configuration.ProductStructureFilter;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.query.PartSearchQuery;
import com.docdoku.plm.server.core.query.Query;
import com.docdoku.plm.server.core.query.QueryResultRow;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IImporterManagerLocal;
import com.docdoku.plm.server.core.services.IPSFilterManagerLocal;
import com.docdoku.plm.server.core.services.IProductManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.core.util.FileIO;
import com.docdoku.plm.server.export.ExcelGenerator;
import com.docdoku.plm.server.rest.collections.QueryResult;
import com.docdoku.plm.server.rest.dto.*;
import com.docdoku.plm.server.rest.file.util.BinaryResourceUpload;
import com.docdoku.plm.server.rest.util.SearchQueryParser;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;

@RequestScoped
@Api(hidden = true, value = "parts", description = "Operation about parts",
        authorizations = {@Authorization(value = "authorization")})
@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
public class PartsResource {

    @Inject
    private IProductManagerLocal productService;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private PartResource partResource;

    @Inject
    private PartEffectivityResource partEffectivityResource;

    @Inject
    private IPSFilterManagerLocal filterService;

    @EJB
    private IImporterManagerLocal importerService;
    private Mapper mapper;

    public PartsResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @ApiOperation(value = "SubResource : PartResource")
    @Path("{partNumber: [^/].*}-{partVersion:[A-Z]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public PartResource getPartResource() {
        return partResource;
    }

    @ApiOperation(value = "SubResource : PartEffectivity")
    @Path("{partNumber: [^/].*}-{partVersion:[A-Z]+}/effectivities")
    @Produces(MediaType.APPLICATION_JSON)
    public PartEffectivityResource getPartEffectivity() {
        return partEffectivityResource;
    }

    @GET
    @ApiOperation(value = "Get part revisions in workspace",
            response = PartRevisionDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPartRevisions(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Start offset", defaultValue = "0") @QueryParam("start") int start,
            @ApiParam(required = false, value = "Max results", defaultValue = "20") @QueryParam("length") int length)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {
        // potential OOM => should restrict length
        List<PartRevision> partRevisions = productService.getPartRevisions(Tools.stripTrailingSlash(workspaceId), start, length);
        List<PartRevisionDTO> partRevisionDTOs = new ArrayList<>();

        for (PartRevision partRevision : partRevisions) {
            PartRevisionDTO partRevisionDTO = Tools.mapPartRevisionToPartDTO(partRevision);

            List<ModificationNotificationDTO> notificationDTOs = getModificationNotificationDTOs(partRevision);
            partRevisionDTO.setNotifications(notificationDTOs);

            partRevisionDTOs.add(partRevisionDTO);
        }
        return Response.ok(new GenericEntity<List<PartRevisionDTO>>((List<PartRevisionDTO>) partRevisionDTOs) {
        }).build();
    }

    @GET
    @ApiOperation(value = "Count part revisions in workspace",
            response = CountDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTO count"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    public CountDTO getTotalNumberOfParts(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        return new CountDTO(productService.getPartsInWorkspaceCount(Tools.stripTrailingSlash(workspaceId)));
    }

    @GET
    @ApiOperation(value = "Get part revisions in workspace",
            response = PartRevisionDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("tags/{tagId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPartRevisionsByTag(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Tag id") @PathParam("tagId") String tagId)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        PartRevision[] partRevisions = productService.findPartRevisionsByTag(Tools.stripTrailingSlash(workspaceId), tagId);
        List<PartRevisionDTO> partRevisionDTOs = new ArrayList<>();

        for (PartRevision partRevision : partRevisions) {
            PartRevisionDTO partRevisionDTO = Tools.mapPartRevisionToPartDTO(partRevision);

            List<ModificationNotificationDTO> notificationDTOs = getModificationNotificationDTOs(partRevision);
            partRevisionDTO.setNotifications(notificationDTOs);

            partRevisionDTOs.add(partRevisionDTO);
        }
        return Response.ok(new GenericEntity<List<PartRevisionDTO>>((List<PartRevisionDTO>) partRevisionDTOs) {
        }).build();
    }

    @GET
    @ApiOperation(value = "Search part revisions in workspace",
            response = PartRevisionDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPartRevisions(
            @Context UriInfo uri,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Query") @QueryParam("q") String q,
            @ApiParam(required = false, value = "Part number") @QueryParam("number") String number,
            @ApiParam(required = false, value = "Part name") @QueryParam("name") String name,
            @ApiParam(required = false, value = "Part version") @QueryParam("version") String version,
            @ApiParam(required = false, value = "Part author") @QueryParam("author") String author,
            @ApiParam(required = false, value = "Part type") @QueryParam("type") String type,
            @ApiParam(required = false, value = "Part created from date") @QueryParam("createdFrom") String createdFrom,
            @ApiParam(required = false, value = "Part created to date") @QueryParam("createdTo") String createdTo,
            @ApiParam(required = false, value = "Part modified from date") @QueryParam("modifiedFrom") String modifiedFrom,
            @ApiParam(required = false, value = "Part modified to date") @QueryParam("modifiedTo") String modifiedTo,
            @ApiParam(required = false, value = "Part tags") @QueryParam("tags") String tags,
            @ApiParam(required = false, value = "Part files content") @QueryParam("content") String content,
            @ApiParam(required = false, value = "Part files attributes") @QueryParam("attributes") String attributes,
            @ApiParam(required = false, value = "Start offset", defaultValue = "0") @QueryParam("from") int from,
            @ApiParam(required = false, value = "Max results", defaultValue = "10") @QueryParam("size") int size,
            @ApiParam(required = false, value = "Search mode (false for history / true for head only)") @QueryParam("fetchHeadOnly") boolean fetchHeadOnly)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, NotAllowedException, WorkspaceNotEnabledException, IndexerRequestException, IndexerNotAvailableException {

        PartSearchQuery partSearchQuery = SearchQueryParser.parsePartStringQuery(workspaceId, uri.getQueryParameters());

        // Set default size
        size = size == 0 ? 10 : size;

        List<PartRevision> partRevisions = productService.searchPartRevisions(partSearchQuery, from, size);
        List<PartRevisionDTO> partRevisionDTOs = new ArrayList<>();

        for (PartRevision partRevision : partRevisions) {
            PartRevisionDTO partRevisionDTO = Tools.mapPartRevisionToPartDTO(partRevision);

            List<ModificationNotificationDTO> notificationDTOs = getModificationNotificationDTOs(partRevision);
            partRevisionDTO.setNotifications(notificationDTOs);

            partRevisionDTOs.add(partRevisionDTO);
        }

        return Response.ok(new GenericEntity<List<PartRevisionDTO>>((List<PartRevisionDTO>) partRevisionDTOs) {
        }).build();
    }

    @GET
    @ApiOperation(value = "Get custom queries in workspace",
            response = QueryDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of QueryDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("queries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomQueries(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        List<Query> queries = productService.getQueries(workspaceId);
        List<QueryDTO> queryDTOs = new ArrayList<>();
        for (Query query : queries) {
            queryDTOs.add(mapper.map(query, QueryDTO.class));
        }
        return Response.ok(new GenericEntity<List<QueryDTO>>((List<QueryDTO>) queryDTOs) {
        }).build();
    }

    @POST
    @ApiOperation(value = "Run custom query in workspace",
            response = File.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of QueryDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("queries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response runCustomQuery(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Save the query flag", defaultValue = "false") @QueryParam("save") boolean save,
            @ApiParam(required = false, value = "Choose export type", defaultValue = "JSON") @QueryParam("export") String exportType,
            @ApiParam(required = true, value = "Query to run") QueryDTO queryDTO)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, CreationException,
            EntityAlreadyExistsException, EntityConstraintException, NotAllowedException, WorkspaceNotEnabledException {

        Query query = mapper.map(queryDTO, Query.class);
        QueryResult queryResult = getQueryResult(workspaceId, query, exportType);

        if (save) {
            productService.createQuery(workspaceId, query);
        }

        return Response.ok(new GenericEntity<QueryResult>((QueryResult) queryResult) {
        }).build();
    }


    @GET
    @ApiOperation(value = "Filter part master with in baseline : resolve part iteration",
            response = PartIterationDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartIterationDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("{partNumber}/filter/{baselineId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response filterPartMasterInBaseline(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Part number") @PathParam("partNumber") String partNumber,
            @ApiParam(required = true, value = "Baseline id") @PathParam("baselineId") int baselineId)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        PartMaster partMaster = productService.getPartMaster(new PartMasterKey(workspaceId, partNumber));
        ProductStructureFilter filter = filterService.getBaselinePSFilter(baselineId);
        List<PartIteration> partIterations = filter.filter(partMaster);
        if (!partIterations.isEmpty()) {
            return Response.ok().entity(Tools.mapPartIterationToPartIterationDTO(partIterations.get(0))).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{partNumber}/latest-revision")
    @ApiOperation(value = "Get part master latest available revision",
            response = PartRevisionDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestPartRevision(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Part number") @PathParam("partNumber") String partNumber)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        PartMasterKey masterKey = new PartMasterKey(workspaceId, partNumber);
        PartMaster partMaster = productService.getPartMaster(masterKey);
        PartRevision partRevision = partMaster.getLastRevision();

        if (productService.canAccess(partRevision.getKey())) {
            PartRevisionDTO partRevisionDTO = Tools.mapPartRevisionToPartDTO(partRevision);
            return Response.ok(partRevisionDTO).build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();

    }

    @POST
    @ApiOperation(value = "Run and export a custom query",
            response = File.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartIterationDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("query-export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.ms-excel")
    public Response exportCustomQuery(
            @Context HttpServletRequest request,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Choose export type", defaultValue = "JSON") @QueryParam("export") String exportType,
            @ApiParam(required = true, value = "Query to export") QueryDTO queryDTO)
            throws EntityNotFoundException, EntityConstraintException, NotAllowedException,
            UserNotActiveException, WorkspaceNotEnabledException {

        Query query = mapper.map(queryDTO, Query.class);
        User user = userManager.whoAmI(workspaceId);
        return export(workspaceId, query, request, exportType, user.getLocale());
    }

    @GET
    @ApiOperation(value = "Run and export an existing query",
            response = File.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful export"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("queries/{queryId}/format/{export}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.ms-excel")
    public Response exportExistingQuery(
            @Context HttpServletRequest request,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Query id") @PathParam("queryId") int queryId,
            @ApiParam(required = true, value = "Choose export type", defaultValue = "JSON") @PathParam("export") String exportType)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, CreationException,
            EntityAlreadyExistsException, EntityConstraintException, NotAllowedException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Query query = productService.loadQuery(workspaceId, queryId);
        return export(workspaceId, query, request, exportType, user.getLocale());
    }

    @DELETE
    @ApiOperation(value = "Delete custom query",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful deletion of QueryDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("queries/{queryId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQuery(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Query id") @PathParam("queryId") int queryId)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        productService.deleteQuery(workspaceId, queryId);
        return Response.noContent().build();
    }

    @GET
    @ApiOperation(value = "Get checked out part revisions",
            response = PartRevisionDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful deletion of PartRevisionDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("checkedout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCheckedOutPartRevisions(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        PartRevision[] checkedOutPartRevisions = productService.getCheckedOutPartRevisions(workspaceId);
        List<PartRevisionDTO> partRevisionDTOs = new ArrayList<>();

        for (PartRevision partRevision : checkedOutPartRevisions) {
            PartRevisionDTO partRevisionDTO = Tools.mapPartRevisionToPartDTO(partRevision);

            List<ModificationNotificationDTO> notificationDTOs = getModificationNotificationDTOs(partRevision);
            partRevisionDTO.setNotifications(notificationDTOs);

            partRevisionDTOs.add(partRevisionDTO);
        }

        return Response.ok(new GenericEntity<List<PartRevisionDTO>>((List<PartRevisionDTO>) partRevisionDTOs) {
        }).build();
    }

    @GET
    @ApiOperation(value = "Count checked out part revisions",
            response = CountDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTO count"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("countCheckedOut")
    @Produces(MediaType.APPLICATION_JSON)
    public CountDTO getCheckedOutNumberOfItems(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        return new CountDTO(productService.getCheckedOutPartRevisions(workspaceId).length);
    }

    @GET
    @ApiOperation(value = "Search for available part numbers in given workspace",
            response = LightPartMasterDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of LightPartMasterDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("numbers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPartNumbers(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Query") @QueryParam("q") String q)
            throws EntityNotFoundException, AccessRightException, WorkspaceNotEnabledException {

        String search = "%" + q + "%";
        List<PartMaster> partMasters = productService.findPartMasters(Tools.stripTrailingSlash(workspaceId), search, search, 8);
        List<LightPartMasterDTO> partsMastersDTO = new ArrayList<>();
        for (PartMaster p : partMasters) {
            LightPartMasterDTO lightPartMasterDTO = new LightPartMasterDTO(p.getNumber(), p.getName());
            partsMastersDTO.add(lightPartMasterDTO);
        }

        return Response.ok(new GenericEntity<List<LightPartMasterDTO>>((List<LightPartMasterDTO>) partsMastersDTO) {
        }).build();
    }


    @POST
    @ApiOperation(value = "Create a new part master and its first revision",
            response = PartRevisionDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartRevisionDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public PartRevisionDTO createNewPart(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Part to create") PartCreationDTO partCreationDTO)
            throws EntityNotFoundException, EntityAlreadyExistsException, CreationException, AccessRightException,
            NotAllowedException, WorkspaceNotEnabledException {

        String pWorkflowModelId = partCreationDTO.getWorkflowModelId();
        RoleMappingDTO[] roleMappingDTOs = partCreationDTO.getRoleMapping();

        Map<String, Collection<String>> userRoleMapping = new HashMap<>();
        Map<String, Collection<String>> groupRoleMapping = new HashMap<>();

        if (roleMappingDTOs != null) {
            for (RoleMappingDTO roleMappingDTO : roleMappingDTOs) {
                userRoleMapping.put(roleMappingDTO.getRoleName(), roleMappingDTO.getUserLogins());
                groupRoleMapping.put(roleMappingDTO.getRoleName(), roleMappingDTO.getGroupIds());
            }
        }

        ACLDTO acl = partCreationDTO.getAcl();
        Map<String, String> userEntries = acl != null ? acl.getUserEntriesMap() : null;
        Map<String, String> userGroupEntries = acl != null ? acl.getUserGroupEntriesMap() : null;

        PartMaster partMaster = productService.createPartMaster(workspaceId, partCreationDTO.getNumber(), partCreationDTO.getName(), partCreationDTO.isStandardPart(), pWorkflowModelId, partCreationDTO.getDescription(), partCreationDTO.getTemplateId(), userEntries, userGroupEntries, userRoleMapping, groupRoleMapping);
        return Tools.mapPartRevisionToPartDTO(partMaster.getLastRevision());
    }

    @GET
    @ApiOperation(value = "Search documents last iteration to link",
            response = PartIterationDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of PartIterationDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("parts_last_iter")
    @Produces(MediaType.APPLICATION_JSON)
    public PartIterationDTO[] searchPartsLastIterationWithReferenceOrName(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Query") @QueryParam("q") String q,
            @ApiParam(required = false, value = "Max results", defaultValue = "15") @QueryParam("l") int limit)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        int maxResults = limit == 0 ? 15 : limit;
        PartRevision[] partRs = productService.getPartRevisionsWithReferenceOrName(workspaceId, q, maxResults);

        List<PartIterationDTO> partsLastIter = new ArrayList<>();
        for (PartRevision partR : partRs) {
            PartIteration partLastIter = partR.getLastIteration();
            if (partLastIter != null) {
                partsLastIter.add(new PartIterationDTO(partLastIter.getWorkspaceId(), partLastIter.getPartName(), partLastIter.getPartNumber(), partLastIter.getPartVersion(), partLastIter.getIteration()));
            }
        }

        return partsLastIter.toArray(new PartIterationDTO[partsLastIter.size()]);
    }

    @POST
    @ApiOperation(value = "Import part iteration attributes from file",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful import"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPartAttributes(
            @Context HttpServletRequest request,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Auto check out parts flag") @QueryParam("autoCheckout") boolean autoCheckout,
            @ApiParam(required = false, value = "Auto check in parts flag") @QueryParam("autoCheckin") boolean autoCheckin,
            @ApiParam(required = false, value = "Permissive update flag") @QueryParam("permissiveUpdate") boolean permissiveUpdate,
            @ApiParam(required = false, value = "Revision note to add") @QueryParam("revisionNote") String revisionNote,
            @ApiParam(required = true, value = "Import type") @QueryParam("importType") String importType)
            throws Exception {

        Collection<Part> parts = request.getParts();

        if (parts.isEmpty() || parts.size() > 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Part part = parts.iterator().next();

        String fileName = URLDecoder.decode(part.getSubmittedFileName(), "UTF-8");
        String tempFolderName = UUID.randomUUID().toString();

        File importFile = Files.createTempFile(tempFolderName, fileName).toFile();
        BinaryResourceUpload.uploadBinary(new BufferedOutputStream(new FileOutputStream(importFile)), part);

        if ("attributes".equals(importType)) {
            importerService.importIntoParts(workspaceId, importFile, fileName, revisionNote, autoCheckout, autoCheckin, permissiveUpdate);
        } else if ("bom".equals(importType)) {
            importerService.importBom(workspaceId, importFile, fileName, revisionNote, autoCheckout, autoCheckin, permissiveUpdate);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        importFile.deleteOnExit();

        return Response.noContent().build();
    }

    @GET
    @ApiOperation(value = "Get imports currently running for given file",
            response = ImportDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of ImportDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("imports/{filename}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImports(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "File name") @PathParam("filename") String filename)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        List<Import> imports = productService.getImports(workspaceId, filename);
        List<ImportDTO> importDTOs = new ArrayList<>();
        for (Import i : imports) {
            importDTOs.add(mapper.map(i, ImportDTO.class));
        }

        return Response.ok(new GenericEntity<List<ImportDTO>>((List<ImportDTO>) importDTOs) {
        }).build();
    }

    @GET
    @ApiOperation(value = "Get import by id",
            response = ImportDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of ImportDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("import/{importId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public ImportDTO getImport(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Import id") @PathParam("importId") String importId)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        Import anImport = productService.getImport(workspaceId, importId);
        return mapper.map(anImport, ImportDTO.class);
    }

    @DELETE
    @ApiOperation(value = "Delete import",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful deletion of ImportDTOs"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("import/{importId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteImport(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Import id") @PathParam("importId") String importId)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        productService.removeImport(workspaceId, importId);
        return Response.noContent().build();
    }

    @POST
    @Path("importPreview")
    @ApiOperation(value = "Get import preview",
            response = ImportPreviewDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of ImportPreview."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportPreview(
            @Context HttpServletRequest request,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = false, value = "Auto check out parts flag") @QueryParam("autoCheckout") boolean autoCheckout,
            @ApiParam(required = false, value = "Auto check in parts flag") @QueryParam("autoCheckin") boolean autoCheckin,
            @ApiParam(required = false, value = "Permissive update flag") @QueryParam("permissiveUpdate") boolean permissiveUpdate,
            @ApiParam(required = true, value = "Import type") @QueryParam("importType") String importType)

            throws Exception {

        Collection<Part> parts = request.getParts();

        if (parts.isEmpty() || parts.size() > 1) {
            return null;
        }

        Part part = parts.iterator().next();
        String name = FileIO.getFileNameWithoutExtension(part.getSubmittedFileName());
        String extension = FileIO.getExtension(part.getSubmittedFileName());

        File importFile = Files.createTempFile("part-" + name, "-import.tmp" + (extension == null ? "" : "." + extension)).toFile();
        BinaryResourceUpload.uploadBinary(new BufferedOutputStream(new FileOutputStream(importFile)), part);

        ImportPreview importPreview;

        if ("attributes".equals(importType)) {
            importPreview = importerService.dryRunImportIntoParts(workspaceId, importFile, name + "." + extension, autoCheckout, autoCheckin, permissiveUpdate);
        } else if ("bom".equals(importType)) {
            importPreview = importerService.dryRunImportBom(workspaceId, importFile, name + "." + extension, autoCheckout, autoCheckin, permissiveUpdate);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        importFile.deleteOnExit();

        return Response.ok().entity(mapper.map(importPreview, ImportPreviewDTO.class)).build();
    }


    private Response export(String workspaceId, Query query, HttpServletRequest request, String exportType, Locale locale)
            throws BaselineNotFoundException, ProductInstanceMasterNotFoundException, EntityConstraintException, WorkspaceNotFoundException, UserNotFoundException, NotAllowedException, PartMasterNotFoundException, ConfigurationItemNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        QueryResult queryResult = getQueryResult(workspaceId, query, exportType);
        String url = request.getRequestURL().toString();
        String baseURL = url.substring(0, url.length() - request.getRequestURI().length()) + request.getContextPath();
        return makeQueryResponse(queryResult, locale, baseURL);
    }

    private QueryResult getQueryResult(String workspaceId, Query query, String pExportType) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, EntityConstraintException, BaselineNotFoundException, ProductInstanceMasterNotFoundException, NotAllowedException, ConfigurationItemNotFoundException, PartMasterNotFoundException, WorkspaceNotEnabledException {
        List<PartRevision> partRevisions = productService.searchPartRevisions(workspaceId, query);
        QueryResult queryResult = new QueryResult(partRevisions, query);
        if (query.hasContext()) {
            List<QueryResultRow> rows = productService.filterProductBreakdownStructure(workspaceId, query);
            queryResult.mergeRows(rows);
        }
        String exportType = pExportType != null ? pExportType : "JSON";
        queryResult.setExportType(QueryResult.ExportType.valueOf(exportType));
        return queryResult;
    }

    private Response makeQueryResponse(QueryResult queryResult, Locale locale, String baseURL) {
        ExcelGenerator excelGenerator = new ExcelGenerator();
        String contentType = "application/vnd.ms-excel";
        String contentDisposition = "attachment; filename=export_parts.xls";
        Response.ResponseBuilder responseBuilder = Response.ok(excelGenerator.generateXLSResponse(queryResult, locale, baseURL));
        responseBuilder
                .header("Content-Type", contentType)
                .header("Content-Disposition", contentDisposition);

        return responseBuilder.build();
    }


    /**
     * Return a list of ModificationNotificationDTO matching with a given PartRevision
     *
     * @param partRevision The specified PartRevision
     * @return A list of ModificationNotificationDTO
     * @throws EntityNotFoundException If an entity doesn't exist
     * @throws AccessRightException    If the user can not get the modification notifications
     * @throws UserNotActiveException  If the user is disabled
     */
    private List<ModificationNotificationDTO> getModificationNotificationDTOs(PartRevision partRevision)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        PartIterationKey iterationKey = new PartIterationKey(partRevision.getKey(), partRevision.getLastIterationNumber());
        List<ModificationNotification> notifications = productService.getModificationNotifications(iterationKey);
        return Tools.mapModificationNotificationsToModificationNotificationDTO(notifications);
    }

}
