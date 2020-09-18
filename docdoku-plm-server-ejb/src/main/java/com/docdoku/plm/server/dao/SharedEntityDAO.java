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


import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.exceptions.SharedEntityNotFoundException;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.sharing.SharedDocument;
import com.docdoku.plm.server.core.sharing.SharedEntity;
import com.docdoku.plm.server.core.sharing.SharedPart;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;


/**
 * @author Morgan Guimard
 */

@RequestScoped
public class SharedEntityDAO {

    @Inject
    private EntityManager em;

    public SharedEntityDAO() {
    }

    public boolean isSharedDocument(String pUuid){
        return em.find(SharedDocument.class, pUuid) != null;
    }

    public boolean isSharedPart(String pUuid){
        return em.find(SharedPart.class, pUuid) != null;
    }

    public SharedDocument loadSharedDocument(String pUuid) throws SharedEntityNotFoundException {

        SharedDocument sharedDocument = em.find(SharedDocument.class, pUuid);
        if (sharedDocument == null) {
            throw new SharedEntityNotFoundException(pUuid);
        } else {
            return sharedDocument;
        }

    }

    public SharedPart loadSharedPart(String pUuid) throws SharedEntityNotFoundException {

        SharedPart sharedPart = em.find(SharedPart.class, pUuid);
        if (sharedPart == null) {
            throw new SharedEntityNotFoundException(pUuid);
        } else {
            return sharedPart;
        }
    }

    public void createSharedDocument(SharedDocument pSharedDocument) {
        em.persist(pSharedDocument);
        em.flush();
    }

    public void createSharedPart(SharedPart pSharedPart) {
        em.persist(pSharedPart);
        em.flush();
    }

    public void deleteSharedDocument(SharedDocument pSharedDocument){
        em.remove(pSharedDocument);
        em.flush();
    }

    public void deleteSharedPart(SharedPart pSharedPart){
        em.remove(pSharedPart);
        em.flush();
    }

    public void deleteSharesForDocument(DocumentRevision pDocR) {
        TypedQuery<SharedDocument> query = em.createNamedQuery("SharedDocument.deleteSharesForGivenDocument", SharedDocument.class);
        query.setParameter("pDocR", pDocR).executeUpdate();
    }

    public void deleteSharesForPart(PartRevision pPartR) {
        TypedQuery<SharedPart> query = em.createNamedQuery("SharedPart.deleteSharesForGivenPart", SharedPart.class);
        query.setParameter("pPartR", pPartR).executeUpdate();
    }

    public SharedEntity loadSharedEntity(String pUuid) throws SharedEntityNotFoundException {
        TypedQuery<SharedEntity> query = em.createNamedQuery("SharedEntity.findSharedEntityForGivenUuid", SharedEntity.class);
        try {
            return query.setParameter("pUuid", pUuid).getSingleResult();
        }catch(NoResultException ex){
            throw new SharedEntityNotFoundException(pUuid);
        }

    }

    public void deleteSharedEntity(SharedEntity pSharedEntity) {
        try {
            SharedEntity sharedEntity = loadSharedEntity(pSharedEntity.getUuid());
            if(pSharedEntity instanceof SharedDocument){
                deleteSharedDocument((SharedDocument) sharedEntity);
            }else if(pSharedEntity instanceof SharedPart){
                deleteSharedPart((SharedPart) sharedEntity);
            }
        } catch (SharedEntityNotFoundException e) {

        }

    }
}
