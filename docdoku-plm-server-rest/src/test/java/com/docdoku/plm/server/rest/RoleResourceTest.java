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
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.services.IWorkflowManagerLocal;
import com.docdoku.plm.server.core.workflow.Role;
import com.docdoku.plm.server.core.workflow.RoleKey;
import com.docdoku.plm.server.rest.dto.RoleDTO;
import com.docdoku.plm.server.rest.dto.UserDTO;
import com.docdoku.plm.server.rest.dto.UserGroupDTO;

import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Morgan Guimard
 */
public class RoleResourceTest {

    @InjectMocks
    private RoleResource roleResource = new RoleResource();

    @Mock
    private IWorkflowManagerLocal roleService;

    private String workspaceId = "wks";
    private Workspace workspace = new Workspace("wks");

    @Before
    public void setup(){
        initMocks(this);
        roleResource.init();
    }

    @Test
    public void getRolesInWorkspaceTest() throws ApplicationException{
        Role role = new Role();
        role.setWorkspace(workspace);
        role.setName("role");
        Role[] roles = new Role[]{role};

        Mockito.when(roleService.getRoles(workspaceId))
                .thenReturn(roles);
        Response res = roleResource.getRolesInWorkspace(workspaceId);

        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList result = (ArrayList) entity;
        Assert.assertEquals(roles.length, result.size());
        Object o = result.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(RoleDTO.class));
        RoleDTO roleDTO = (RoleDTO) o;
        Assert.assertEquals(role.getName(), roleDTO.getName());
        Assert.assertEquals(role.getWorkspace().getId(), roleDTO.getWorkspaceId());
    }

    @Test
    public void getRolesInUseInWorkspaceTest() throws ApplicationException{
        Role role = new Role();
        role.setWorkspace(workspace);
        role.setName("role");
        Role[] roles = new Role[]{role};

        Mockito.when(roleService.getRolesInUse(workspaceId))
                .thenReturn(roles);
        Response res = roleResource.getRolesInUseInWorkspace(workspaceId);

        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList result = (ArrayList) entity;
        Assert.assertEquals(roles.length, result.size());
        Object o = result.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(RoleDTO.class));
        RoleDTO roleDTO = (RoleDTO) o;
        Assert.assertEquals(role.getName(), roleDTO.getName());
        Assert.assertEquals(role.getWorkspace().getId(), roleDTO.getWorkspaceId());
    }

    @Test
    public void createRoleTest() throws ApplicationException{
        Role role = new Role();
        role.setWorkspace(workspace);
        role.setName("role");

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setName("role");
        roleDTO.setWorkspaceId(workspaceId);
        UserDTO user = new UserDTO();
        String login ="foo";
        user.setLogin(login);
        List<UserDTO> users = Collections.singletonList(user);
        roleDTO.setDefaultAssignedUsers(users);

        UserGroupDTO group = new UserGroupDTO();
        String groupId = "bar";
        group.setId(groupId);
        List<UserGroupDTO> groups = Collections.singletonList(group);
        roleDTO.setDefaultAssignedGroups(groups);
        List<String> userLogins = Collections.singletonList(login);
        List<String> userGroupIds  = Collections.singletonList(groupId);

        Mockito.when(roleService.createRole(roleDTO.getName(), roleDTO.getWorkspaceId(), userLogins, userGroupIds))
                .thenReturn(role);

        Response res = roleResource.createRole(workspaceId, roleDTO);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());

    }

    @Test
    public void updateRoleTest() throws ApplicationException{
        Role role = new Role();
        role.setWorkspace(workspace);
        role.setName("role");

        RoleKey roleKey = new RoleKey(role.getWorkspace().getId(), role.getName());

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setName("role");
        roleDTO.setWorkspaceId(workspaceId);
        UserDTO user = new UserDTO();
        String login ="foo";
        user.setLogin(login);
        List<UserDTO> users = Collections.singletonList(user);
        roleDTO.setDefaultAssignedUsers(users);

        UserGroupDTO group = new UserGroupDTO();
        String groupId = "bar";
        group.setId(groupId);
        List<UserGroupDTO> groups = Collections.singletonList(group);
        roleDTO.setDefaultAssignedGroups(groups);
        List<String> userLogins = Collections.singletonList(login);
        List<String> userGroupIds  = Collections.singletonList(groupId);

        Mockito.when(roleService.updateRole(roleKey, userLogins, userGroupIds))
                .thenReturn(role);

        Response res = roleResource.updateRole(workspaceId, role.getName(), roleDTO);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

    }

    @Test
    public void deleteRoleTest() throws ApplicationException{
        RoleKey roleKey = new RoleKey(workspaceId, "role");
        Mockito.doNothing().when(roleService).deleteRole(roleKey);
        Response res = roleResource.deleteRole(workspaceId, roleKey.getName());
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
    }

}
