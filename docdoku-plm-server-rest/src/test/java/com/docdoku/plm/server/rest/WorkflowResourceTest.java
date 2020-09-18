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
import com.docdoku.plm.server.core.workflow.Workflow;
import com.docdoku.plm.server.rest.dto.WorkflowDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Morgan Guimard
 */
public class WorkflowResourceTest {

    @InjectMocks
    private WorkflowResource workflowResource = new WorkflowResource();

    @Mock
    private IWorkflowManagerLocal workflowService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        workflowResource.init();
    }

    @Test
    public void getWorkflowInstanceTest() throws ApplicationException {
        int workflowId = 42;

        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        Mockito.when(workflowService.getWorkflow(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).thenReturn(workflow);

        WorkflowDTO workflowDTO = workflowResource.getWorkflowInstance("wks", workflowId);
        Assert.assertNotNull(workflowDTO);
        Assert.assertEquals(workflowId, workflowDTO.getId());
    }

    @Test
    public void getWorkflowAbortedWorkflowListTest() throws ApplicationException {
        int workflowId = 42;
        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        Workflow[] list = new Workflow[]{workflow};
        Mockito.when(workflowService.getWorkflowAbortedWorkflowList(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).thenReturn(list);

        Response res = workflowResource.getWorkflowAbortedWorkflowList("wks", workflowId);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        List workflowList = (ArrayList) entity;
        Object workflowEntity = workflowList.get(0);
        Assert.assertTrue(workflowEntity.getClass().isAssignableFrom(WorkflowDTO.class));
        WorkflowDTO workflowDTO = (WorkflowDTO) workflowEntity;
        Assert.assertNotNull(workflowDTO);
        Assert.assertEquals(workflowId, workflowDTO.getId());
    }
}
