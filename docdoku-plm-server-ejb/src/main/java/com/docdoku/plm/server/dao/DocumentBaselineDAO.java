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

import com.docdoku.plm.server.core.configuration.DocumentBaseline;
import com.docdoku.plm.server.core.exceptions.BaselineNotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;


/**
 * Data access object for DocumentBaseline
 *
 * @author Taylor LABEJOF
 * @version 2.0, 28/08/14
 * @since   V2.0
 */
@RequestScoped
public class DocumentBaselineDAO {

    @Inject
    private EntityManager em;

    public DocumentBaselineDAO() {}

    public void createBaseline(DocumentBaseline documentBaseline) {
        em.persist(documentBaseline);
        em.flush();
    }

    public List<DocumentBaseline> findBaselines(String workspaceId) {
        return em.createQuery("SELECT b FROM DocumentBaseline b WHERE b.author.workspace.id = :workspaceId", DocumentBaseline.class)
                .setParameter("workspaceId",workspaceId)
                .getResultList();
    }

    public DocumentBaseline loadBaseline(int pBaselineId) throws BaselineNotFoundException {
        DocumentBaseline documentBaseline = em.find(DocumentBaseline.class, pBaselineId);
        if(documentBaseline == null){
            throw new BaselineNotFoundException(pBaselineId);
        }else{
            return documentBaseline;
        }
    }

    public void deleteBaseline(DocumentBaseline documentBaseline) {
        em.remove(documentBaseline);
        em.flush();
    }

    public boolean existBaselinedDocument(String workspaceId, String documentId, String documentVersion) {
        return em.createNamedQuery("BaselinedDocument.existBaselinedDocument", Long.class)
                .setParameter("documentId", documentId)
                .setParameter("documentVersion", documentVersion)
                .setParameter("workspaceId", workspaceId)
                .getSingleResult() > 0;
    }
}
