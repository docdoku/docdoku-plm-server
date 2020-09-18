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
import com.docdoku.plm.server.core.configuration.ProductInstanceIteration;
import com.docdoku.plm.server.core.configuration.ProductInstanceMaster;
import com.docdoku.plm.server.core.configuration.ProductInstanceMasterKey;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.ProductInstanceAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.ProductInstanceMasterNotFoundException;
import com.docdoku.plm.server.core.product.PartRevision;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;


@RequestScoped
public class ProductInstanceMasterDAO {

    @Inject
    private EntityManager em;

    public ProductInstanceMasterDAO() {
    }

    public List<ProductInstanceMaster> findProductInstanceMasters(String workspaceId) {
        return em.createQuery("SELECT pim FROM ProductInstanceMaster pim WHERE pim.instanceOf.workspace.id = :workspaceId", ProductInstanceMaster.class)
                .setParameter("workspaceId",workspaceId)
                .getResultList();
    }

    public List<ProductInstanceMaster> findProductInstanceMasters(String ciId, String workspaceId) {
        return em.createNamedQuery("ProductInstanceMaster.findByConfigurationItemId", ProductInstanceMaster.class)
                .setParameter("ciId", ciId)
                .setParameter("workspaceId",workspaceId)
                .getResultList();
    }

    public List<ProductInstanceMaster> findProductInstanceMasters(PartRevision partRevision) {
        return em.createNamedQuery("ProductInstanceMaster.findByPart", ProductInstanceMaster.class)
                .setParameter("partRevision", partRevision)
                .getResultList();
    }

    public void createProductInstanceMaster(ProductInstanceMaster pProductInstanceMaster) throws ProductInstanceAlreadyExistsException, CreationException {
        try{
            em.persist(pProductInstanceMaster);
            em.flush();
        }catch (EntityExistsException e){
            throw new ProductInstanceAlreadyExistsException(pProductInstanceMaster);
        }catch (PersistenceException e){
            throw new CreationException();
        }

    }

    public ProductInstanceMaster loadProductInstanceMaster(ProductInstanceMasterKey pId) throws ProductInstanceMasterNotFoundException {
        ProductInstanceMaster productInstanceMaster = em.find(ProductInstanceMaster.class, pId);
        if (productInstanceMaster == null) {
            throw new ProductInstanceMasterNotFoundException(pId);
        } else {
            return productInstanceMaster;
        }
    }

    public boolean existsProductInstanceMaster(ProductInstanceMasterKey pId) {
        return em.find(ProductInstanceMaster.class, pId) != null;
    }

    public void deleteProductInstanceMaster(ProductInstanceMaster productInstanceMaster) {
        for(ProductInstanceIteration productInstanceIteration : productInstanceMaster.getProductInstanceIterations()){
            for(BaselinedPart baselinedPart : productInstanceIteration.getBaselinedParts().values()){
                em.remove(baselinedPart);
            }
            
            em.refresh(productInstanceIteration.getPartCollection());
            em.remove(productInstanceIteration.getPartCollection());
            em.remove(productInstanceIteration);
        }

        em.remove(productInstanceMaster);
        em.flush();
    }
}
