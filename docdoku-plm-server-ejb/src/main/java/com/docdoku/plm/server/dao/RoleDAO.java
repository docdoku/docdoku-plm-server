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
import com.docdoku.plm.server.core.common.UserGroup;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.RoleAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.RoleNotFoundException;
import com.docdoku.plm.server.core.workflow.Role;
import com.docdoku.plm.server.core.workflow.RoleKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Morgan Guimard
 */
@RequestScoped
public class RoleDAO {

    @Inject
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(RoleDAO.class.getName());

    public RoleDAO() {
    }

    public Role loadRole(RoleKey pRoleKey) throws RoleNotFoundException {

        Role role = em.find(Role.class, pRoleKey);
        if (role == null) {
            throw new RoleNotFoundException(pRoleKey);
        } else {
            return role;
        }

    }

    public List<Role> findRolesInWorkspace(String pWorkspaceId){
        return em.createNamedQuery("Role.findByWorkspace",Role.class).setParameter("workspaceId", pWorkspaceId).getResultList();
    }

    public void createRole(Role pRole) throws CreationException, RoleAlreadyExistsException {
        try{
            em.persist(pRole);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            LOGGER.log(Level.FINEST,null,pEEEx);
            throw new RoleAlreadyExistsException(pRole);
        } catch (PersistenceException pPEx) {
            LOGGER.log(Level.FINEST,null,pPEx);
            throw new CreationException();
        }
    }

    public void deleteRole(Role pRole){
        em.remove(pRole);
        em.flush();
    }

    public boolean isRoleInUseInWorkflowModel(Role role) {
        return !em.createNamedQuery("Role.findRolesInUseByRoleName")
                 .setParameter("roleName", role.getName())
                 .setParameter("workspace", role.getWorkspace())
                 .getResultList().isEmpty();

    }

    public List<Role> findRolesInUseWorkspace(String pWorkspaceId) {
        return em.createNamedQuery("Role.findRolesInUse",Role.class).setParameter("workspaceId", pWorkspaceId).getResultList();
    }

    public void removeUserFromRoles(User pUser) {
        List<Role> roles = em.createNamedQuery("Role.findRolesWhereUserIsAssigned", Role.class)
                .setParameter("user", pUser)
                .getResultList();
        for(Role role:roles){
            role.removeUser(pUser);
        }
        em.flush();
    }

    public void removeGroupFromRoles(UserGroup pGroup) {
        List<Role> roles = em.createNamedQuery("Role.findRolesWhereGroupIsAssigned", Role.class)
                .setParameter("userGroup", pGroup)
                .getResultList();
        for(Role role:roles){
            role.removeUserGroup(pGroup);
        }
        em.flush();
    }
}
