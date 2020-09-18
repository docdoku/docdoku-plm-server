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
import com.docdoku.plm.server.core.common.*;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.meta.Tag;
import com.docdoku.plm.server.core.notification.TagUserGroupSubscription;
import com.docdoku.plm.server.core.services.INotificationManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.rest.dto.TagSubscriptionDTO;
import com.docdoku.plm.server.rest.dto.UserDTO;
import com.docdoku.plm.server.rest.dto.UserGroupDTO;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.MockitoAnnotations.initMocks;

public class UserGroupResourceTest {

    @InjectMocks
    private UserGroupResource userGroupResource = new UserGroupResource();

    @Mock
    private IUserManagerLocal userManager;

    @Mock
    private INotificationManagerLocal notificationManager;
    private String workspaceId = "wks";
    private Workspace workspace = new Workspace(workspaceId);
    private String groupId = "bar";


    @Before
    public void setup() throws Exception {
        initMocks(this);
        userGroupResource.init();
    }

    @Test
    public void getGroupsTest() throws ApplicationException {
        UserGroup group = new UserGroup(workspace, groupId);
        UserGroup[] groups = new UserGroup[]{group};
        Mockito.when(userManager.getUserGroups(workspaceId))
                .thenReturn(groups);
        UserGroupDTO[] result = userGroupResource.getGroups(workspaceId);
        Assert.assertEquals(groups.length, result.length);
        Assert.assertEquals(groups[0].getId(), result[0].getId());
        Assert.assertEquals(groups[0].getWorkspaceId(), result[0].getWorkspaceId());
    }

    @Test
    public void getTagSubscriptionsForGroupTest() throws ApplicationException {

        Tag tag1 = new Tag(workspace, "tag1");
        Tag tag2 = new Tag(workspace, "tag2");

        UserGroup group1 = new UserGroup(workspace, "group1");
        UserGroup group2 = new UserGroup(workspace, "group2");

        TagUserGroupSubscription tagSubscription1 = new TagUserGroupSubscription(tag1, group1);
        TagUserGroupSubscription tagSubscription2 = new TagUserGroupSubscription(tag2, group2);

        List<TagUserGroupSubscription> subscriptions = Arrays.asList(
                tagSubscription1, tagSubscription2
        );

        Mockito.when(notificationManager.getTagUserGroupSubscriptionsByGroup(workspaceId, groupId))
                .thenReturn(subscriptions);

        TagSubscriptionDTO[] subscriptionsDTO = userGroupResource.getTagSubscriptionsForGroup(workspaceId, groupId);
        Assert.assertEquals(subscriptions.size(), subscriptionsDTO.length);

    }

    @Test
    public void getUsersInGroupTest() throws ApplicationException {
        UserGroup group = new UserGroup(workspace, groupId);
        Set<User> users = new HashSet<>();
        Account account = new Account("foo");
        User user = new User(workspace, account);
        users.add(user);
        group.setUsers(users);
        Mockito.when(userManager.getUserGroup(new UserGroupKey(workspaceId, groupId)))
                .thenReturn(group);
        UserDTO[] result = userGroupResource.getUsersInGroup(workspaceId, groupId);
        Assert.assertEquals(1, result.length);

        Assert.assertEquals(user.getLogin(), result[0].getLogin());
    }

    @Test
    public void updateUserGroupSubscriptionTest() throws ApplicationException{
        String tagName = "foo";
        TagSubscriptionDTO subDTO = new TagSubscriptionDTO();
        subDTO.setOnIterationChange(true);
        subDTO.setOnStateChange(true);
        Mockito.when(notificationManager.createOrUpdateTagUserGroupSubscription(workspaceId,
                groupId, tagName, subDTO.isOnIterationChange(), subDTO.isOnStateChange()))
        .thenReturn(null);
        Response res = userGroupResource.updateUserGroupSubscription(workspaceId, groupId, tagName, subDTO);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());

        // cannot cover full method, as the UnsupportedEncodingException cannot be thrown
        // ...
    }

    @Test
    public void deleteUserGroupSubscriptionTest() throws ApplicationException {
        String tagName = "whatever";
        Mockito.doNothing().when(notificationManager)
                .removeTagUserGroupSubscription(workspaceId, groupId, tagName);
        Response response = userGroupResource.deleteUserGroupSubscription(workspaceId, groupId, tagName);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

}
