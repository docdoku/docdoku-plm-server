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

import com.docdoku.plm.server.core.change.ChangeOrder;
import com.docdoku.plm.server.core.change.ChangeRequest;
import com.docdoku.plm.server.core.change.Milestone;
import com.docdoku.plm.server.core.exceptions.MilestoneAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.MilestoneNotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;


@RequestScoped
public class MilestoneDAO {

    public static final String WORKSPACE_ID = "workspaceId";
    public static final String MILESTONE_ID = "milestoneId";

    @Inject
    private EntityManager em;

    @Inject
    private ACLDAO aclDAO;

    public MilestoneDAO() {
    }


    public List<Milestone> findAllMilestone(String pWorkspaceId) {
        return em.createNamedQuery("Milestone.findMilestonesByWorkspace", Milestone.class)
                                        .setParameter(WORKSPACE_ID, pWorkspaceId)
                                        .getResultList();
    }
    
    public Milestone loadMilestone(int pId) throws MilestoneNotFoundException {
        Milestone milestone = em.find(Milestone.class, pId);
        if (milestone == null) {
            throw new MilestoneNotFoundException(pId);
        } else {
            return milestone;
        }
    }

    public Milestone loadMilestone(String pTitle, String pWorkspace) throws MilestoneNotFoundException {
        Milestone milestone = em.createNamedQuery("Milestone.findMilestonesByTitleAndWorkspace", Milestone.class)
                .setParameter("title", pTitle)
                .setParameter(WORKSPACE_ID, pWorkspace)
                .getSingleResult();
        if (milestone == null) {
            throw new MilestoneNotFoundException(pTitle);
        } else {
            return milestone;
        }
    }

    public void createMilestone(Milestone pMilestone) throws MilestoneAlreadyExistsException {
        if(!this.checkTitleUniqueness(pMilestone.getTitle(),pMilestone.getWorkspace().getId()))
            throw new MilestoneAlreadyExistsException(pMilestone.getTitle(), null);

        if(pMilestone.getACL()!=null) {
            aclDAO.createACL(pMilestone.getACL());
        }

        em.persist(pMilestone);
        em.flush();
    }

    public void deleteMilestone(Milestone pMilestone) {
        em.remove(pMilestone);
        em.flush();
    }

    public List<ChangeRequest> getAllRequests(int pId,String pWorkspace){
        try{
            return em.createNamedQuery("ChangeRequest.getRequestByMilestonesAndWorkspace",ChangeRequest.class)
                    .setParameter(MILESTONE_ID, pId)
                    .setParameter(WORKSPACE_ID, pWorkspace)
                    .getResultList();
        }catch(Exception e){
            return null;
        }
    }

    public List<ChangeOrder> getAllOrders(int pId,String pWorkspace){
        try{
            return em.createNamedQuery("ChangeOrder.getOrderByMilestonesAndWorkspace",ChangeOrder.class)
                    .setParameter(MILESTONE_ID, pId)
                    .setParameter(WORKSPACE_ID, pWorkspace)
                    .getResultList();
        }catch(Exception e){
            return null;
        }
    }

    public int getNumberOfRequests(int pId,String pWorkspace){
        try{
            return ((Number)em.createNamedQuery("ChangeRequest.countRequestByMilestonesAndWorkspace")
                    .setParameter(MILESTONE_ID, pId)
                    .setParameter(WORKSPACE_ID, pWorkspace)
                    .getSingleResult()).intValue();
        }catch(Exception e){
            return 0;
        }
    }

    public int getNumberOfOrders(int pId,String pWorkspace){
        try{
            return ((Number)em.createNamedQuery("ChangeOrder.countOrderByMilestonesAndWorkspace")
                    .setParameter(MILESTONE_ID, pId)
                    .setParameter(WORKSPACE_ID, pWorkspace)
                    .getSingleResult()).intValue();
        }catch(Exception e){
            return 0;
        }
    }
    
    private boolean checkTitleUniqueness(String pTitle,String pWorkspace){
        try{
            return em.createNamedQuery("Milestone.findMilestonesByTitleAndWorkspace")
                    .setParameter("title", pTitle)
                    .setParameter(WORKSPACE_ID, pWorkspace)
                    .getResultList().isEmpty();
        }catch (NoResultException e){
            return true;
        }
    }
}
