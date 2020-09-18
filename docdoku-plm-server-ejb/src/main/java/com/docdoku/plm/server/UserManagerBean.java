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

import com.docdoku.plm.server.core.common.*;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.security.*;
import com.docdoku.plm.server.core.services.IContextManagerLocal;
import com.docdoku.plm.server.core.services.INotifierLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.config.ServerConfig;
import com.docdoku.plm.server.dao.*;
import com.docdoku.plm.server.events.*;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(IUserManagerLocal.class)
@Stateless(name = "UserManagerBean")
public class UserManagerBean implements IUserManagerLocal {

    @Inject
    private EntityManager em;

    @Inject
    private AccountDAO accountDAO;

    @Inject
    private UserDAO userDAO;

    @Inject
    private UserGroupDAO userGroupDAO;

    @Inject
    private WorkspaceDAO workspaceDAO;

    @Inject
    private Event<WorkspaceAccessEvent> workspaceAccessEvent;

    @Inject
    private Event<UserEvent> userEvent;

    @Inject
    private Event<UserGroupEvent> groupEvent;

    @Inject
    private IContextManagerLocal contextManager;

    @Inject
    private INotifierLocal mailer;

    @Inject
    private ServerConfig serverConfig;

    @Inject
    private PasswordRecoveryRequestDAO passwordRecoveryRequestDAO;

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void addUserInGroup(UserGroupKey pGroupKey, String pLogin) throws AccessRightException, UserGroupNotFoundException, AccountNotFoundException, WorkspaceNotFoundException, UserAlreadyExistsException, FolderAlreadyExistsException, CreationException {
        checkAdmin(pGroupKey.getWorkspaceId());
        User userToAdd = em.find(User.class, new UserKey(pGroupKey.getWorkspaceId(), pLogin));
        if (userToAdd == null) {
            Account userAccount = accountDAO.loadAccount(pLogin);
            Workspace workspace = em.getReference(Workspace.class, pGroupKey.getWorkspaceId());
            userToAdd = new User(workspace, userAccount);
            userDAO.createUser(userToAdd);
        }
        UserGroup group = userGroupDAO.loadUserGroup(pGroupKey);

        userDAO.removeUserMembership(new WorkspaceUserMembershipKey(pGroupKey.getWorkspaceId(), pGroupKey.getWorkspaceId(), pLogin));
        group.addUser(userToAdd);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void addUserInWorkspace(String pWorkspaceId, String pLogin) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException, UserAlreadyExistsException, FolderAlreadyExistsException, CreationException {
        checkAdmin(pWorkspaceId);
        User userToAdd = em.find(User.class, new UserKey(pWorkspaceId, pLogin));
        Workspace workspace = em.getReference(Workspace.class, pWorkspaceId);
        if (userToAdd == null) {
            Account userAccount = accountDAO.loadAccount(pLogin);
            userToAdd = new User(workspace, userAccount);
            userDAO.createUser(userToAdd);
        }
        userDAO.addUserMembership(workspace, userToAdd);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Workspace removeUser(String pWorkspaceId, String login) throws UserNotFoundException, AccessRightException, AccountNotFoundException, WorkspaceNotFoundException, EntityConstraintException {
        Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
        Workspace workspace = workspaceDAO.loadWorkspace(pWorkspaceId);
        checkAdmin(workspace, account);

        User user = userDAO.loadUser(new UserKey(pWorkspaceId, login));

        userEvent.select(new AnnotationLiteral<Removed>() {
        }).fire(new UserEvent(user));
        userDAO.removeUser(user);

        return workspace;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void removeUserFromGroup(UserGroupKey pGroupKey, String[] pLogins) throws AccessRightException, UserGroupNotFoundException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pGroupKey.getWorkspaceId());
        UserGroup group = userGroupDAO.loadUserGroup(pGroupKey);
        for (String login : pLogins) {
            User userToRemove = em.getReference(User.class, new UserKey(pGroupKey.getWorkspaceId(), login));
            group.removeUser(userToRemove);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public UserGroup removeUserFromGroup(UserGroupKey pGroupKey, String login) throws AccessRightException, UserGroupNotFoundException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pGroupKey.getWorkspaceId());
        UserGroup group = userGroupDAO.loadUserGroup(pGroupKey);
        User userToRemove = em.getReference(User.class, new UserKey(pGroupKey.getWorkspaceId(), login));
        group.removeUser(userToRemove);
        return group;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public UserGroup createUserGroup(String pId, Workspace pWorkspace) throws UserGroupAlreadyExistsException, AccessRightException, AccountNotFoundException, CreationException {
        checkAdmin(pWorkspace);
        UserGroup groupToCreate = new UserGroup(pWorkspace, pId);
        userGroupDAO.createUserGroup(groupToCreate);
        userGroupDAO.addUserGroupMembership(pWorkspace, groupToCreate);
        return groupToCreate;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public UserGroup createUserGroup(String pId, String workspaceId) throws UserGroupAlreadyExistsException, AccessRightException, AccountNotFoundException, CreationException, WorkspaceNotFoundException {
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        return createUserGroup(pId, workspace);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public UserGroup[] getUserGroups(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccountNotFoundException, WorkspaceNotEnabledException {
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
            return userGroupDAO.findAllUserGroups(pWorkspaceId);
        } else {
            checkWorkspaceReadAccess(pWorkspaceId);
            return userGroupDAO.findAllUserGroups(pWorkspaceId);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public UserGroup getUserGroup(UserGroupKey pKey) throws WorkspaceNotFoundException, UserGroupNotFoundException, UserNotFoundException, UserNotActiveException, AccountNotFoundException, WorkspaceNotEnabledException {
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            return userGroupDAO.loadUserGroup(pKey);
        } else {
            checkWorkspaceReadAccess(pKey.getWorkspaceId());
            return userGroupDAO.loadUserGroup(pKey);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public WorkspaceUserMembership getWorkspaceSpecificUserMemberships(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = checkWorkspaceReadAccess(pWorkspaceId);
        return userDAO.loadUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, user.getLogin()));
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public WorkspaceUserMembership[] getWorkspaceUserMemberships(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccountNotFoundException, WorkspaceNotEnabledException {
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
            return userDAO.findAllWorkspaceUserMemberships(pWorkspaceId);
        } else {
            checkWorkspaceReadAccess(pWorkspaceId);
            return userDAO.findAllWorkspaceUserMemberships(pWorkspaceId);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public WorkspaceUserGroupMembership[] getWorkspaceSpecificUserGroupMemberships(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, UserGroupNotFoundException, WorkspaceNotEnabledException {
        User user = checkWorkspaceReadAccess(pWorkspaceId);
        List<UserGroup> userGroups = userGroupDAO.getUserGroups(pWorkspaceId, user);
        WorkspaceUserGroupMembership[] workspaceUserGroupMembership = new WorkspaceUserGroupMembership[userGroups.size()];
        for (int i = 0; i < userGroups.size(); i++) {
            workspaceUserGroupMembership[i] = userGroupDAO.loadUserGroupMembership(
                    new WorkspaceUserGroupMembershipKey(pWorkspaceId, pWorkspaceId, userGroups.get(i).getId()));
        }
        return workspaceUserGroupMembership;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public WorkspaceUserGroupMembership[] getWorkspaceUserGroupMemberships(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccountNotFoundException, WorkspaceNotEnabledException {
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
            return userGroupDAO.findAllWorkspaceUserGroupMemberships(pWorkspaceId);
        } else {
            checkWorkspaceReadAccess(pWorkspaceId);
            return userGroupDAO.findAllWorkspaceUserGroupMemberships(pWorkspaceId);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void grantUserAccess(String pWorkspaceId, String[] pLogins, boolean pReadOnly) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        for (String login : pLogins) {
            WorkspaceUserMembership ms = userDAO.loadUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));
            if (ms != null) {
                ms.setReadOnly(pReadOnly);
            }
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public WorkspaceUserMembership grantUserAccess(String pWorkspaceId, String login, boolean pReadOnly) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        WorkspaceUserMembership ms = userDAO.loadUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));
        if (ms != null) {
            ms.setReadOnly(pReadOnly);
        }
        return ms;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public WorkspaceUserGroupMembership grantGroupAccess(String pWorkspaceId, String groupId, boolean pReadOnly) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException, UserGroupNotFoundException {
        checkAdmin(pWorkspaceId);
        WorkspaceUserGroupMembership ms = userGroupDAO.loadUserGroupMembership(
                new WorkspaceUserGroupMembershipKey(pWorkspaceId, pWorkspaceId, groupId));
        if (ms != null) {
            ms.setReadOnly(pReadOnly);
        }

        return ms;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void grantGroupAccess(String pWorkspaceId, String[] pGroupIds, boolean pReadOnly) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException, UserGroupNotFoundException {
        checkAdmin(pWorkspaceId);
        for (String id : pGroupIds) {
            WorkspaceUserGroupMembership ms = userGroupDAO.loadUserGroupMembership(new WorkspaceUserGroupMembershipKey(pWorkspaceId, pWorkspaceId, id));
            if (ms != null) {
                ms.setReadOnly(pReadOnly);
            }
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void activateUsers(String pWorkspaceId, String[] pLogins) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        Workspace workspace = em.getReference(Workspace.class, pWorkspaceId);
        for (String login : pLogins) {
            User member = em.getReference(User.class, new UserKey(pWorkspaceId, login));
            userDAO.addUserMembership(workspace, member);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void activateUserGroups(String pWorkspaceId, String[] pGroupIds) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        Workspace workspace = em.getReference(Workspace.class, pWorkspaceId);
        for (String id : pGroupIds) {
            UserGroup member = em.getReference(UserGroup.class, new UserGroupKey(pWorkspaceId, id));
            userGroupDAO.addUserGroupMembership(workspace, member);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void activateUser(String pWorkspaceId, String login) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        Workspace workspace = em.getReference(Workspace.class, pWorkspaceId);
        User member = em.getReference(User.class, new UserKey(pWorkspaceId, login));
        userDAO.addUserMembership(workspace, member);

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void activateUserGroup(String pWorkspaceId, String groupId) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        Workspace workspace = em.getReference(Workspace.class, pWorkspaceId);
        UserGroup member = em.getReference(UserGroup.class, new UserGroupKey(pWorkspaceId, groupId));
        userGroupDAO.addUserGroupMembership(workspace, member);

    }


    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void passivateUserGroups(String pWorkspaceId, String[] pGroupIds) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        for (String id : pGroupIds) {
            userGroupDAO.removeUserGroupMembership(new WorkspaceUserGroupMembershipKey(pWorkspaceId, pWorkspaceId, id));
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void passivateUsers(String pWorkspaceId, String[] pLogins) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        for (String login : pLogins) {
            userDAO.removeUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void passivateUserGroup(String pWorkspaceId, String groupId) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        userGroupDAO.removeUserGroupMembership(new WorkspaceUserGroupMembershipKey(pWorkspaceId, pWorkspaceId, groupId));

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void removeUsers(String pWorkspaceId, String[] pLogins) throws UserNotFoundException, AccessRightException, AccountNotFoundException, WorkspaceNotFoundException, EntityConstraintException {
        checkAdmin(pWorkspaceId);
        for (String login : pLogins) {
            User user = userDAO.loadUser(new UserKey(pWorkspaceId, login));
            userEvent.select(new AnnotationLiteral<Removed>() {
            }).fire(new UserEvent(user));
            userDAO.removeUser(user);
        }

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void passivateUser(String pWorkspaceId, String login) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        checkAdmin(pWorkspaceId);
        userDAO.removeUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void removeUserGroups(String pWorkspaceId, String[] pIds) throws UserGroupNotFoundException, AccessRightException, AccountNotFoundException, WorkspaceNotFoundException, EntityConstraintException {
        checkAdmin(pWorkspaceId);
        for (String id : pIds) {
            UserGroupKey userGroupKey = new UserGroupKey(pWorkspaceId, id);
            if (userGroupDAO.hasACLConstraint(userGroupKey)) {
                throw new EntityConstraintException("EntityConstraintException11");
            }
            UserGroup group = userGroupDAO.loadUserGroup(userGroupKey);
            groupEvent.select(new AnnotationLiteral<Removed>() {
            }).fire(new UserGroupEvent(group));
            userGroupDAO.removeUserGroup(group);
        }
    }


    @Override
    public void recoverPassword(String pPasswdRRUuid, String pPassword) throws PasswordRecoveryRequestNotFoundException {
        PasswordRecoveryRequest passwdRR = passwordRecoveryRequestDAO.loadPasswordRecoveryRequest(pPasswdRRUuid);
        accountDAO.updateCredential(passwdRR.getLogin(), pPassword, serverConfig.getDigestAlgorithm());
        passwordRecoveryRequestDAO.removePasswordRecoveryRequest(passwdRR);
    }

    @Override
    public PasswordRecoveryRequest createPasswordRecoveryRequest(Account account) {
        PasswordRecoveryRequest recoveryRequest = PasswordRecoveryRequest.createPasswordRecoveryRequest(account.getLogin());
        em.persist(recoveryRequest);
        mailer.sendPasswordRecovery(account, recoveryRequest.getUuid());
        return recoveryRequest;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public User checkWorkspaceReadAccess(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        String login = contextManager.getCallerPrincipalLogin();
        WorkspaceUserMembership userMS = userDAO.loadUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));
        Workspace wks = workspaceDAO.loadWorkspace(pWorkspaceId);
        User user = userDAO.loadUser(new UserKey(pWorkspaceId, login));

        if (!wks.isEnabled()) {
            throw new WorkspaceNotEnabledException(pWorkspaceId);
        } else if (userMS != null) {
            user = userMS.getMember();
        } else if (!wks.getAdmin().getLogin().equals(login)) {
            WorkspaceUserGroupMembership[] groupMS = userGroupDAO.getUserGroupMemberships(pWorkspaceId, user);
            if (groupMS.length == 0) {
                throw new UserNotActiveException(login);
            }
        }

        workspaceAccessEvent.select(new AnnotationLiteral<Read>() {
        }).fire(new WorkspaceAccessEvent(user));

        return user;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public User checkWorkspaceWriteAccess(String pWorkspaceId) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        String login = contextManager.getCallerPrincipalLogin();

        User user = userDAO.loadUser(new UserKey(pWorkspaceId, login));
        if (!hasWorkspaceWriteAccess(user, pWorkspaceId)) {
            throw new AccessRightException(user);
        }
        workspaceAccessEvent.select(new AnnotationLiteral<Write>() {
        }).fire(new WorkspaceAccessEvent(user));

        return user;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public boolean hasWorkspaceWriteAccess(User user, String pWorkspaceId) throws WorkspaceNotFoundException, WorkspaceNotEnabledException {
        String login = contextManager.getCallerPrincipalLogin();

        Workspace wks = workspaceDAO.loadWorkspace(pWorkspaceId);
        if (!wks.isEnabled()) {
            throw new WorkspaceNotEnabledException(pWorkspaceId);
        }
        if (!wks.getAdmin().getLogin().equals(login)) {
            WorkspaceUserMembership userMS = userDAO.loadUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));
            if (userMS != null) {
                return !userMS.isReadOnly();
            } else {
                WorkspaceUserGroupMembership[] groupMS = userGroupDAO.getUserGroupMemberships(pWorkspaceId, user);
                boolean readOnly = true;
                for (WorkspaceUserGroupMembership ms : groupMS) {
                    if (!ms.isReadOnly()) {
                        readOnly = false;
                        break;
                    }
                }
                return !readOnly;
            }
        }
        return true;
    }


    /*
    * Don't expose this method on remote.
    * Method returns true if given users have a common workspace, false otherwise.
    */
    @Override
    public boolean hasCommonWorkspace(String userLogin1, String userLogin2) {
        return userLogin1 != null && userLogin2 != null && userDAO.hasCommonWorkspace(userLogin1, userLogin2);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public UserGroup[] getUserGroupsForUser(UserKey userKey) throws UserNotFoundException {
        User user = userDAO.loadUser(userKey);
        List<UserGroup> userGroups = userGroupDAO.getUserGroups(userKey.getWorkspace(), user);
        return userGroups.toArray(new UserGroup[0]);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Workspace[] getWorkspacesWhereCallerIsActive() {
        String callerLogin = contextManager.getCallerPrincipalLogin();
        List<Workspace> workspaces = workspaceDAO.findWorkspacesWhereUserIsActive(callerLogin);
        return workspaces.stream().filter(Workspace::isEnabled).toArray(Workspace[]::new);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Account checkAdmin(Workspace pWorkspace) throws AccessRightException, AccountNotFoundException {
        Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
        checkAdmin(pWorkspace, account);
        return account;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Account checkAdmin(String pWorkspaceId) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
        Workspace wks = workspaceDAO.loadWorkspace(pWorkspaceId);
        checkAdmin(wks, account);
        return account;
    }

    private void checkAdmin(Workspace workspace, Account account) throws AccessRightException {
        if (!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID) && !workspace.getAdmin().equals(account)) {
            throw new AccessRightException(account);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public User whoAmI(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        return checkWorkspaceReadAccess(pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public User[] getReachableUsers() throws AccountNotFoundException {
        String callerLogin = contextManager.getCallerPrincipalLogin();
        accountDAO.loadAccount(callerLogin);
        return userDAO.findReachableUsersForCaller(callerLogin);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public User[] getUsers(String pWorkspaceId) throws WorkspaceNotFoundException, AccountNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
            return userDAO.findAllUsers(pWorkspaceId);
        }
        checkWorkspaceReadAccess(pWorkspaceId);
        return userDAO.findAllUsers(pWorkspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Workspace[] getAdministratedWorkspaces() throws AccountNotFoundException {
        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            return accountDAO.getAllWorkspaces();
        } else {
            Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
            Workspace[] workspaces = accountDAO.getAdministratedWorkspaces(account);
            return Stream.of(workspaces).filter(Workspace::isEnabled).toArray(Workspace[]::new);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean isUserEnabled(String login, String pWorkspaceId) throws AccountNotFoundException, UserNotFoundException {
        String callerLogin = contextManager.getCallerPrincipalLogin();
        accountDAO.loadAccount(callerLogin);
        WorkspaceUserMembership userMS = userDAO.loadUserMembership(new WorkspaceUserMembershipKey(pWorkspaceId, pWorkspaceId, login));
        User user = userDAO.loadUser(new UserKey(pWorkspaceId, login));
        WorkspaceUserGroupMembership[] groupMS = userGroupDAO.getUserGroupMemberships(pWorkspaceId, user);
        return userMS != null || groupMS.length > 0;
    }
}
