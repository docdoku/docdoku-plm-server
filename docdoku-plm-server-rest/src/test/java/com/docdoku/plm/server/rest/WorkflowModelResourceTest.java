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
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.security.ACLPermission;
import com.docdoku.plm.server.core.services.IWorkflowManagerLocal;
import com.docdoku.plm.server.core.workflow.WorkflowModel;
import com.docdoku.plm.server.core.workflow.WorkflowModelKey;
import com.docdoku.plm.server.rest.dto.*;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

public class WorkflowModelResourceTest {

    @InjectMocks
    private WorkflowModelResource workflowModelResource = new WorkflowModelResource();

    @Mock
    private IWorkflowManagerLocal workflowService;

    private String workspaceId = "wks";
    private Workspace workspace = new Workspace(workspaceId);
    private Account account = new Account("foo");
    private User user = new User(workspace, account);

    @Before
    public void setup() throws Exception {
        initMocks(this);
        workflowModelResource.init();
    }

    @Test
    public void getWorkflowModelsInWorkspaceTest() throws ApplicationException {
        WorkflowModel workflowModel = new WorkflowModel(workspace, "id", user, "state");
        WorkflowModel[] workflowModels = new WorkflowModel[]{workflowModel};
        Mockito.when(workflowService.getWorkflowModels(workspaceId))
                .thenReturn(workflowModels);
        WorkflowModelDTO[] result = workflowModelResource.getWorkflowModelsInWorkspace(workspaceId);
        Assert.assertEquals(workflowModels.length, result.length);
    }

    @Test
    public void getWorkflowModelInWorkspaceTest() throws ApplicationException {
        String workflowModelId = "whatever";
        WorkflowModel workflowModel = new WorkflowModel(workspace, workflowModelId, user, "state");
        Mockito.when(workflowService.getWorkflowModel(new WorkflowModelKey(workspaceId, workflowModelId)))
                .thenReturn(workflowModel);
        WorkflowModelDTO result = workflowModelResource.getWorkflowModelInWorkspace(workspaceId, workflowModelId);
        Assert.assertEquals(workflowModelId, result.getId());
        Assert.assertEquals("state", result.getFinalLifeCycleState());
    }

    @Test
    public void delWorkflowModelTest() throws ApplicationException {
        String workflowModelId = "whatever";
        Mockito.doNothing().when(workflowService)
                .deleteWorkflowModel(new WorkflowModelKey(workspaceId, workflowModelId));
        Response res = workflowModelResource.delWorkflowModel(workspaceId, workflowModelId);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
    }

    @Test
    public void updateWorkflowModelTest() throws ApplicationException {
        String workflowModelId = "whatever";
        WorkflowModel workflowModel = new WorkflowModel(workspace, workflowModelId, user, "state");
        Mockito.when(workflowService.updateWorkflowModel(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any()))
                .thenReturn(workflowModel);
        WorkflowModelDTO workflowModelDTO = new WorkflowModelDTO();
        ActivityModelDTO activityModel1 = new ActivityModelDTO();
        activityModel1.setType(ActivityType.SEQUENTIAL);
        ActivityModelDTO activityModel2 = new ActivityModelDTO();
        activityModel2.setType(ActivityType.SEQUENTIAL);
        activityModel2.setRelaunchStep(0);
        workflowModelDTO.setActivityModels(Arrays.asList(activityModel1, activityModel2));
        WorkflowModelDTO result = workflowModelResource.updateWorkflowModel(workspaceId, workflowModelId, workflowModelDTO);
        Assert.assertNotNull(result);
    }

    @Test
    public void updateWorkflowModelACLTest() throws ApplicationException {
        ACLDTO acl = new ACLDTO();
        String workflowModelId = "whatever";
        Response res = workflowModelResource.updateWorkflowModelACL(workspaceId, workflowModelId, acl);
        Mockito.verify(workflowService, Mockito.times(1)).removeACLFromWorkflow(workspaceId, workflowModelId);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());

        ACLEntryDTO entry = new ACLEntryDTO();
        entry.setKey("k");
        entry.setValue(ACLPermission.READ_ONLY);
        List<ACLEntryDTO> entries = Collections.singletonList(entry);
        acl.setUserEntries(entries);
        res = workflowModelResource.updateWorkflowModelACL(workspaceId, workflowModelId, acl);
        Mockito.verify(workflowService,Mockito.times(1)).updateACLForWorkflow(workspaceId, workflowModelId, acl.getUserEntriesMap(), acl.getUserGroupEntriesMap());
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
    }

    @Test
    public void createWorkflowModelTest() throws ApplicationException {
        String workflowModelId = "whatever";
        WorkflowModel workflowModel = new WorkflowModel(workspace, workflowModelId, user, "state");
        Mockito.when(workflowService.createWorkflowModel(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any()))
                .thenReturn(workflowModel);
        WorkflowModelDTO workflowModelDTO = new WorkflowModelDTO();
        ActivityModelDTO activityModel1 = new ActivityModelDTO();
        activityModel1.setType(ActivityType.SEQUENTIAL);
        ActivityModelDTO activityModel2 = new ActivityModelDTO();
        activityModel2.setType(ActivityType.SEQUENTIAL);
        activityModel2.setRelaunchStep(0);
        workflowModelDTO.setActivityModels(Arrays.asList(activityModel1, activityModel2));
        WorkflowModelDTO result = workflowModelResource.createWorkflowModel(workspaceId, workflowModelDTO);
        Assert.assertNotNull(result);
    }

}
