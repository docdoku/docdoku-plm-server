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

import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.Organization;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IAccountManagerLocal;
import com.docdoku.plm.server.core.services.IContextManagerLocal;
import com.docdoku.plm.server.core.services.IOrganizationManagerLocal;
import com.docdoku.plm.server.dao.AccountDAO;
import com.docdoku.plm.server.dao.OrganizationDAO;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(IOrganizationManagerLocal.class)
@Stateless(name = "OrganizationManagerBean")
public class OrganizationManagerBean implements IOrganizationManagerLocal {

    @Inject
    private AccountDAO accountDAO;

    @Inject
    private OrganizationDAO organizationDAO;

    @Inject
    private IContextManagerLocal contextManagerLocal;

    @Inject
    private IAccountManagerLocal accountManager;

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void updateOrganization(Organization pOrganization)
            throws AccountNotFoundException, OrganizationNotFoundException, AccessRightException {

        Organization oldOrganization = organizationDAO.loadOrganization(pOrganization.getName());

        if (accountManager.checkAdmin(oldOrganization) != null) {
            organizationDAO.updateOrganization(pOrganization);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Organization getOrganizationOfAccount(String pLogin) throws AccountNotFoundException, OrganizationNotFoundException {
        return organizationDAO.findOrganizationOfAccount(accountManager.getMyAccount());
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Organization getMyOrganization() throws AccountNotFoundException, OrganizationNotFoundException {
        return getOrganizationOfAccount(contextManagerLocal.getCallerPrincipalLogin());
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Organization createOrganization(String pName, String pDescription) throws AccountNotFoundException, OrganizationAlreadyExistsException, CreationException, NotAllowedException {
        Account me = accountManager.getMyAccount();

        if (organizationDAO.hasOrganization(me)) {
            throw new NotAllowedException("NotAllowedException11");
        } else {
            Organization organization = new Organization(pName, me, pDescription);
            organizationDAO.createOrganization(organization);
            organization.addMember(me);
            return organization;
        }
    }

    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public void deleteOrganization(String pName) throws OrganizationNotFoundException, AccessRightException, AccountNotFoundException {
        Organization organization = organizationDAO.loadOrganization(pName);

        if (accountManager.checkAdmin(organization) != null) {
            organizationDAO.deleteOrganization(organization);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void addAccountInOrganization(String pOrganizationName, String pLogin) throws OrganizationNotFoundException, AccountNotFoundException, NotAllowedException, AccessRightException {
        Organization organization = organizationDAO.loadOrganization(pOrganizationName);
        accountManager.checkAdmin(organization);

        Account accountToAdd = accountDAO.loadAccount(pLogin);
        Organization accountToAddOrg = organizationDAO.findOrganizationOfAccount(accountToAdd);
        if (accountToAddOrg != null) {
            throw new NotAllowedException("NotAllowedException12");
        } else {
            organization.addMember(accountToAdd);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void removeAccountsFromOrganization(String pOrganizationName, String[] pLogins) throws OrganizationNotFoundException, AccessRightException, AccountNotFoundException {
        Organization organization = organizationDAO.loadOrganization(pOrganizationName);

        accountManager.checkAdmin(organization);

        for (String login : pLogins) {
            Account accountToRemove = accountDAO.loadAccount(login);
            organization.removeMember(accountToRemove);
        }

    }

}
