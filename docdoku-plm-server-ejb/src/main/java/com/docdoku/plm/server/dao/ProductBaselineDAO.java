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

import com.docdoku.plm.server.core.configuration.*;
import com.docdoku.plm.server.core.exceptions.BaselineNotFoundException;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.ProductInstanceMasterNotFoundException;
import com.docdoku.plm.server.core.product.ConfigurationItemKey;
import com.docdoku.plm.server.core.product.PartRevision;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class ProductBaselineDAO {

    public static final String WORKSPACE_ID = "workspaceId";

    @Inject
    private EntityManager em;

    @Inject
    private ProductInstanceMasterDAO productInstanceMasterDAO;

    private static final Logger LOGGER = Logger.getLogger(ProductBaselineDAO.class.getName());

    public ProductBaselineDAO() {
    }

    public List<ProductBaseline> findBaselines(String workspaceId) {
        return em.createNamedQuery("ProductBaseline.findByWorkspace", ProductBaseline.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .getResultList();
    }

    public List<ProductBaseline> findBaselines(String ciId, String workspaceId) {
        return em.createNamedQuery("ProductBaseline.findByConfigurationItemId", ProductBaseline.class)
                .setParameter("ciId", ciId)
                .setParameter(WORKSPACE_ID, workspaceId)
                .getResultList();
    }

    public void createBaseline(ProductBaseline pProductBaseline) throws CreationException {
        try {
            em.persist(pProductBaseline);
            em.flush();
        } catch (PersistenceException pPEx) {
            LOGGER.log(Level.FINEST, null, pPEx);
            throw new CreationException();
        }
    }

    public ProductBaseline loadBaseline(int pId) throws BaselineNotFoundException {
        ProductBaseline productBaseline = em.find(ProductBaseline.class, pId);
        if (productBaseline == null) {
            throw new BaselineNotFoundException(pId);
        } else {
            return productBaseline;
        }
    }

    public void deleteBaseline(ProductBaseline productBaseline) {
        flushBaselinedParts(productBaseline);
        em.remove(productBaseline);
        em.flush();
    }

    public boolean existBaselinedPart(String workspaceId, String partNumber) {
        return em.createNamedQuery("BaselinedPart.existBaselinedPart", Long.class)
                .setParameter("partNumber", partNumber)
                .setParameter(WORKSPACE_ID, workspaceId)
                .getSingleResult() > 0;
    }

    public void flushBaselinedParts(ProductBaseline productBaseline) {
        productBaseline.removeAllBaselinedParts();
        em.flush();
    }

    public List<ProductBaseline> findBaselineWherePartRevisionHasIterations(PartRevision partRevision) {
        return em.createNamedQuery("ProductBaseline.getBaselinesForPartRevision", ProductBaseline.class)
                .setParameter("partRevision", partRevision)
                .getResultList();
    }

    public List<PartRevision> findObsoletePartsInBaseline(String workspaceId, ProductBaseline productBaseline) {
        return em.createNamedQuery("ProductBaseline.findObsoletePartRevisions", PartRevision.class)
                .setParameter("productBaseline", productBaseline)
                .setParameter(WORKSPACE_ID, workspaceId)
                .getResultList();
    }

    public ProductBaseline findBaselineById(int baselineId) {
        return em.find(ProductBaseline.class, baselineId);
    }

    public List<BaselinedPart> findBaselinedPartWithReferenceLike(int collectionId, String q, int maxResults) {
        return em.createNamedQuery("BaselinedPart.findByReference", BaselinedPart.class)
                .setParameter("id", "%" + q + "%")
                .setParameter("partCollection", collectionId)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public ProductBaseline findLastBaselineWithSerialNumber(ConfigurationItemKey ciKey, String serialNumber) throws ProductInstanceMasterNotFoundException {
        ProductInstanceMasterKey productInstanceMasterKey = new ProductInstanceMasterKey(serialNumber, ciKey);
        ProductInstanceMaster productIM = productInstanceMasterDAO.loadProductInstanceMaster(productInstanceMasterKey);
        ProductInstanceIteration productII = productIM.getLastIteration();

        ProductBaseline basedOn = null;

        if (productII != null ) {
            basedOn = productII.getBasedOn();
        }

        return basedOn;
    }

}
