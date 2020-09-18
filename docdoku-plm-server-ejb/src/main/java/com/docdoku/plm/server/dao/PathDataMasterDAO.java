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

import com.docdoku.plm.server.core.configuration.PathDataMaster;
import com.docdoku.plm.server.core.configuration.ProductInstanceIteration;
import com.docdoku.plm.server.core.configuration.ProductInstanceMaster;
import com.docdoku.plm.server.core.exceptions.PathDataMasterNotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class PathDataMasterDAO {

    @Inject
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(PathDataMasterDAO.class.getName());

    public PathDataMasterDAO() {
    }

    public void createPathData(PathDataMaster pathDataMaster) {
        try {
            em.persist(pathDataMaster);
            em.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fail to create path data", e);
        }
    }


    public PathDataMaster findByPathAndProductInstanceIteration(String pPathAsString, ProductInstanceIteration pProductInstanceIteration) throws PathDataMasterNotFoundException {
        try {
            return em.createNamedQuery("PathDataMaster.findByPathAndProductInstanceIteration", PathDataMaster.class)
                    .setParameter("path", pPathAsString)
                    .setParameter("productInstanceIteration", pProductInstanceIteration)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new PathDataMasterNotFoundException(pPathAsString);
        }
    }

    public PathDataMaster findByPathIdAndProductInstanceIteration(int pPathId, ProductInstanceIteration pProductInstanceIteration) throws PathDataMasterNotFoundException {
        try {
            return em.createNamedQuery("PathDataMaster.findByPathIdAndProductInstanceIteration", PathDataMaster.class)
                    .setParameter("pathId", pPathId)
                    .setParameter("productInstanceIteration", pProductInstanceIteration)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new PathDataMasterNotFoundException(pPathId);
        }
    }

    public ProductInstanceMaster findByPathData(PathDataMaster pathDataMaster) {
        try {
            return em.createNamedQuery("ProductInstanceMaster.findByPathData", ProductInstanceMaster.class)
                    .setParameter("pathDataMasterList", pathDataMaster)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void removePathData(PathDataMaster pathDataMaster) {
        em.remove(pathDataMaster);
        em.flush();
    }
}
