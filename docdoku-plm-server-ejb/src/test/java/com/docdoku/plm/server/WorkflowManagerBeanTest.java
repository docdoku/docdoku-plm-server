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

package com.docdoku.plm.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserGroup;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.AccessRightException;
import com.docdoku.plm.server.core.security.ACL;
import com.docdoku.plm.server.core.security.ACLPermission;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.core.workflow.WorkflowModel;
import com.docdoku.plm.server.core.workflow.WorkflowModelKey;
import com.docdoku.plm.server.dao.WorkflowModelDAO;
import com.docdoku.plm.server.factory.ACLFactory;
import com.docdoku.plm.server.util.WorkflowUtil;

import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.MockitoAnnotations.initMocks;

public class WorkflowManagerBeanTest {
    @InjectMocks
    private WorkflowManagerBean workflowManagerBean = new WorkflowManagerBean();

    @Mock
    private IUserManagerLocal userManager;

    @Mock
    private TypedQuery<ACL> aclTypedQuery;

    @Mock
    private WorkflowModelDAO workflowModelDAO;

    @Mock
    private ACLFactory aclFactory;

    private User user;
    private Workspace workspace;
    private WorkflowModelKey workflowModelKey;

    @Before
    public void setup() {
        initMocks(this);
        Account account = new Account(WorkflowUtil.ADMIN_LOGIN, WorkflowUtil.ADMIN_NAME, WorkflowUtil.ADMIN_MAIL, "en", new Date(), null);
        workspace = new Workspace(WorkflowUtil.WORKSPACE_ID, account, WorkflowUtil.WORKSPACE_DESCRIPTION, false);
        user = new User(workspace,new Account(WorkflowUtil.USER_LOGIN, WorkflowUtil.USER_NAME,WorkflowUtil.USER_MAIL, "en", new Date(), null));
        workflowModelKey = new WorkflowModelKey(WorkflowUtil.WORKSPACE_ID, WorkflowUtil.WORKFLOW_MODEL_ID);
    }

