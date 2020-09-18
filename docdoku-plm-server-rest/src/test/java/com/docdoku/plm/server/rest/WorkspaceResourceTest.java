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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.exceptions.AccessRightException;
import com.docdoku.plm.server.core.exceptions.EntityNotFoundException;
import com.docdoku.plm.server.core.security.WorkspaceUserMembership;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.rest.dto.UserDTO;
import com.docdoku.plm.server.rest.dto.WorkspaceMembership;

import javax.ws.rs.core.Response;


public class WorkspaceResourceTest {

    @InjectMocks
    WorkspaceResource workspaceResource = new WorkspaceResource();

    @Mock
    private IUserManagerLocal userManager;

    @Mock
    private UserDTO userDTO;

    @Mock
    private WorkspaceUserMembership workspaceUserMembership;

    @Before
    public void setUp(){

        MockitoAnnotations.initMocks(this);
        workspaceResource.init();
    }

    @Test
    public void setUserAccessTest() throws EntityNotFoundException, AccessRightException {

        Mockito.when(userDTO.getMembership()).thenReturn(WorkspaceMembership.READ_ONLY);
        Mockito.when(userManager.grantUserAccess("wks-0001", userDTO.getLogin(), userDTO.getMembership() == WorkspaceMembership.READ_ONLY)).thenReturn(null);
        Response response =  workspaceResource.setUserAccess("wks-0001",userDTO);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),response.getStatus());

        Mockito.when(userDTO.getMembership()).thenReturn(null);
        response =  workspaceResource.setUserAccess("wks-0001",userDTO);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),response.getStatus());

        Mockito.when(userDTO.getMembership()).thenReturn(WorkspaceMembership.READ_ONLY);
        Mockito.when(userManager.grantUserAccess("wks-0001", userDTO.getLogin(), userDTO.getMembership() == WorkspaceMembership.READ_ONLY))
                .thenReturn(workspaceUserMembership);
        Mockito.when(workspaceUserMembership.getMember()).thenReturn(new User());
        response =  workspaceResource.setUserAccess("wks-0001",userDTO);
        Assert.assertNotNull(response.getEntity());
    }
}