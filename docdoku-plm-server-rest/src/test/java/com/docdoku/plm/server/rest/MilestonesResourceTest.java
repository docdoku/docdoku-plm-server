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
import com.docdoku.plm.server.core.change.ChangeOrder;
import com.docdoku.plm.server.core.change.ChangeRequest;
import com.docdoku.plm.server.core.change.Milestone;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.security.ACLPermission;
import com.docdoku.plm.server.core.services.IChangeManagerLocal;
import com.docdoku.plm.server.rest.dto.ACLDTO;
import com.docdoku.plm.server.rest.dto.ACLEntryDTO;
import com.docdoku.plm.server.rest.dto.change.ChangeOrderDTO;
import com.docdoku.plm.server.rest.dto.change.ChangeRequestDTO;
import com.docdoku.plm.server.rest.dto.change.MilestoneDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

public class MilestonesResourceTest {

    @InjectMocks
    private MilestonesResource milestonesResource = new MilestonesResource();

    @Mock
    private IChangeManagerLocal changeManager;

    private String workspaceId = "wks";

    @Before
    public void setup() throws Exception {
        initMocks(this);
        milestonesResource.init();
    }


    @Test
    public void getMilestonesTest() throws ApplicationException {
        List<Milestone> list = new ArrayList<>();
        Workspace workspace = new Workspace(workspaceId);
        Milestone milestone = new Milestone(workspace, "title");

        list.add(milestone);

        Mockito.when(changeManager.getMilestones(workspaceId))
                .thenReturn(list);

        Mockito.when(changeManager.isMilestoneWritable(milestone))
                .thenReturn(true);

        Mockito.when(changeManager.getNumberOfRequestByMilestone(workspaceId, milestone.getId()))
                .thenReturn(1);

        Mockito.when(changeManager.getNumberOfOrderByMilestone(workspaceId, milestone.getId()))
                .thenReturn(1);

        Response res = milestonesResource.getMilestones(workspaceId);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
        Assert.assertTrue(res.getEntity().getClass().isAssignableFrom(ArrayList.class));

    }

    @Test
    public void createMilestoneTest() throws ApplicationException {

        Workspace workspace = new Workspace(workspaceId);
        Milestone milestone = new Milestone(workspace, "title");

        MilestoneDTO milestoneDTO = new MilestoneDTO();

        Mockito.when(changeManager.createMilestone(workspaceId, milestoneDTO.getTitle(), milestoneDTO.getDescription(), milestoneDTO.getDueDate()))
                .thenReturn(milestone);

        MilestoneDTO milestoneDTOCreated = milestonesResource.createMilestone(workspaceId, milestoneDTO);
        Assert.assertNotNull(milestoneDTOCreated);
        Assert.assertTrue(milestoneDTOCreated.isWritable());
        Assert.assertEquals(milestone.getTitle(), milestoneDTOCreated.getTitle());
    }

    @Test
    public void getMilestoneTest() throws ApplicationException {
        int milestoneId = 22;
        Workspace workspace = new Workspace(workspaceId);
        Milestone milestone = new Milestone(workspace, "title");

        Mockito.when(changeManager.getMilestone(workspaceId, milestoneId))
                .thenReturn(milestone);

        Mockito.when(changeManager.isMilestoneWritable(milestone))
                .thenReturn(true);

        Mockito.when(changeManager.getNumberOfRequestByMilestone(workspaceId, milestone.getId()))
                .thenReturn(1);

        Mockito.when(changeManager.getNumberOfOrderByMilestone(workspaceId, milestone.getId()))
                .thenReturn(1);


        MilestoneDTO milestoneDTO = milestonesResource.getMilestone(workspaceId, milestoneId);
        Assert.assertNotNull(milestoneDTO);
    }

    @Test
    public void updateMilestoneTest() throws ApplicationException {
        int milestoneId = 22;
        Workspace workspace = new Workspace(workspaceId);
        Milestone milestone = new Milestone(workspace, "title");
        MilestoneDTO pMilestoneDTO = new MilestoneDTO();

        Mockito.when(changeManager.updateMilestone(milestoneId, workspaceId, pMilestoneDTO.getTitle(), pMilestoneDTO.getDescription(), pMilestoneDTO.getDueDate()))
                .thenReturn(milestone);

        Mockito.when(changeManager.isMilestoneWritable(milestone))
                .thenReturn(true);

        Mockito.when(changeManager.getNumberOfRequestByMilestone(workspaceId, milestone.getId()))
                .thenReturn(1);

        Mockito.when(changeManager.getNumberOfOrderByMilestone(workspaceId, milestone.getId()))
                .thenReturn(1);

        MilestoneDTO milestoneDTO = milestonesResource.updateMilestone(workspaceId, milestoneId, pMilestoneDTO);
        Assert.assertNotNull(milestoneDTO);
        Assert.assertEquals(milestone.getTitle(), milestoneDTO.getTitle());

    }


