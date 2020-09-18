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
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.Organization;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.gcm.GCMAccount;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IAccountManagerLocal;
import com.docdoku.plm.server.core.services.IContextManagerLocal;
import com.docdoku.plm.server.core.services.INotifierLocal;
import com.docdoku.plm.server.core.services.IPlatformOptionsManagerLocal;
import com.docdoku.plm.server.i18n.PropertiesLoader;
import com.docdoku.plm.server.config.ServerConfig;
import com.docdoku.plm.server.dao.AccountDAO;
import com.docdoku.plm.server.dao.GCMAccountDAO;
import com.docdoku.plm.server.dao.OrganizationDAO;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(IAccountManagerLocal.class)
@Stateless(name = "AccountManagerBean")
public class AccountManagerBean implements IAccountManagerLocal {

    @Inject
    private EntityManager em;

    @Inject
    private AccountDAO accountDAO;

    @Inject
    private GCMAccountDAO gcmAccountDAO;

    @Inject
    private OrganizationDAO organizationDAO;

    @Inject
    private IContextManagerLocal contextManager;

    @Inject
    private INotifierLocal mailer;

    @Inject
    private IPlatformOptionsManagerLocal platformOptionsManager;

    @Inject
    private ServerConfig serverConfig;

    public AccountManagerBean() {
    }

    @Override
    public Account authenticateAccount(String login, String password) {
        Account account = null;

        if (accountDAO.authenticate(login, password, serverConfig.getDigestAlgorithm())) {

            try {
                account = getAccount(login);
            } catch (AccountNotFoundException e) {
                return null;
            }
        }

        return account;
    }

    @Override
    public UserGroupMapping getUserGroupMapping(String login) {
        return em.find(UserGroupMapping.class, login);
    }

    @Override
    public Account createAccount(String pLogin, String pName, String pEmail, String pLanguage, String pPassword, String pTimeZone) throws AccountAlreadyExistsException, CreationException {
        OperationSecurityStrategy registrationStrategy = platformOptionsManager.getRegistrationStrategy();
        Date now = new Date();
        Account account = new Account(pLogin, pName, pEmail, pLanguage, now, pTimeZone);
        account.setEnabled(registrationStrategy.equals(OperationSecurityStrategy.NONE));
        accountDAO.createAccount(account, pPassword, serverConfig.getDigestAlgorithm());
        mailer.sendCredential(account);
        return account;
    }

    @Override
    public Account getAccount(String pLogin) throws AccountNotFoundException {
        return accountDAO.loadAccount(pLogin);
    }

    public String getRole(String login) {
        UserGroupMapping userGroupMapping = em.find(UserGroupMapping.class, login);
        if (userGroupMapping == null) {
            return null;
        } else {
            return userGroupMapping.getGroupName();
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Account updateAccount(String pName, String pEmail, String pLanguage, String pPassword, String pTimeZone) throws AccountNotFoundException, NotAllowedException {
        
        if(!isLanguageSupported(pLanguage)){
            throw new NotAllowedException("NotAllowedException74");
        }
        if(!isTimeZoneAvailable(pTimeZone)) {
            throw new NotAllowedException("NotAllowedException75");
        }
        Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
        account.setName(pName);
        account.setEmail(pEmail);
        account.setLanguage(pLanguage);
        account.setTimeZone(pTimeZone);
        if (pPassword != null) {
            accountDAO.updateCredential(account.getLogin(), pPassword, serverConfig.getDigestAlgorithm());
        }
        return account;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Account checkAdmin(Organization pOrganization) throws AccessRightException, AccountNotFoundException {
        Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());

        if (!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID) && !pOrganization.getOwner().equals(account)) {
            throw new AccessRightException(account);
        }

        return account;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Account checkAdmin(String pOrganizationName)
            throws AccessRightException, AccountNotFoundException, OrganizationNotFoundException {

        Account account = accountDAO.loadAccount(contextManager.getCallerPrincipalLogin());
        Organization organization = organizationDAO.loadOrganization(pOrganizationName);

        if (!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID) && !organization.getOwner().equals(account)) {
            throw new AccessRightException(account);
        }

        return account;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void setGCMAccount(String gcmId) throws AccountNotFoundException, GCMAccountAlreadyExistsException, CreationException, GCMAccountNotFoundException {
        String callerLogin = contextManager.getCallerPrincipalLogin();
        Account account = getAccount(callerLogin);
        if (gcmAccountDAO.hasGCMAccount(account)) {
            GCMAccount gcmAccount = gcmAccountDAO.loadGCMAccount(account);
            gcmAccount.setGcmId(gcmId);
        } else {
            gcmAccountDAO.createGCMAccount(new GCMAccount(account, gcmId));
        }

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void deleteGCMAccount() throws AccountNotFoundException, GCMAccountNotFoundException {
        String callerLogin = contextManager.getCallerPrincipalLogin();
        Account account = getAccount(callerLogin);
        GCMAccount gcmAccount = gcmAccountDAO.loadGCMAccount(account);
        gcmAccountDAO.deleteGCMAccount(gcmAccount);
    }

    @Override
    public boolean isAccountEnabled(String pLogin) throws AccountNotFoundException {
        Account account = getAccount(pLogin);
        return account.isEnabled();
    }

    @Override
    @RolesAllowed(UserGroupMapping.ADMIN_ROLE_ID)
    public List<Account> getAccounts() {
        return accountDAO.getAccounts();
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public Account getMyAccount() throws AccountNotFoundException {
        return getAccount(contextManager.getCallerPrincipalName());
    }

    @Override
    @RolesAllowed(UserGroupMapping.ADMIN_ROLE_ID)
    public Account enableAccount(String login, boolean enabled) throws AccountNotFoundException, NotAllowedException {
        String callerPrincipalLogin = contextManager.getCallerPrincipalLogin();
        if (!callerPrincipalLogin.equals(login)) {
            Account account = getAccount(login);
            account.setEnabled(enabled);
            return account;
        } else {
            throw new NotAllowedException("NotAllowedException67");
        }
    }

    @Override
    @RolesAllowed(UserGroupMapping.ADMIN_ROLE_ID)
    public Account updateAccount(String pLogin, String pName, String pEmail, String pLanguage, String pPassword, String pTimeZone) throws AccountNotFoundException, NotAllowedException {
        if(!isLanguageSupported(pLanguage)){
            throw new NotAllowedException("NotAllowedException74");
        }
        if(!isTimeZoneAvailable(pTimeZone)) {
            throw new NotAllowedException("NotAllowedException75");
        }
        Account otherAccount = getAccount(pLogin);
        otherAccount.setName(pName);
        otherAccount.setEmail(pEmail);
        otherAccount.setLanguage(pLanguage);
        otherAccount.setTimeZone(pTimeZone);
        if (pPassword != null) {
            accountDAO.updateCredential(otherAccount.getLogin(), pPassword, serverConfig.getDigestAlgorithm());
        }
        return otherAccount;
    }
    private Boolean  isTimeZoneAvailable(String value) {
        return Arrays.asList(TimeZone.getAvailableIDs()).contains(value);
    }
    private Boolean isLanguageSupported(String value) {
        return PropertiesLoader.getSupportedLanguages().contains(value);
    }
}
