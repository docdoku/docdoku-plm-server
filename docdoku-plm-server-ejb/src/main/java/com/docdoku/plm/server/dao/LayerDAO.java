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

import com.docdoku.plm.server.core.exceptions.LayerNotFoundException;
import com.docdoku.plm.server.core.product.ConfigurationItemKey;
import com.docdoku.plm.server.core.product.Layer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;


@RequestScoped
public class LayerDAO {

    @Inject
    private EntityManager em;
    
    public LayerDAO() {
    }

    public List<Layer> findAllLayers(ConfigurationItemKey pKey) {
        TypedQuery<Layer> query = em.createNamedQuery("Layer.findLayersByConfigurationItem", Layer.class);
        query.setParameter("workspaceId", pKey.getWorkspace());
        query.setParameter("configurationItemId", pKey.getId());
        return query.getResultList();
    }
    
    public Layer loadLayer(int pId) throws LayerNotFoundException {
        Layer layer = em.find(Layer.class, pId);
        if (layer == null) {
            throw new LayerNotFoundException(pId);
        } else {
            return layer;
        }
    }

    public void createLayer(Layer pLayer) {
        em.persist(pLayer);
        em.flush();
    }

    public void deleteLayer(Layer layer) {
        em.remove(layer);
        em.flush();
    }
}
