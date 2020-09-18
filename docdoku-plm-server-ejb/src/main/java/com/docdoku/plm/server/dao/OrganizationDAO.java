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
import com.docdoku.plm.server.core.common.Organization;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.OrganizationAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.OrganizationNotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;


@RequestScoped
public class OrganizationDAO {

    @Inject
    private EntityManager em;

    public OrganizationDAO() {
    }

    public Organization findOrganizationOfAccount(Account pAccount) throws OrganizationNotFoundException {
        try {
            return em.createNamedQuery("Organization.ofAccount", Organization.class)
                    .setParameter("account", pAccount).getSingleResult();
        } catch (NoResultException ex) {
            throw new OrganizationNotFoundException(pAccount.getLogin());
        }
    }

    public boolean hasOrganization(Account pAccount) {
        return !em.createNamedQuery("Organization.ofAccount", Organization.class)
                .setParameter("account", pAccount).getResultList().isEmpty();
    }

    public void updateOrganization(Organization pOrganization) {
        em.merge(pOrganization);
    }

    public void createOrganization(Organization pOrganization) throws OrganizationAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            if (pOrganization.getName().trim().equals(""))
                throw new CreationException();
            em.persist(pOrganization);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            throw new OrganizationAlreadyExistsException(pOrganization);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException();
        }
    }

    public void deleteOrganization(Organization pOrganization) {
        em.remove(pOrganization);
        em.flush();
    }

    public Organization loadOrganization(String pName) throws OrganizationNotFoundException {
        Organization organization = em.find(Organization.class, pName);
        if (organization == null) {
            throw new OrganizationNotFoundException(pName);
        } else {
            return organization;
        }
    }

}
