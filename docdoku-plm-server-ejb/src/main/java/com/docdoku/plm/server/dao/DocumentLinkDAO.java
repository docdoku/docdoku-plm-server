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

import com.docdoku.plm.server.core.configuration.PathDataIteration;
import com.docdoku.plm.server.core.configuration.ProductInstanceIteration;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentLink;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.product.PartIteration;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import java.util.List;

@RequestScoped
public class DocumentLinkDAO {

    public static final String DOCUMENT_REVISION = "documentRevision";

    @Inject
    private EntityManager em;

    public void createLink(DocumentLink pLink){
        try{
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pLink);
            em.flush();
        }catch(EntityExistsException pEEEx){
            //already created
        }
    }

    public List<DocumentIteration> getInverseDocumentsLinks(DocumentRevision documentRevision){
        return em.createNamedQuery("DocumentLink.findInverseDocumentLinks",DocumentIteration.class)
                .setParameter(DOCUMENT_REVISION,documentRevision)
                .getResultList();
    }

    public List<PartIteration> getInversePartsLinks(DocumentRevision documentRevision){
        return em.createNamedQuery("DocumentLink.findInversePartLinks",PartIteration.class)
                .setParameter(DOCUMENT_REVISION,documentRevision)
                .getResultList();
    }
    public List<ProductInstanceIteration> getInverseProductInstanceIteration(DocumentRevision documentRevision){
        return em.createNamedQuery("DocumentLink.findProductInstanceIteration", ProductInstanceIteration.class)
                .setParameter(DOCUMENT_REVISION,documentRevision)
                .getResultList();
    }
    public List<PathDataIteration> getInversefindPathData(DocumentRevision documentRevision){
        return em.createNamedQuery("DocumentLink.findPathData", PathDataIteration.class)
                .setParameter(DOCUMENT_REVISION,documentRevision)
                .getResultList();
    }


}
