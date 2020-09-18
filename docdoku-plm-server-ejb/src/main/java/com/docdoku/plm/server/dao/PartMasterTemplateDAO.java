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
import com.docdoku.plm.server.core.exceptions.PartMasterTemplateAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.PartMasterTemplateNotFoundException;
import com.docdoku.plm.server.core.meta.ListOfValuesKey;
import com.docdoku.plm.server.core.product.PartMasterTemplate;
import com.docdoku.plm.server.core.product.PartMasterTemplateKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.List;


@RequestScoped
public class PartMasterTemplateDAO {

    @Inject
    private EntityManager em;

    public PartMasterTemplateDAO() {
    }

    public PartMasterTemplate removePartMTemplate(PartMasterTemplateKey pKey) throws PartMasterTemplateNotFoundException {
        PartMasterTemplate template = loadPartMTemplate(pKey);
        em.remove(template);
        return template;
    }

    public List<PartMasterTemplate> findAllPartMTemplates(String pWorkspaceId) {
        TypedQuery<PartMasterTemplate> query = em.createQuery("SELECT DISTINCT t FROM PartMasterTemplate t WHERE t.workspaceId = :workspaceId", PartMasterTemplate.class);
        return query.setParameter("workspaceId", pWorkspaceId).getResultList();
    }

    public PartMasterTemplate loadPartMTemplate(PartMasterTemplateKey pKey)
            throws PartMasterTemplateNotFoundException {
        PartMasterTemplate template = em.find(PartMasterTemplate.class, pKey);
        if (template == null) {
            throw new PartMasterTemplateNotFoundException(pKey.getId());
        } else {
            return template;
        }
    }

    public void createPartMTemplate(PartMasterTemplate pTemplate) throws PartMasterTemplateAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pTemplate);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            throw new PartMasterTemplateAlreadyExistsException(pTemplate);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException();
        }
    }

    public List<PartMasterTemplate> findAllPartMTemplatesFromLOV(ListOfValuesKey lovKey){
        return em.createNamedQuery("PartMasterTemplate.findWhereLOV", PartMasterTemplate.class)
                .setParameter("lovName", lovKey.getName())
                .setParameter("workspace_id", lovKey.getWorkspaceId())
                .getResultList();
    }
}