    /**
     * test the remove of acl from a workflow operated by user who doesn't have write access to the workflow
     * @throws Exception
     */
    @Test(expected = AccessRightException.class)
    public void testRemoveACLFromWorkflow() throws Exception {
        //Given
        WorkflowModel workflowModel = new WorkflowModel(workspace, WorkflowUtil.WORKSPACE_ID, user, "");
        ACL acl = new ACL();
        acl.addEntry(user, ACLPermission.READ_ONLY);
        workflowModel.setAcl(acl);

        Mockito.when(userManager.checkWorkspaceReadAccess(WorkflowUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(workflowModelDAO.loadWorkflowModel(workflowModelKey)).thenReturn(workflowModel);
        
        //When
        workflowManagerBean.removeACLFromWorkflow(WorkflowUtil.WORKSPACE_ID, WorkflowUtil.WORKFLOW_MODEL_ID);

        //Then, removeACLFromWorkflow should throw AccessRightException, user doesn't have write access to the workflow
    }

    /**
     * create an ACL of the workflow, user is the admin of the workspace
     * @throws Exception
     */
    @Test
    public void testUpdateACLForWorkflowWithNoACL() throws Exception {
        //Given
        WorkflowModel workflowModel = new WorkflowModel(workspace, WorkflowUtil.WORKSPACE_ID, user, "");
        Map<String, String> userEntries = new HashMap<>();
        User user2 = new User(workspace,new Account(WorkflowUtil.USER2_LOGIN , WorkflowUtil.USER2_NAME,WorkflowUtil.USER2_MAIL, "en", new Date(), null));
        User user3 = new User(workspace,new Account(WorkflowUtil.USER3_LOGIN , WorkflowUtil.USER3_NAME,WorkflowUtil.USER3_MAIL, "en", new Date(), null));
        userEntries.put(user.getLogin(), ACLPermission.FORBIDDEN.name());
        userEntries.put(user2.getLogin(), ACLPermission.READ_ONLY.name());
        userEntries.put(user3.getLogin(), ACLPermission.FULL_ACCESS.name());
        ACL acl = new ACL();
        acl.addEntry(user, ACLPermission.FORBIDDEN);
        acl.addEntry(user2, ACLPermission.READ_ONLY);
        acl.addEntry(user3, ACLPermission.FULL_ACCESS);

        Mockito.when(userManager.checkWorkspaceReadAccess(WorkflowUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(workflowModelDAO.loadWorkflowModel(workflowModelKey)).thenReturn(workflowModel);
        Mockito.when(aclFactory.createACL(workspace.getId(), userEntries, null)).thenReturn(acl);

        //When
        WorkflowModel workflow= workflowManagerBean.updateACLForWorkflow(WorkflowUtil.WORKSPACE_ID, WorkflowUtil.WORKFLOW_MODEL_ID, userEntries, null);

        //Then
        Assert.assertEquals(0, workflow.getAcl().getGroupEntries().size());
        Assert.assertEquals(3, workflow.getAcl().getUserEntries().size());
        Assert.assertEquals(ACLPermission.FORBIDDEN, workflow.getAcl().getUserEntries().get(user).getPermission());
        Assert.assertEquals(ACLPermission.READ_ONLY, workflow.getAcl().getUserEntries().get(user2).getPermission());
        Assert.assertEquals(ACLPermission.FULL_ACCESS, workflow.getAcl().getUserEntries().get(user3).getPermission());
    }

    @Test
    public void testUpdateACLForWorkflowWithAnExistingACL() throws Exception {
        //Given
        Map<String, String> userEntries = new HashMap<>();
        Map<String, String> grpEntries = new HashMap<>();
        User user2 = new User(workspace,new Account(WorkflowUtil.USER2_LOGIN , WorkflowUtil.USER2_NAME,WorkflowUtil.USER2_MAIL, "en", new Date(), null));
        User user3 = new User(workspace,new Account(WorkflowUtil.USER3_LOGIN , WorkflowUtil.USER3_NAME,WorkflowUtil.USER3_MAIL, "en", new Date(), null));
        UserGroup group1 = new UserGroup(workspace,WorkflowUtil.GRP1_ID);

        WorkflowModel workflowModel = new WorkflowModel(workspace, WorkflowUtil.WORKSPACE_ID, user, "");
        ACL acl = new ACL();
        acl.addEntry(user2, ACLPermission.READ_ONLY);
        acl.addEntry(group1, ACLPermission.FULL_ACCESS);

        ACL expectedAcl = new ACL();
        expectedAcl.addEntry(user, ACLPermission.FORBIDDEN);
        expectedAcl.addEntry(user2, ACLPermission.FORBIDDEN);
        expectedAcl.addEntry(user3, ACLPermission.FULL_ACCESS);
        expectedAcl.addEntry(group1, ACLPermission.FULL_ACCESS);

        workflowModel.setAcl(acl);

        userEntries.put(user.getLogin(), ACLPermission.FORBIDDEN.name());
        userEntries.put(user2.getLogin(), ACLPermission.FORBIDDEN.name());
        userEntries.put(user3.getLogin(), ACLPermission.FULL_ACCESS.name());

        group1.addUser(user2);
        group1.addUser(user);

        grpEntries.put(group1.getId(),ACLPermission.FULL_ACCESS.name());

        Mockito.when(userManager.checkWorkspaceReadAccess(workspace.getId())).thenReturn(user);
        Mockito.when(workflowModelDAO.loadWorkflowModel(workflowModelKey)).thenReturn(workflowModel);
        Mockito.when(aclTypedQuery.setParameter(ArgumentMatchers.anyString(),ArgumentMatchers.any())).thenReturn(aclTypedQuery);
        Mockito.when(aclFactory.updateACL(workspace.getId(), acl, userEntries, grpEntries)).thenReturn(expectedAcl);

        //When
        WorkflowModel workflow= workflowManagerBean.updateACLForWorkflow(WorkflowUtil.WORKSPACE_ID, WorkflowUtil.WORKFLOW_MODEL_ID, userEntries, grpEntries);

        //Then
        Assert.assertEquals(1, workflow.getAcl().getGroupEntries().size());
        Assert.assertEquals(3, workflow.getAcl().getUserEntries().size());
        Assert.assertEquals(ACLPermission.FORBIDDEN, workflow.getAcl().getUserEntries().get(user).getPermission());
        Assert.assertEquals(ACLPermission.FORBIDDEN, workflow.getAcl().getUserEntries().get(user2).getPermission());
        Assert.assertEquals(ACLPermission.FULL_ACCESS, workflow.getAcl().getUserEntries().get(user3).getPermission());
    }
}
