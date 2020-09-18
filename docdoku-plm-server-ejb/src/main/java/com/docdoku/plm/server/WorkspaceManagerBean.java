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


import com.docdoku.plm.server.core.admin.OperationSecurityStrategy;
import com.docdoku.plm.server.core.admin.WorkspaceBackOptions;
import com.docdoku.plm.server.core.admin.WorkspaceFrontOptions;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.core.util.NamingConvention;
import com.docdoku.plm.server.dao.AccountDAO;
import com.docdoku.plm.server.dao.UserDAO;
import com.docdoku.plm.server.dao.WorkspaceDAO;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(IWorkspaceManagerLocal.class)
@Stateless(name = "WorkspaceManagerBean")
public class WorkspaceManagerBean implements IWorkspaceManagerLocal {

    @Inject
    private EntityManager em;

    @Inject
    private AccountDAO accountDAO;

    @Inject
    private UserDAO userDAO;

    @Inject
    private WorkspaceDAO workspaceDAO;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private IContextManagerLocal contextManager;

    @Inject
    private INotifierLocal mailerManager;

    @Inject
    private IIndexerManagerLocal indexerManager;

    @Inject
    private IPlatformOptionsManagerLocal platformOptionsManager;

    @Inject
    private IBinaryStorageManagerLocal storageManager;

    private static final Logger LOGGER = Logger.getLogger(WorkspaceManagerBean.class.getName());

    @RolesAllowed(UserGroupMapping.ADMIN_ROLE_ID)
    @Override
    public long getDiskUsageInWorkspace(String workspaceId) throws AccountNotFoundException {
        accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
        return workspaceDAO.getDiskUsageForWorkspace(workspaceId);
    }

    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public void deleteWorkspace(String workspaceId)
            throws WorkspaceNotFoundException, AccountNotFoundException, AccessRightException {
        if (!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            userManager.checkAdmin(workspaceId);
        }
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        doDeleteWorkspace(workspace);
    }

    @Asynchronous
    private void doDeleteWorkspace(Workspace workspace) {
        String workspaceId = workspace.getId();
        Exception exceptionThrown = null;
        Account admin = workspace.getAdmin();
        try {
            workspaceDAO.removeWorkspace(workspace);
            storageManager.deleteWorkspaceFolder(workspaceId);
            indexerManager.deleteWorkspaceIndex(workspaceId);
            mailerManager.sendWorkspaceDeletionNotification(admin, workspaceId);
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Application Exception deleting workspace " + workspaceId, e);
            exceptionThrown = e;
        } catch (StorageException e) {
            LOGGER.log(Level.SEVERE, "Unhandled Exception deleting workspace " + workspaceId, e);
            exceptionThrown = e;
        }
        if (null != exceptionThrown) {
            mailerManager.sendWorkspaceDeletionErrorNotification(admin, workspaceId);
        }
    }

    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public Workspace changeAdmin(String workspaceId, String login) throws WorkspaceNotFoundException, AccountNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException, NotAllowedException {
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        Account account = accountDAO.loadAccount(login);

        if (contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID) || workspace.getAdmin().getLogin().equals(contextManager.getCallerPrincipalLogin())) {
            User[] users = userManager.getUsers(workspaceId);
            if (Arrays.stream(users).noneMatch(u -> u.getLogin().equals(login))) {

                throw new NotAllowedException("NotAllowedException70");
            }
            workspace.setAdmin(account);
        } else {
            User user = userManager.whoAmI(workspaceId);
            throw new AccessRightException(user);
        }

        return workspace;
    }

    @Override
    @RolesAllowed(UserGroupMapping.ADMIN_ROLE_ID)
    public Workspace enableWorkspace(String workspaceId, boolean enabled) throws WorkspaceNotFoundException {
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        workspace.setEnabled(enabled);
        return workspace;
    }


    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Workspace createWorkspace(String pID, Account pAdmin, String pDescription, boolean pFolderLocked) throws WorkspaceAlreadyExistsException, FolderAlreadyExistsException, UserAlreadyExistsException, CreationException, NotAllowedException {
        if (!NamingConvention.correct(pID)) {
            throw new NotAllowedException("NotAllowedException9", pID);
        }
        OperationSecurityStrategy workspaceCreationStrategy = platformOptionsManager.getWorkspaceCreationStrategy();
        Workspace workspace = new Workspace(pID, pAdmin, pDescription, pFolderLocked);
        workspace.setEnabled(workspaceCreationStrategy.equals(OperationSecurityStrategy.NONE));
        workspaceDAO.createWorkspace(workspace);
        User userToCreate = new User(workspace, pAdmin);
        userDAO.createUser(userToCreate);
        userDAO.addUserMembership(workspace, userToCreate);

        indexerManager.createWorkspaceIndex(pID);

        return workspace;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Workspace getWorkspace(String workspaceId)
            throws WorkspaceNotFoundException, AccountNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        if(!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)){
            userManager.checkWorkspaceReadAccess(workspaceId);
        }
        return workspaceDAO.loadWorkspace(workspaceId);
    }


    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public WorkspaceFrontOptions getWorkspaceFrontOptions(String workspaceId)
            throws AccountNotFoundException, WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        if(!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)){
            userManager.checkWorkspaceReadAccess(workspaceId);
        }
        return workspaceDAO.loadWorkspaceFrontOptions(workspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void updateWorkspaceFrontOptions(WorkspaceFrontOptions pWorkspaceFrontOptions) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        String workspaceId = pWorkspaceFrontOptions.getWorkspace().getId();
        userManager.checkAdmin(workspaceId);
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        pWorkspaceFrontOptions.setWorkspace(workspace);
        workspaceDAO.updateWorkspaceFrontOptions(pWorkspaceFrontOptions);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public Workspace updateWorkspace(String workspaceId, String description, boolean isFolderLocked) throws AccessRightException, AccountNotFoundException, WorkspaceNotFoundException {
        userManager.checkAdmin(workspaceId);
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        workspace.setDescription(description);
        workspace.setFolderLocked(isFolderLocked);
        return workspace;
    }

    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public void updateWorkspaceBackOptions(WorkspaceBackOptions pWorkspaceBackOptions) throws WorkspaceNotFoundException, AccountNotFoundException, AccessRightException {
        String workspaceId = pWorkspaceBackOptions.getWorkspace().getId();
        userManager.checkAdmin(workspaceId);
        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);
        pWorkspaceBackOptions.setWorkspace(workspace);
        workspaceDAO.updateWorkspaceBackOptions(pWorkspaceBackOptions);
    }

    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public WorkspaceBackOptions getWorkspaceBackOptions(String workspaceId) throws AccountNotFoundException, WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {

        if (!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
            userManager.checkWorkspaceReadAccess(workspaceId);
        }
        WorkspaceBackOptions workspaceBackOptions = workspaceDAO.loadWorkspaceBackOptions(workspaceId);
        if (workspaceBackOptions == null) {
            Workspace workspace = em.find(Workspace.class, workspaceId);
            workspaceBackOptions = new WorkspaceBackOptions(workspace);
        }
        return workspaceBackOptions;
    }

}
