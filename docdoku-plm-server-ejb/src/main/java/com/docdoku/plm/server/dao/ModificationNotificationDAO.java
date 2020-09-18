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

import com.docdoku.plm.server.core.change.ModificationNotification;
import com.docdoku.plm.server.core.product.PartIterationKey;
import com.docdoku.plm.server.core.product.PartRevisionKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

@RequestScoped
public class ModificationNotificationDAO {

    public static final String WORKSPACE_ID = "workspaceId";
    public static final String PART_NUMBER = "partNumber";
    public static final String VERSION = "version";

    @Inject
    private EntityManager em;

    public void removeModificationNotifications(PartIterationKey pPartIPK){
        em.createNamedQuery("ModificationNotification.removeAllOnPartIteration")
                .setParameter(WORKSPACE_ID, pPartIPK.getWorkspaceId())
                .setParameter(PART_NUMBER, pPartIPK.getPartMasterNumber())
                .setParameter(VERSION, pPartIPK.getPartRevisionVersion())
                .setParameter("iteration", pPartIPK.getIteration()).executeUpdate();
    }

    public void removeModificationNotifications(PartRevisionKey pPartRPK){
        em.createNamedQuery("ModificationNotification.removeAllOnPartRevision")
                .setParameter(WORKSPACE_ID, pPartRPK.getWorkspaceId())
                .setParameter(PART_NUMBER, pPartRPK.getPartMasterNumber())
                .setParameter(VERSION, pPartRPK.getVersion()).executeUpdate();
    }

    public void createModificationNotification(ModificationNotification pNotification) {
        em.persist(pNotification);
    }

    public ModificationNotification getModificationNotification(int pId) {
        return em.find(ModificationNotification.class, pId);
    }

    public List<ModificationNotification> getModificationNotifications(PartIterationKey pPartIPK) {
        return em.createNamedQuery("ModificationNotification.findByImpactedPartIteration", ModificationNotification.class)
                .setParameter(WORKSPACE_ID, pPartIPK.getWorkspaceId())
                .setParameter(PART_NUMBER, pPartIPK.getPartMasterNumber())
                .setParameter(VERSION, pPartIPK.getPartRevisionVersion())
                .setParameter("iteration", pPartIPK.getIteration()).getResultList();
    }

    public boolean hasModificationNotifications(PartIterationKey pPartIPK){
        return !getModificationNotifications(pPartIPK).isEmpty();
    }

}
