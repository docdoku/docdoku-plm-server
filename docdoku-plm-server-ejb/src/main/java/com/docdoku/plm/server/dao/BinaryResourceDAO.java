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

import com.docdoku.plm.server.core.common.BinaryResource;
import com.docdoku.plm.server.core.configuration.PathDataIteration;
import com.docdoku.plm.server.core.configuration.ProductInstanceIteration;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentMasterTemplate;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.FileAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.FileNotFoundException;
import com.docdoku.plm.server.core.product.Geometry;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartMasterTemplate;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class BinaryResourceDAO {

    private static final Logger LOGGER = Logger.getLogger(BinaryResourceDAO.class.getName());

    @Inject
    private EntityManager em;

    public BinaryResourceDAO() {
    }

    public void createBinaryResource(BinaryResource pBinaryResource) throws FileAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pBinaryResource);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            LOGGER.log(Level.FINER, null, pEEEx);
            throw new FileAlreadyExistsException(pBinaryResource);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            LOGGER.log(Level.FINER, null, pPEx);
            throw new CreationException();
        }
    }

    public void removeBinaryResource(BinaryResource pBinaryResource) {
        em.remove(pBinaryResource);
        em.flush();
    }

    public BinaryResource loadBinaryResource(String pFullName) throws FileNotFoundException {
        BinaryResource file = em.find(BinaryResource.class, pFullName);
        if (null == file) {
            throw new FileNotFoundException(pFullName);
        }
        return file;
    }

    public boolean exists(String pFullName) {
        return em.find(BinaryResource.class, pFullName) != null;
    }

    public PartIteration getPartHolder(BinaryResource pBinaryResource) {
        TypedQuery<PartIteration> query;
        String fileType = pBinaryResource.getFileType();
        if (pBinaryResource instanceof Geometry) {
            query = em.createQuery("SELECT p FROM PartIteration p WHERE :binaryResource MEMBER OF p.geometries", PartIteration.class);
        } else if (PartIteration.NATIVE_CAD_SUBTYPE.equals(fileType)) {
            query = em.createQuery("SELECT p FROM PartIteration p WHERE p.nativeCADFile = :binaryResource", PartIteration.class);
        } else {
            query = em.createQuery("SELECT p FROM PartIteration p WHERE :binaryResource MEMBER OF p.attachedFiles", PartIteration.class);
        }
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }

    public DocumentIteration getDocumentHolder(BinaryResource pBinaryResource) {
        TypedQuery<DocumentIteration> query = em.createQuery("SELECT d FROM DocumentIteration d WHERE :binaryResource MEMBER OF d.attachedFiles", DocumentIteration.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }

    public ProductInstanceIteration getProductInstanceIterationHolder(BinaryResource pBinaryResource) {
        TypedQuery<ProductInstanceIteration> query = em.createQuery("SELECT d FROM ProductInstanceIteration d WHERE :binaryResource MEMBER OF d.attachedFiles", ProductInstanceIteration.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }

    public PathDataIteration getPathDataHolder(BinaryResource pBinaryResource) {
        TypedQuery<PathDataIteration> query = em.createQuery("SELECT p FROM PathDataIteration p WHERE :binaryResource MEMBER OF p.attachedFiles", PathDataIteration.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }


    public DocumentMasterTemplate getDocumentTemplateHolder(BinaryResource pBinaryResource) {
        TypedQuery<DocumentMasterTemplate> query = em.createQuery("SELECT t FROM DocumentMasterTemplate t WHERE :binaryResource MEMBER OF t.attachedFiles", DocumentMasterTemplate.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }

    public PartMasterTemplate getPartTemplateHolder(BinaryResource pBinaryResource) {
        TypedQuery<PartMasterTemplate> query = em.createQuery("SELECT t FROM PartMasterTemplate t WHERE t.attachedFile = :binaryResource", PartMasterTemplate.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }

    public BinaryResource findNativeCadBinaryResourceInWorkspace(String workspaceId, String cadFileName) {
        TypedQuery<BinaryResource> query = em.createQuery("SELECT br FROM BinaryResource br WHERE br.fullName like :name", BinaryResource.class);
        try {
            return query.setParameter("name", workspaceId + "/parts/%/nativecad/" + cadFileName).getSingleResult();
        } catch (NoResultException pNREx) {
            LOGGER.log(Level.FINER, null, pNREx);
            return null;
        }
    }

}
