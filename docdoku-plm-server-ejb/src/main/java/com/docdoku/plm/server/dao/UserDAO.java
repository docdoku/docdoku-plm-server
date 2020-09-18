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
package com.docdoku.plm.server.dao;

import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserKey;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.Folder;
import com.docdoku.plm.server.core.security.WorkspaceUserMembership;
import com.docdoku.plm.server.core.security.WorkspaceUserMembershipKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RequestScoped
public class UserDAO {

    @Inject
    private EntityManager em;

    @Inject
    private ACLDAO aclDAO;

    @Inject
    private FolderDAO folderDAO;

    @Inject
    private UserGroupDAO userGroupDAO;

    public UserDAO() {
    }

    public User loadUser(UserKey pUserKey) throws UserNotFoundException {
        User user = em.find(User.class, pUserKey);
        if (user == null) {
            throw new UserNotFoundException(pUserKey.getAccount());
        } else {
            return user;
        }
    }

    public WorkspaceUserMembership loadUserMembership(WorkspaceUserMembershipKey pKey) {
        return em.find(WorkspaceUserMembership.class, pKey);
    }

    public void addUserMembership(Workspace pWorkspace, User pMember) {
        WorkspaceUserMembership ms = em.find(WorkspaceUserMembership.class, new WorkspaceUserMembershipKey(pWorkspace.getId(), pWorkspace.getId(), pMember.getLogin()));
        if (ms == null) {
            ms = new WorkspaceUserMembership(pWorkspace, pMember);
            em.persist(ms);
        }
    }

    public void removeUserMembership(WorkspaceUserMembershipKey pKey) {
        WorkspaceUserMembership ms = em.find(WorkspaceUserMembership.class, pKey);
        if (ms != null) {
            em.remove(ms);
        }
    }

    public User[] findAllUsers(String pWorkspaceId) {
        User[] users;
        Query query = em.createQuery("SELECT DISTINCT u FROM User u WHERE u.workspaceId = :workspaceId");
        List listUsers = query.setParameter("workspaceId", pWorkspaceId).getResultList();
        users = new User[listUsers.size()];
        for (int i = 0; i < listUsers.size(); i++) {
            users[i] = (User) listUsers.get(i);
        }
        return users;
    }

    public WorkspaceUserMembership[] findAllWorkspaceUserMemberships(String pWorkspaceId) {
        WorkspaceUserMembership[] memberships;
        Query query = em.createQuery("SELECT DISTINCT m FROM WorkspaceUserMembership m WHERE m.workspaceId = :workspaceId");
        List listMemberships = query.setParameter("workspaceId", pWorkspaceId).getResultList();
        memberships = new WorkspaceUserMembership[listMemberships.size()];
        for (int i = 0; i < listMemberships.size(); i++) {
            memberships[i] = (WorkspaceUserMembership) listMemberships.get(i);
        }

        return memberships;
    }

    public void removeUser(User pUser) throws EntityConstraintException {
        removeUserMembership(new WorkspaceUserMembershipKey(pUser.getWorkspaceId(), pUser.getWorkspaceId(), pUser.getLogin()));
        userGroupDAO.removeUserFromAllGroups(pUser);
        aclDAO.removeAclUserEntries(pUser);
        try {
            em.remove(pUser);
            em.flush();
        } catch (PersistenceException pPEx) {
            throw new EntityConstraintException("EntityConstraintException27");
        }
    }

    public void createUser(User pUser) throws UserAlreadyExistsException, FolderAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pUser);
            em.flush();
            folderDAO.createFolder(new Folder(pUser.getWorkspaceId() + "/~" + pUser.getLogin()));
        } catch (EntityExistsException pEEEx) {
            throw new UserAlreadyExistsException(pUser);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException();
        }
    }

    public User[] getUsers(String pLogin) {
        User[] users;
        Query query = em.createQuery("SELECT DISTINCT u FROM User u WHERE u.login = :login");
        List listUsers = query.setParameter("login", pLogin).getResultList();
        users = new User[listUsers.size()];
        for (int i = 0; i < listUsers.size(); i++) {
            users[i] = (User) listUsers.get(i);
        }

        return users;
    }

    public User[] findReachableUsersForCaller(String callerLogin) {

        Map<String, User> users = new TreeMap<>();

        List<String> listWorkspaceId = em.createQuery("SELECT u.workspaceId FROM User u WHERE u.login = :login", String.class)
                .setParameter("login", callerLogin).getResultList();

        if (!listWorkspaceId.isEmpty()) {

            List<User> listUsers = em.createQuery("SELECT u FROM User u where u.workspaceId IN :workspacesId", User.class)
                    .setParameter("workspacesId", listWorkspaceId).getResultList();

            for (User user : listUsers) {
                String loginUser = user.getLogin();
                if (!users.keySet().contains(loginUser)) {
                    users.put(loginUser, user);
                }
            }

        }

        return users.values().toArray(new User[0]);
    }

    public boolean hasCommonWorkspace(String userLogin1, String userLogin2) {
        return !em.createNamedQuery("findCommonWorkspacesForGivenUsers").
                setParameter("userLogin1", userLogin1).
                setParameter("userLogin2", userLogin2).
                getResultList().
                isEmpty();
    }

}
