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

import com.docdoku.plm.server.core.exceptions.MarkerNotFoundException;
import com.docdoku.plm.server.core.product.Marker;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;


@RequestScoped
public class MarkerDAO {

    @Inject
    private EntityManager em;

    public MarkerDAO() {
    }

    public void createMarker(Marker pMarker) {
        em.persist(pMarker);
        em.flush();
    }

    public Marker loadMarker(int pId) throws MarkerNotFoundException {
        Marker marker = em.find(Marker.class, pId);
        if (marker == null) {
            throw new MarkerNotFoundException(pId);
        } else {
            return marker;
        }
    }

    public void removeMarker(int pId) throws MarkerNotFoundException {
        Marker marker = loadMarker(pId);
        em.remove(marker);
    }

}
