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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class PathDataIterationDAO {

    @Inject
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(PathDataIterationDAO.class.getName());

    public void createPathDataIteration(PathDataIteration pathDataIteration){
        try {
            em.persist(pathDataIteration);
            em.flush();
        }catch (Exception e){
            LOGGER.log(Level.SEVERE,"Fail to create path data",e);
        }
    }

    public List<PathDataIteration> getLastPathDataIterations(ProductInstanceIteration productInstanceIteration){
        return em.createNamedQuery("PathDataIteration.findLastIterationFromProductInstanceIteration",PathDataIteration.class)
                .setParameter("productInstanceIteration", productInstanceIteration)
                .getResultList();
    }
}
