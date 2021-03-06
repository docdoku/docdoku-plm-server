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
import com.docdoku.plm.server.core.hooks.SNSWebhookApp;
import com.docdoku.plm.server.core.hooks.SimpleWebhookApp;
import com.docdoku.plm.server.core.hooks.Webhook;
import com.docdoku.plm.server.core.hooks.WebhookApp;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IWebhookManagerLocal;
import com.docdoku.plm.server.rest.dto.WebhookAppParameterDTO;
import com.docdoku.plm.server.rest.dto.WebhookDTO;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@RequestScoped
@Api(hidden = true, value = "webhook", description = "Operations about webhooks",
        authorizations = {@Authorization(value = "authorization")})
@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
public class WebhookResource {

    @Inject
    private IWebhookManagerLocal webhookManager;

    private Mapper mapper;

    public WebhookResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }


    @GET
    @ApiOperation(value = "Get webhooks in given workspace",
            response = WebhookDTO.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of WebhookDTOs. It can be an empty list."),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WebhookDTO[] getWebhooks(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId
    ) throws EntityNotFoundException, AccessRightException {
        List<Webhook> webHooks = webhookManager.getWebHooks(workspaceId);
        List<WebhookDTO> webHookDTOs = new ArrayList<>();
        for (Webhook webhook : webHooks) {
            webHookDTOs.add(mapper.map(webhook, WebhookDTO.class));
        }
        return webHookDTOs.toArray(new WebhookDTO[webHookDTOs.size()]);
    }

    @GET
    @Path("/{webhookId}")
    @ApiOperation(value = "Get webhook by id",
            response = WebhookDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of WebhookDTO"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WebhookDTO getWebhook(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Webhook id") @PathParam("webhookId") int webhookId
    ) throws EntityNotFoundException, AccessRightException, WebhookNotFoundException {
        Webhook webHook = webhookManager.getWebHook(workspaceId, webhookId);
        WebhookDTO dto = mapper.map(webHook, WebhookDTO.class);
        return dto;
    }

    @POST
    @ApiOperation(value = "Create a new webhook in given workspace",
            response = WebhookDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful creation of webhook"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WebhookDTO createWebhook(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Webhook definition") WebhookDTO webhookDTO
    ) throws AccessRightException, UserNotActiveException, EntityNotFoundException, WorkspaceNotEnabledException{
        Webhook webHook = webhookManager.createWebhook(workspaceId, webhookDTO.getName(), webhookDTO.isActive());
        WebhookApp webhookApp = configureWebhook(workspaceId, webHook.getId(), webhookDTO);
        webHook.setWebhookApp(webhookApp);
        return mapper.map(webHook, WebhookDTO.class);
    }

    @PUT
    @Path("/{webhookId}")
    @ApiOperation(value = "Update a webhook",
            response = WebhookDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful update of webhook"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Webhook not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WebhookDTO updateWebhook(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Webhook id") @PathParam("webhookId") Integer webhookId,
            @ApiParam(required = true, value = "Webhook definition") WebhookDTO webhookDTO
    ) throws AccessRightException, EntityNotFoundException {
        Webhook webHook = webhookManager.updateWebHook(workspaceId, webhookId, webhookDTO.getName(), webhookDTO.isActive());
        WebhookApp webhookApp = configureWebhook(workspaceId, webhookId, webhookDTO);
        webHook.setWebhookApp(webhookApp);
        return mapper.map(webHook, WebhookDTO.class);
    }

    @DELETE
    @Path("/{webhookId}")
    @ApiOperation(value = "Delete a webhook",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful delete of webhook"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Webhook not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteWebhook(
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") String workspaceId,
            @ApiParam(required = true, value = "Webhook id") @PathParam("webhookId") int webhookId
    ) throws AccessRightException, EntityNotFoundException {
        webhookManager.deleteWebhook(workspaceId, webhookId);
        return Response.noContent().build();
    }


    private WebhookApp configureWebhook(String workspaceId, int webhookId, WebhookDTO webhookDTO) throws WorkspaceNotFoundException, AccessRightException, WebhookNotFoundException, AccountNotFoundException {
        List<WebhookAppParameterDTO> parameters = webhookDTO.getParameters();
        String appName = webhookDTO.getAppName();
        if (appName != null && parameters != null && !parameters.isEmpty()) {
            switch (appName) {
                case SimpleWebhookApp.APP_NAME:
                    return updateSimpleWebhook(workspaceId, webhookId, parameters);
                case SNSWebhookApp.APP_NAME:
                    return updateSNSWebhook(workspaceId, webhookId, parameters);
                default:
                    break;
            }
        }
        // default case, use simple http webhook
        return webhookManager.configureSimpleWebhook(workspaceId, webhookId, "POST", null, null);
    }

    private WebhookApp updateSNSWebhook(String workspaceId, int webhookId, List<WebhookAppParameterDTO> parameters)
            throws WorkspaceNotFoundException, AccessRightException, WebhookNotFoundException, AccountNotFoundException {
        String region = null;
        String topicArn = null;
        String awsAccount = null;
        String awsSecret = null;
        for (WebhookAppParameterDTO parameter : parameters) {
            if(parameter.getName()==null) continue;
            switch(parameter.getName()){
                case "topicArn":
                    topicArn = parameter.getValue();
                    break;
                case "region":
                    region = parameter.getValue();
                    break;
                case "awsAccount":
                    awsAccount = parameter.getValue();
                    break;
                case "awsSecret":
                    awsSecret = parameter.getValue();
                    break;
            }

        }
        return webhookManager.configureSNSWebhook(workspaceId, webhookId, topicArn, region, awsAccount, awsSecret);
    }

    private WebhookApp updateSimpleWebhook(String workspaceId, int webhookId, List<WebhookAppParameterDTO> parameters)
            throws WorkspaceNotFoundException, AccessRightException, WebhookNotFoundException, AccountNotFoundException {
        String method = null;
        String uri = null;
        String authorization = null;
        for (WebhookAppParameterDTO parameter : parameters) {
            if(parameter.getName()==null) continue;

            switch(parameter.getName()){
                case "method":
                    method = parameter.getValue();
                    break;
                case "uri":
                    uri = parameter.getValue();
                    break;
                case "authorization":
                    authorization = parameter.getValue();
                    break;
            }
        }
        return webhookManager.configureSimpleWebhook(workspaceId, webhookId, method, uri, authorization);
    }

}
