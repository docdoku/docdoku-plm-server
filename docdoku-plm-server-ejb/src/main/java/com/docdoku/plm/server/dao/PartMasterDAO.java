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
import com.docdoku.plm.server.core.exceptions.PartMasterAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.PartMasterNotFoundException;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartMasterKey;
import com.docdoku.plm.server.core.product.PartRevision;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class PartMasterDAO {

    public static final String WORKSPACE_ID = "workspaceId";

    @Inject
    private EntityManager em;

    @Inject
    private PartRevisionDAO partRevisionDAO;

    @Inject
    private WorkflowDAO workflowDAO;

    private static final Logger LOGGER = Logger.getLogger(PartMasterDAO.class.getName());

    public PartMasterDAO() {
    }

    public PartMaster loadPartM(PartMasterKey pKey) throws PartMasterNotFoundException {
        PartMaster partM = em.find(PartMaster.class, pKey);
        if (partM == null) {
            throw new PartMasterNotFoundException(pKey.getNumber());
        } else {
            return partM;
        }
    }

    public PartMaster getPartMRef(PartMasterKey pKey) throws PartMasterNotFoundException {
        try {
            return em.getReference(PartMaster.class, pKey);
        } catch (EntityNotFoundException pENFEx) {
            LOGGER.log(Level.FINEST,null,pENFEx);
            throw new PartMasterNotFoundException(pKey.getNumber());
        }
    }

    public void createPartM(PartMaster pPartM) throws PartMasterAlreadyExistsException, CreationException {
        try {
            PartRevision firstRev = pPartM.getLastRevision();
            if(firstRev!=null && firstRev.getWorkflow()!=null) {
                workflowDAO.createWorkflow(firstRev.getWorkflow());
            }
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pPartM);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            LOGGER.log(Level.FINEST,null,pEEEx);
            throw new PartMasterAlreadyExistsException(pPartM);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            LOGGER.log(Level.FINEST,null,pPEx);
            throw new CreationException();
        }
    }

    public void removePartM(PartMaster pPartM) {
        for(PartRevision partRevision:pPartM.getPartRevisions()){
            partRevisionDAO.removeRevision(partRevision);
        }
        em.remove(pPartM);
    }

    public List<PartMaster> findPartMasters(String workspaceId, String partNumber, String partName, int maxResults){
        return em.createNamedQuery("PartMaster.findByNameOrNumber", PartMaster.class)
            .setParameter("partNumber", partNumber)
            .setParameter("partName", partName)
            .setParameter(WORKSPACE_ID, workspaceId)
            .setMaxResults(maxResults)
            .getResultList();
    }

    public String findLatestPartMId(String pWorkspaceId, String pType) {
        String partMId;
        TypedQuery<String> query = em.createQuery("SELECT m.number FROM PartMaster m "
                + "WHERE m.workspace.id = :workspaceId "
                + "AND m.type = :type "
                + "AND m.creationDate = ("
                + "SELECT MAX(m2.creationDate) FROM PartMaster m2 "
                + "WHERE m2.workspace.id = :workspaceId "
                + "AND m2.type = :type "
                + ")", String.class);
        query.setParameter(WORKSPACE_ID, pWorkspaceId);
        query.setParameter("type", pType);
        partMId = query.getSingleResult();
        return partMId;
    }

    public List<PartMaster> getPartMasters(String pWorkspaceId, int pStart, int pMaxResults) {
        return em.createNamedQuery("PartMaster.findByWorkspace", PartMaster.class)
                .setParameter(WORKSPACE_ID, pWorkspaceId)
                .setFirstResult(pStart)
                .setMaxResults(pMaxResults)
                .getResultList();
    }

    public long getDiskUsageForPartsInWorkspace(String pWorkspaceId) {
        Number result = (Number)em.createNamedQuery("BinaryResource.diskUsageInPath")
                .setParameter("path", pWorkspaceId+"/parts/%")
                .getSingleResult();

        return result != null ? result.longValue() : 0L;
    }

    public long getDiskUsageForPartTemplatesInWorkspace(String pWorkspaceId) {
        Number result = (Number)em.createNamedQuery("BinaryResource.diskUsageInPath")
                .setParameter("path", pWorkspaceId+"/part-templates/%")
                .getSingleResult();

        return result != null ? result.longValue() : 0L;
    }

    public List<PartMaster> getPaginatedByWorkspace(String workspaceId, int limit, int offset) {
        return em.createNamedQuery("PartMaster.findByWorkspace",PartMaster.class)
                .setParameter(WORKSPACE_ID,workspaceId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public Long getCountByWorkspace(String workspaceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<PartMaster> pm = countQuery.from(PartMaster.class);
        countQuery.select(cb.count(pm)).where(cb.equal(pm.get("workspace").get("id"), workspaceId));
        return em.createQuery(countQuery).getSingleResult();
    }
}
