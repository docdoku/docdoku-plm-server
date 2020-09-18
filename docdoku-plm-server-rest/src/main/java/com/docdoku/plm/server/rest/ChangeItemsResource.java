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
import io.swagger.annotations.Authorization;
import com.docdoku.plm.server.core.security.UserGroupMapping;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

@RequestScoped
@Api(hidden = true, value = "changeItems", description = "Operations about change items",
        authorizations = {@Authorization(value = "authorization")})
@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
public class ChangeItemsResource {

    @Inject
    private ChangeIssuesResource issues;
    @Inject
    private ChangeRequestsResource requests;
    @Inject
    private ChangeOrdersResource orders;
    @Inject
    private MilestonesResource milestones;

    public ChangeItemsResource() {
    }

    @Path("/issues")
    @ApiOperation(value = "SubResource : ChangeIssuesResource")
    public ChangeIssuesResource issues() {
        return issues;
    }

    @Path("/requests")
    @ApiOperation(value = "SubResource : ChangeRequestsResource")
    public ChangeRequestsResource requests() {
        return requests;
    }

    @Path("/orders")
    @ApiOperation(value = "SubResource : ChangeOrdersResource")
    public ChangeOrdersResource orders() {
        return orders;
    }

    @Path("/milestones")
    @ApiOperation(value = "SubResource : MilestonesResource")
    public MilestonesResource milestones() {
        return milestones;
    }

}
