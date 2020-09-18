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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import com.docdoku.plm.server.core.exceptions.PlatformHealthException;
import com.docdoku.plm.server.core.services.IPlatformHealthManagerLocal;
import com.docdoku.plm.server.rest.dto.PlatformHealthDTO;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Api(value = "Platforms", description = "Operations about platform")
@Path("platform")
public class PlatformResource {

    @Inject
    private IPlatformHealthManagerLocal platformHealthManager;

    public PlatformResource() {
    }

    @GET
    @Path("health")
    @ApiOperation(value = "Get platform health status",
            response = PlatformHealthDTO.class,
            authorizations = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Server health is ok. A JSON object is sent in the body"),
            @ApiResponse(code = 500, message = "Server health is ko or partial")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformHealthDTO getPlatformHealthStatus() throws PlatformHealthException {
        long before=System.currentTimeMillis();
        platformHealthManager.runHealthCheck();
        long after=System.currentTimeMillis();
        PlatformHealthDTO platformHealthDTO = new PlatformHealthDTO();
        platformHealthDTO.setStatus("ok");
        platformHealthDTO.setExecutionTime(after-before);
        return platformHealthDTO;
    }
}
