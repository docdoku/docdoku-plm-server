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
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.product.Import;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.List;


@RequestScoped
public class ImportDAO {

    @Inject
    private EntityManager em;

    public ImportDAO() {
    }

    public void createImport(Import pImportToPersist) throws CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pImportToPersist);
            em.flush();
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException();
        }
    }

    public Import findImport(User user, String id) {
        TypedQuery<Import> query = em.createQuery("SELECT DISTINCT i FROM Import i WHERE i.id = :id AND i.user = :user", Import.class);
        query.setParameter("user", user);
        query.setParameter("id", id);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Import> findImports(User user, String filename) {
        TypedQuery<Import> query = em.createQuery("SELECT DISTINCT i FROM Import i WHERE i.user = :user AND i.fileName = :filename", Import.class);
        query.setParameter("user", user);
        query.setParameter("filename", filename);
        return query.getResultList();
    }

    public void deleteImport(Import importToDelete) {
        em.remove(importToDelete);
        em.flush();
    }

}
