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

import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserGroupKey;
import com.docdoku.plm.server.core.common.UserKey;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.TagKey;
import com.docdoku.plm.server.core.notification.TagUserGroupSubscription;
import com.docdoku.plm.server.core.notification.TagUserGroupSubscriptionKey;
import com.docdoku.plm.server.core.notification.TagUserSubscription;
import com.docdoku.plm.server.core.notification.TagUserSubscriptionKey;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.INotificationManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.dao.SubscriptionDAO;
import com.docdoku.plm.server.dao.TagDAO;
import com.docdoku.plm.server.dao.UserDAO;
import com.docdoku.plm.server.dao.UserGroupDAO;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.*;

/**
 * @author Florent Garin on 07/09/16
 */
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(INotificationManagerLocal.class)
@Stateless(name = "NotificationManagerBean")
public class NotificationManagerBean implements INotificationManagerLocal {

    @Inject
    private EntityManager em;

    @Inject
    private SubscriptionDAO subscriptionDAO;

    @Inject
    private TagDAO tagDAO;

    @Inject
    private UserDAO userDAO;

    @Inject
    private UserGroupDAO userGroupDAO;

    @Inject
    private IUserManagerLocal userManager;

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public TagUserSubscription subscribeToTagEvent(String pWorkspaceId, String pLabel, boolean pOnIterationChange, boolean pOnStateChange) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, TagNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        TagUserSubscription subscription = new TagUserSubscription(
                tagDAO.loadTag(new TagKey(pWorkspaceId, pLabel)),
                user,
                pOnIterationChange, pOnStateChange);
        return subscriptionDAO.saveTagUserSubscription(subscription);
    }


    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void unsubscribeToTagEvent(String pWorkspaceId, String pLabel) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        subscriptionDAO.removeTagUserSubscription(new TagUserSubscriptionKey(pWorkspaceId, user.getLogin(), pLabel));
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public TagUserSubscription createOrUpdateTagUserSubscription(String pWorkspaceId, String pLogin, String pLabel, boolean pOnIterationChange, boolean pOnStateChange) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, TagNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            TagUserSubscription subscription = new TagUserSubscription(
                    tagDAO.loadTag(new TagKey(pWorkspaceId, pLabel)),
                    userDAO.loadUser(new UserKey(pWorkspaceId, pLogin)),
                    pOnIterationChange, pOnStateChange);
            return subscriptionDAO.saveTagUserSubscription(subscription);
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public TagUserGroupSubscription createOrUpdateTagUserGroupSubscription(String pWorkspaceId, String pId, String pLabel, boolean pOnIterationChange, boolean pOnStateChange) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, TagNotFoundException, UserGroupNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            TagUserGroupSubscription subscription = new TagUserGroupSubscription(
                    tagDAO.loadTag(new TagKey(pWorkspaceId, pLabel)),
                    userGroupDAO.loadUserGroup(new UserGroupKey(pWorkspaceId, pId)),
                    pOnIterationChange, pOnStateChange);
            return subscriptionDAO.saveTagUserGroupSubscription(subscription);
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeTagUserSubscription(String pWorkspaceId, String pLogin, String pLabel) throws AccessRightException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            subscriptionDAO.removeTagUserSubscription(new TagUserSubscriptionKey(pWorkspaceId, pLogin, pLabel));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeAllTagSubscriptions(String pWorkspaceId, String pLabel) throws TagNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            subscriptionDAO.removeAllTagSubscriptions(tagDAO.loadTag(new TagKey(pWorkspaceId, pLabel)));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeAllTagUserSubscriptions(String pWorkspaceId, String pLogin) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            subscriptionDAO.removeAllTagSubscriptions(userDAO.loadUser(new UserKey(pWorkspaceId, pLogin)));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeAllTagUserGroupSubscriptions(String pWorkspaceId, String pGroupId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, UserGroupNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            subscriptionDAO.removeAllTagSubscriptions(userGroupDAO.loadUserGroup(new UserGroupKey(pWorkspaceId, pGroupId)));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeAllSubscriptions(String pWorkspaceId, String pLogin) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            subscriptionDAO.removeAllSubscriptions(userDAO.loadUser(new UserKey(pWorkspaceId, pLogin)));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeTagUserGroupSubscription(String pWorkspaceId, String pId, String pLabel) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            subscriptionDAO.removeTagUserGroupSubscription(new TagUserGroupSubscriptionKey(pWorkspaceId, pId, pLabel));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<TagUserGroupSubscription> getTagUserGroupSubscriptionsByGroup(String pWorkspaceId, String pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, UserGroupNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            return subscriptionDAO.getTagUserGroupSubscriptionsByGroup(userGroupDAO.loadUserGroup(new UserGroupKey(pWorkspaceId, pId)));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<TagUserSubscription> getTagUserSubscriptionsByUser(String pWorkspaceId, String pLogin) throws AccessRightException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Check if it is the workspace's administrator
        if (user.isAdministrator()) {
            return subscriptionDAO.getTagUserSubscriptionsByUser(userDAO.loadUser(new UserKey(pWorkspaceId, pLogin)));
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Collection<User> getSubscribersForTag(String pWorkspaceId, String pLabel) {
        Set<User> users=new HashSet<>();

        List<User> listUsers = em.createNamedQuery("TagUserSubscription.findSubscribersByTags", User.class)
                .setParameter("workspaceId", pWorkspaceId)
                .setParameter("tags", Collections.singletonList(pLabel))
                .getResultList();
        users.addAll(listUsers);

        listUsers = em.createNamedQuery("TagUserGroupSubscription.findSubscribersByTags", User.class)
                .setParameter("workspaceId", pWorkspaceId)
                .setParameter("tags", Collections.singletonList(pLabel))
                .getResultList();
        users.addAll(listUsers);

        return users;
    }
}
