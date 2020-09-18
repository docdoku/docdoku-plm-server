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

import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.product.Conversion;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartRevision;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@RequestScoped
public class ConversionDAO {

    @Inject
    private EntityManager em;

    public ConversionDAO() {
    }

    public void createConversion(Conversion conversion) throws  CreationException {
        try{
            //the EntityExistsException is thrown only when flush occurs
            em.persist(conversion);
            em.flush();
        } catch(PersistenceException pPEx){
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException();
        }
    }

    public Conversion findConversion(PartIteration partIteration) {
        TypedQuery<Conversion> query = em.createQuery("SELECT DISTINCT c FROM Conversion c WHERE c.partIteration = :partIteration", Conversion.class);
        query.setParameter("partIteration", partIteration);
        try{
            return query.getSingleResult();
        }catch(NoResultException e){
            return null;
        }
    }

    public void deleteConversion(Conversion conversion) {
        em.remove(conversion);
        em.flush();
    }

    public void removePartRevisionConversions(PartRevision pPartR) {
        em.createQuery("DELETE FROM Conversion c WHERE c.partIteration.partRevision = :partRevision", Conversion.class)
                .setParameter("partRevision", pPartR)
                .executeUpdate();
    }

    public void removePartIterationConversion(PartIteration pPartI) {
        em.createQuery("DELETE FROM Conversion c WHERE c.partIteration = :partIteration", Conversion.class)
                .setParameter("partIteration", pPartI)
                .executeUpdate();
    }

    public Integer setPendingConversionsAsFailedIfOver(Integer retentionTimeMs) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, -retentionTimeMs);

        TypedQuery<Conversion> query =
                em.createQuery("SELECT DISTINCT c FROM Conversion c WHERE c.pending = true AND c.startDate <= :date", Conversion.class)
                        .setParameter("date", calendar.getTime());

        List<Conversion> conversions = query.getResultList();
        if(!conversions.isEmpty()){
            for(Conversion conversion: conversions){
                conversion.setPending(false);
                conversion.setEndDate(new Date());
                conversion.setSucceed(false);
            }
            em.flush();
        }

        return conversions.size();

    }
}
