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
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.GCMAccountAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.GCMAccountNotFoundException;
import com.docdoku.plm.server.core.gcm.GCMAccount;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;


@RequestScoped
public class GCMAccountDAO {

    @Inject
    private EntityManager em;

    public GCMAccountDAO() {
    }

    public GCMAccount loadGCMAccount(Account pAccount) throws GCMAccountNotFoundException {
        GCMAccount gcmAccount = em.find(GCMAccount.class, pAccount.getLogin());
        if(gcmAccount == null){
            throw new GCMAccountNotFoundException(pAccount.getLogin());
        }
        return gcmAccount;
    }
    public boolean hasGCMAccount(Account pAccount) {
        GCMAccount gcmAccount = em.find(GCMAccount.class, pAccount.getLogin());
        return gcmAccount != null;
    }

    public void createGCMAccount(GCMAccount pGMCAccount) throws GCMAccountAlreadyExistsException, CreationException {
        try{
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pGMCAccount);
            em.flush();
        }catch(EntityExistsException pEEEx){
            throw new GCMAccountAlreadyExistsException(pGMCAccount.getAccount().getLogin());
        }catch(PersistenceException pPEx){
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException();
        }
    }

    public void deleteGCMAccount(GCMAccount gcmAccount){
        em.remove(gcmAccount);
        em.flush();
    }

}
