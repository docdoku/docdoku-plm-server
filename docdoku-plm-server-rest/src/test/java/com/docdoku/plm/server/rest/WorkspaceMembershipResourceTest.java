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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserGroup;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.security.WorkspaceUserGroupMembership;
import com.docdoku.plm.server.core.security.WorkspaceUserMembership;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.rest.dto.WorkspaceUserGroupMemberShipDTO;
import com.docdoku.plm.server.rest.dto.WorkspaceUserMemberShipDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static org.mockito.MockitoAnnotations.initMocks;

public class WorkspaceMembershipResourceTest {

    @InjectMocks
    private WorkspaceMembershipResource workspaceMembershipResource = new WorkspaceMembershipResource();

    @Mock
    private IUserManagerLocal userManager;

    private String workspaceId = "wks";
    private String login = "foo";
    private String groupId = "bar";
    private Workspace workspace = new Workspace(workspaceId);
    private Account account = new Account(login);
    private User user= new User(workspace, account);
    private UserGroup group = new UserGroup(workspace, groupId);

    @Before
    public void setup() throws Exception {
        initMocks(this);
        workspaceMembershipResource.init();
    }

    @Test
    public void getWorkspaceUserMemberShipsTest() throws ApplicationException {
        WorkspaceUserMembership membership = new WorkspaceUserMembership(workspace, user);
        WorkspaceUserMembership[] list = new WorkspaceUserMembership[]{membership};
        Mockito.when(userManager.getWorkspaceUserMemberships(workspaceId))
                .thenReturn(list);
        WorkspaceUserMemberShipDTO[] workspaceUserMemberShips = workspaceMembershipResource.getWorkspaceUserMemberShips(workspaceId);
        Assert.assertNotNull(workspaceUserMemberShips);
        Assert.assertEquals(list.length, workspaceUserMemberShips.length);
    }

    @Test
    public void getWorkspaceSpecificUserMemberShipsTest() throws ApplicationException {
        WorkspaceUserMembership membership = new WorkspaceUserMembership(workspace, user);
        Mockito.when(userManager.getWorkspaceSpecificUserMemberships(workspaceId))
                .thenReturn(membership);
        WorkspaceUserMemberShipDTO workspaceSpecificUserMemberShips = workspaceMembershipResource.getWorkspaceSpecificUserMemberShips(workspaceId);
        Assert.assertNotNull(workspaceSpecificUserMemberShips);
        Assert.assertEquals(login, workspaceSpecificUserMemberShips.getMember().getLogin());
    }

    @Test
    public void getWorkspaceUserGroupMemberShipsTest() throws ApplicationException {
        WorkspaceUserGroupMembership membership = new WorkspaceUserGroupMembership(workspace, group);
        WorkspaceUserGroupMembership[] list = new WorkspaceUserGroupMembership[]{membership};
        Mockito.when(userManager.getWorkspaceUserGroupMemberships(workspaceId))
                .thenReturn(list);
        WorkspaceUserGroupMemberShipDTO[] workspaceUserGroupMemberShips = workspaceMembershipResource.getWorkspaceUserGroupMemberShips(workspaceId);
        Assert.assertEquals(list.length, workspaceUserGroupMemberShips.length);
    }

    @Test
    public void getWorkspaceSpecificUserGroupMemberShipsTest() throws ApplicationException {
        WorkspaceUserGroupMembership membership = new WorkspaceUserGroupMembership(workspace, group);
        WorkspaceUserGroupMembership[] list = new WorkspaceUserGroupMembership[]{membership, null};
        Mockito.when(userManager.getWorkspaceSpecificUserGroupMemberships(workspaceId))
                .thenReturn(list);
        Response res = workspaceMembershipResource.getWorkspaceSpecificUserGroupMemberShips(workspaceId);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList resultList = (ArrayList) entity;
        Assert.assertEquals(1, resultList.size());
        Object o = resultList.get(0);
        Assert.assertNotNull(o);
        Assert.assertTrue(o.getClass().isAssignableFrom(WorkspaceUserGroupMemberShipDTO.class));
        WorkspaceUserGroupMemberShipDTO result = (WorkspaceUserGroupMemberShipDTO) o;
        Assert.assertEquals(groupId,result.getMemberId());

    }

}
