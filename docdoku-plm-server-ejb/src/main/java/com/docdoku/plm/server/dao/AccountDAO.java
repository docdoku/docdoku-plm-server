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

import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.AccountAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.AccountNotFoundException;
import com.docdoku.plm.server.core.security.Credential;
import com.docdoku.plm.server.core.security.UserGroupMapping;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.List;


@RequestScoped
public class AccountDAO {

    @Inject
    private EntityManager em;
    //private EntityManager em;

    public AccountDAO() {
    }

    public void createAccount(Account pAccount, String pPassword, String pAlgorithm) throws AccountAlreadyExistsException {
        try {
            //the EntityExistsException is thrown only when flush occurs 
            em.persist(pAccount);
            em.flush();
            Credential credential = Credential.createCredential(pAccount.getLogin(), pPassword, pAlgorithm);
            em.persist(credential);
            em.persist(new UserGroupMapping(pAccount.getLogin()));
        } catch (PersistenceException pEEEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new AccountAlreadyExistsException(pAccount.getLogin());
        }
    }

    public void updateCredential(String pLogin, String pPassword, String pAlgorithm) {
        Credential credential = Credential.createCredential(pLogin, pPassword, pAlgorithm);
        em.merge(credential);
    }

    public Account loadAccount(String pLogin) throws AccountNotFoundException {
        Account account = em.find(Account.class, pLogin);
        if (account == null) {
            throw new AccountNotFoundException(pLogin);
        } else {
            return account;
        }
    }

    public Workspace[] getAdministratedWorkspaces(Account pAdmin) {
        Workspace[] workspaces;
        TypedQuery<Workspace> query = em.createQuery("SELECT DISTINCT w FROM Workspace w WHERE w.admin = :admin", Workspace.class);
        List<Workspace> listWorkspaces = query.setParameter("admin", pAdmin).getResultList();
        workspaces = new Workspace[listWorkspaces.size()];
        for (int i = 0; i < listWorkspaces.size(); i++) {
            workspaces[i] = listWorkspaces.get(i);
        }

        return workspaces;
    }

    public Workspace[] getAllWorkspaces() {
        Workspace[] workspaces;
        TypedQuery<Workspace> query = em.createQuery("SELECT DISTINCT w FROM Workspace w", Workspace.class);
        List<Workspace> listWorkspaces = query.getResultList();
        workspaces = new Workspace[listWorkspaces.size()];
        for (int i = 0; i < listWorkspaces.size(); i++) {
            workspaces[i] = listWorkspaces.get(i);
        }

        return workspaces;
    }

    public List<Account> getAccounts() {
        TypedQuery<Account> query = em.createQuery("SELECT DISTINCT a FROM Account a", Account.class);
        return query.getResultList();
    }


    public boolean authenticate(String login, String password, String pAlgorithm) {
        Credential credential = em.find(Credential.class, login);
        return credential != null && credential.authenticate(password, pAlgorithm);
    }
}
