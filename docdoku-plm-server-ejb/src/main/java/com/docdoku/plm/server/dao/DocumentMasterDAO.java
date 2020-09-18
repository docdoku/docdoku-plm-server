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

import com.docdoku.plm.server.core.document.DocumentMaster;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.DocumentMasterAlreadyExistsException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class DocumentMasterDAO {

    private static final Logger LOGGER = Logger.getLogger(DocumentMasterDAO.class.getName());

    @Inject
    private EntityManager em;

    @Inject
    private DocumentRevisionDAO documentRevisionDAO;

    public DocumentMasterDAO() {
    }

    public void createDocM(DocumentMaster pDocumentMaster) throws DocumentMasterAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pDocumentMaster);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            LOGGER.log(Level.FINER,null,pEEEx);
            throw new DocumentMasterAlreadyExistsException(pDocumentMaster);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            LOGGER.log(Level.FINER,null,pPEx);
            throw new CreationException();
        }
    }

    public void removeDocM(DocumentMaster pDocM) {
        List<DocumentRevision> docRs = new ArrayList<>(pDocM.getDocumentRevisions());
        for(DocumentRevision documentRevision:docRs){
            documentRevisionDAO.removeRevision(documentRevision);
        }
        em.remove(pDocM);
    }

    public List<DocumentMaster> getPaginatedByWorkspace(String workspaceId, int limit, int offset) {
        return em.createNamedQuery("DocumentMaster.findByWorkspace",DocumentMaster.class)
                .setParameter("workspaceId",workspaceId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public Long getCountByWorkspace(String workspaceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<DocumentMaster> dm = countQuery.from(DocumentMaster.class);
        countQuery.select(cb.count(dm)).where(cb.equal(dm.get("workspace").get("id"), workspaceId));
        return em.createQuery(countQuery).getSingleResult();
    }
}
