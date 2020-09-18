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

import com.docdoku.plm.server.core.configuration.BaselinedPart;
import com.docdoku.plm.server.core.configuration.ProductBaseline;
import com.docdoku.plm.server.core.configuration.ProductInstanceIteration;
import com.docdoku.plm.server.core.configuration.ProductInstanceIterationKey;
import com.docdoku.plm.server.core.exceptions.ProductInstanceIterationNotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class ProductInstanceIterationDAO {

    @Inject
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(ProductInstanceIterationDAO.class.getName());

    public ProductInstanceIterationDAO() {
    }

    public void createProductInstanceIteration(ProductInstanceIteration productInstanceIteration){
        try {
            em.persist(productInstanceIteration);
            em.flush();
        }catch (Exception e){
            LOGGER.log(Level.SEVERE,"Fail to create product instance iteration",e);
        }
    }


    public ProductInstanceIteration loadProductInstanceIteration(ProductInstanceIterationKey pId) throws ProductInstanceIterationNotFoundException {
        ProductInstanceIteration productInstanceIteration = em.find(ProductInstanceIteration.class, pId);
        if (productInstanceIteration == null) {
            throw new ProductInstanceIterationNotFoundException(pId);
        } else {
            return productInstanceIteration;
        }
    }

    public List<BaselinedPart> findBaselinedPartWithReferenceLike(int collectionId, String q, int maxResults) {
        return em.createNamedQuery("BaselinedPart.findByReference",BaselinedPart.class)
                .setParameter("id", "%" + q + "%")
                .setParameter("partCollection",collectionId)
                .setMaxResults(maxResults)
                .getResultList();

    }

    public boolean isBaselinedUsed(ProductBaseline productBaseline) {
        return !em.createNamedQuery("ProductInstanceIteration.findByProductBaseline",ProductInstanceIteration.class)
                .setParameter("productBaseline",productBaseline)
                .getResultList().isEmpty();
    }
}