    @Test
    public void removeMilestoneTest() throws ApplicationException {
        int milestoneId = 22;
        Mockito.doNothing().when(changeManager).deleteMilestone(workspaceId, milestoneId);
        Response response = milestonesResource.removeMilestone(workspaceId, milestoneId);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

    }

    @Test
    public void getRequestsByMilestoneTest() throws ApplicationException {
        int milestoneId = 22;

        List<ChangeRequest> requests = new ArrayList<>();
        ChangeRequest request = new ChangeRequest();
        Workspace workspace = new Workspace(workspaceId);
        request.setWorkspace(workspace);
        Account account = new Account("foo");
        User author = new User(workspace, account);
        request.setAuthor(author);
        request.setName("name");
        requests.add(request);

        Mockito.when(changeManager.getChangeRequestsByMilestone(workspaceId, milestoneId))
            .thenReturn(requests);

        Response response = milestonesResource.getRequestsByMilestone(workspaceId, milestoneId);
        Object entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        List requestList = (ArrayList) entity;
        Assert.assertFalse(requestList.isEmpty());
        Object o = requestList.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(ChangeRequestDTO.class));
        ChangeRequestDTO dto = (ChangeRequestDTO) o;
        Assert.assertEquals(request.getName(), dto.getName());

    }

    @Test
    public void getOrdersByMilestoneTest() throws ApplicationException {
        int milestoneId = 22;

        List<ChangeOrder> orders = new ArrayList<>();
        ChangeOrder order = new ChangeOrder();
        Workspace workspace = new Workspace(workspaceId);
        order.setWorkspace(workspace);
        Account account = new Account("foo");
        User author = new User(workspace, account);
        order.setAuthor(author);
        order.setName("name");
        orders.add(order);

        Mockito.when(changeManager.getChangeOrdersByMilestone(workspaceId, milestoneId))
                .thenReturn(orders);

        Response response = milestonesResource.getOrdersByMilestone(workspaceId, milestoneId);
        Object entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        List orderList = (ArrayList) entity;
        Assert.assertFalse(orderList.isEmpty());
        Object o = orderList.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(ChangeOrderDTO.class));
        ChangeOrderDTO dto = (ChangeOrderDTO) o;
        Assert.assertEquals(order.getName(), dto.getName());


    }

    @Test
    public void updateMilestoneACLTest() throws ApplicationException {

        int milestoneId = 22;
        ACLDTO acl = new ACLDTO();

        ACLEntryDTO userEntry = new ACLEntryDTO();
        userEntry.setKey("key");
        userEntry.setValue(ACLPermission.FULL_ACCESS);

        ACLEntryDTO groupEntry = new ACLEntryDTO();
        groupEntry.setKey("key");
        groupEntry.setValue(ACLPermission.FULL_ACCESS);

        acl.setUserEntries(Collections.singletonList(userEntry));
        acl.setGroupEntries(Collections.singletonList(groupEntry));

        Mockito.when(changeManager.updateACLForMilestone(workspaceId, milestoneId, acl.getUserEntriesMap(), acl.getUserGroupEntriesMap()))
            .thenReturn(null);

        Response response = milestonesResource.updateMilestoneACL(workspaceId, milestoneId, acl);
        Mockito.verify(changeManager, Mockito.times(1))
                .updateACLForMilestone(workspaceId, milestoneId, acl.getUserEntriesMap(), acl.getUserGroupEntriesMap());

        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        acl.setUserEntries(new ArrayList<>());
        acl.setGroupEntries(new ArrayList<>());

        Mockito.when(changeManager.removeACLFromMilestone(workspaceId, milestoneId))
                .thenReturn(null);

        response = milestonesResource.updateMilestoneACL(workspaceId, milestoneId, acl);

        Mockito.verify(changeManager, Mockito.times(1))
                .removeACLFromMilestone(workspaceId, milestoneId);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());



    }

}
