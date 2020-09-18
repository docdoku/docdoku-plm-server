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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.services.IWorkflowManagerLocal;
import com.docdoku.plm.server.core.workflow.WorkspaceWorkflow;
import com.docdoku.plm.server.rest.dto.RoleMappingDTO;
import com.docdoku.plm.server.rest.dto.WorkspaceWorkflowCreationDTO;
import com.docdoku.plm.server.rest.dto.WorkspaceWorkflowDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Morgan Guimard
 */
public class WorkspaceWorkflowResourceTest {

    @InjectMocks
    private WorkspaceWorkflowResource workspaceWorkflowResource = new WorkspaceWorkflowResource();

    @Mock
    private IWorkflowManagerLocal workflowService;

    private String workspaceId = "wks";

    @Before
    public void init() {
        initMocks(this);
        workspaceWorkflowResource.init();
    }

    @Test
    public void getWorkspaceWorkflowListTest() throws ApplicationException {
        WorkspaceWorkflow workflow = new WorkspaceWorkflow();
        workflow.setId("id");
        WorkspaceWorkflow[] list = new WorkspaceWorkflow[]{workflow};
        Mockito.when(workflowService.getWorkspaceWorkflowList(workspaceId))
                .thenReturn(list);

        Response res = workspaceWorkflowResource.getWorkspaceWorkflowList(workspaceId);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList result = (ArrayList) entity;
        Object o = result.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(WorkspaceWorkflowDTO.class));
        WorkspaceWorkflowDTO workflowDTO = (WorkspaceWorkflowDTO) o;
        Assert.assertEquals(workflow.getId(), String.valueOf(workflowDTO.getId()));
    }

    @Test
    public void getWorkspaceWorkflowTest() throws ApplicationException {
        String workspaceWorkflowId = "id";
        WorkspaceWorkflow workflow = new WorkspaceWorkflow();
        Mockito.when(workflowService.getWorkspaceWorkflow(workspaceId, workspaceWorkflowId))
                .thenReturn(workflow);
        WorkspaceWorkflowDTO workspaceWorkflow = workspaceWorkflowResource.getWorkspaceWorkflow(workspaceId,
                workspaceWorkflowId);
        Assert.assertEquals(workflow.getId(), workspaceWorkflow.getId());
    }

    @Test
    public void createWorkspaceWorkflowTest() throws ApplicationException {
        WorkspaceWorkflowCreationDTO workflowCreationDTO = new WorkspaceWorkflowCreationDTO();
        workflowCreationDTO.setId("id");
        RoleMappingDTO roleMappingDTO = new RoleMappingDTO();
        roleMappingDTO.setRoleName("role");
        List<String> loginList = Arrays.asList("foo", "bar");
        roleMappingDTO.setUserLogins(loginList);
        RoleMappingDTO[] roleMapping = new RoleMappingDTO[]{roleMappingDTO};
        workflowCreationDTO.setRoleMapping(roleMapping);
        workflowCreationDTO.setWorkflowModelId("modelId");

        WorkspaceWorkflow workspaceWorkflow = new WorkspaceWorkflow();
        workspaceWorkflow.setId("id");

        Mockito.when(workflowService.instantiateWorkflow(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(workspaceWorkflow);

        WorkspaceWorkflowDTO result = workspaceWorkflowResource.createWorkspaceWorkflow(workspaceId,
                workflowCreationDTO);

        Assert.assertEquals(workflowCreationDTO.getId(), result.getId());

    }

    @Test
    public void deleteWorkspaceWorkflowTest() throws ApplicationException {
        Mockito.doNothing().when(workflowService).deleteWorkspaceWorkflow(workspaceId, "id");
        Response res = workspaceWorkflowResource.deleteWorkspaceWorkflow(workspaceId, "id");
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
    }

}
