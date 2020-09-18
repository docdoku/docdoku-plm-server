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
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.ListOfValues;
import com.docdoku.plm.server.core.meta.ListOfValuesKey;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.ILOVManagerLocal;
import com.docdoku.plm.server.rest.dto.ListOfValuesDTO;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lebeaujulien on 03/03/15.
 */

@RequestScoped
@Api(hidden = true, value = "listOfValues", description = "Operations about ListOfValues",
        authorizations = {@Authorization(value = "authorization")})
@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
public class LOVResource {

    @Inject
    private ILOVManagerLocal lovManager;

    private Mapper mapper;

    public LOVResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @GET
    @ApiOperation(value = "Get a list of ListOfValues for given parameters",
            response = ListOfValuesDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of ListOfValuesDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLOVs(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        List<ListOfValuesDTO> LOVDTOs = new ArrayList<>();
        List<ListOfValues> LOVs = lovManager.findLOVFromWorkspace(workspaceId);

        for (ListOfValues lov : LOVs) {
            ListOfValuesDTO lovDTO = mapper.map(lov, ListOfValuesDTO.class);
            lovDTO.setDeletable(lovManager.isLOVDeletable(new ListOfValuesKey(lov.getWorkspaceId(), lov.getName())));
            LOVDTOs.add(lovDTO);
        }
        return Response.ok(new GenericEntity<List<ListOfValuesDTO>>((List<ListOfValuesDTO>) LOVDTOs) {
        }).build();
    }

    @POST
    @ApiOperation(value = "Create a new ListOfValues",
            response = ListOfValuesDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of created ListOfValuesDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLOV(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "LOV to create") ListOfValuesDTO lovDTO)
            throws EntityAlreadyExistsException, EntityNotFoundException, CreationException,
            AccessRightException, UserNotActiveException, WorkspaceNotEnabledException {

        ListOfValues lov = mapper.map(lovDTO, ListOfValues.class);
        lovManager.createLov(workspaceId, lov.getName(), lov.getValues());
        return Tools.prepareCreatedResponse(lov.getName(), lovDTO);
    }

    @GET
    @ApiOperation(value = "Get a ListOfValues from the given parameters",
            response = ListOfValuesDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of ListOfValuesDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ListOfValuesDTO getLOV(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Name") @PathParam("name") String name)
            throws EntityNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        ListOfValuesKey lovKey = new ListOfValuesKey(workspaceId, name);
        ListOfValues lov = lovManager.findLov(lovKey);
        return mapper.map(lov, ListOfValuesDTO.class);
    }

    @PUT
    @Path("/{name}")
    @ApiOperation(value = "Update a ListOfValues",
            response = ListOfValuesDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of updated ListOfValuesDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ListOfValuesDTO updateLOV(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Name") @PathParam("name") String name,
            @ApiParam(required = true, value = "LOV to update") ListOfValuesDTO lovDTO)
            throws EntityNotFoundException, EntityAlreadyExistsException, CreationException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {

        ListOfValuesKey lovKey = new ListOfValuesKey(workspaceId, name);
        ListOfValues lov = mapper.map(lovDTO, ListOfValues.class);

        ListOfValues updatedLOV = lovManager.updateLov(lovKey, lov.getName(), workspaceId, lov.getValues());
        return mapper.map(updatedLOV, ListOfValuesDTO.class);
    }

    @DELETE
    @Path("/{name}")
    @ApiOperation(value = "Delete a ListOfValues",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful deletion of ListOfValuesDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLOV(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Name") @PathParam("name") String name)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, EntityConstraintException, WorkspaceNotEnabledException {
        ListOfValuesKey lovKey = new ListOfValuesKey(workspaceId, name);
        lovManager.deleteLov(lovKey);
        return Response.noContent().build();
    }
}
