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

import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.workflow.Activity;
import com.docdoku.plm.server.core.workflow.Workflow;
import com.docdoku.plm.server.core.workflow.WorkspaceWorkflow;
import com.docdoku.plm.server.core.workflow.WorkspaceWorkflowKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

@RequestScoped
public class WorkflowDAO {

    public static final String WORKFLOW = "workflow";
    public static final String WORKSPACE = "workspace";

    @Inject
    private EntityManager em;

    public void createWorkflow(Workflow pWf) {
        //Hack to prevent a bug inside the JPA implementation (Eclipse Link)
        List<Activity> activities = pWf.getActivities();
        pWf.setActivities(new ArrayList<>());
        em.persist(pWf);
        em.flush();
        pWf.setActivities(activities);
    }

    public Workflow duplicateWorkflow(Workflow workflow) {
        Workflow duplicatedWF = new Workflow(workflow.getFinalLifeCycleState());
        em.persist(duplicatedWF);
        em.flush();

        List<Activity> rwActivities = new ArrayList<>();
        for (Activity activity : workflow.getActivities()) {
            Activity clonedActivity = activity.clone();
            clonedActivity.setWorkflow(duplicatedWF);
            Activity relaunchActivity = activity.getRelaunchActivity();
            if (relaunchActivity != null) {
                Activity clonedRelaunchActivity = rwActivities.get(relaunchActivity.getStep());
                clonedActivity.setRelaunchActivity(clonedRelaunchActivity);
            }
            rwActivities.add(clonedActivity);
        }

        duplicatedWF.setActivities(rwActivities);
        return duplicatedWF;

    }

    public DocumentRevision getDocumentTarget(Workflow pWorkflow) {
        TypedQuery<DocumentRevision> query = em.createNamedQuery("DocumentRevision.findByWorkflow", DocumentRevision.class);
        try {
            return query.setParameter(WORKFLOW, pWorkflow).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public PartRevision getPartTarget(Workflow pWorkflow) {
        TypedQuery<PartRevision> query = em.createNamedQuery("PartRevision.findByWorkflow", PartRevision.class);
        try {
            return query.setParameter(WORKFLOW, pWorkflow).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public WorkspaceWorkflow getWorkspaceWorkflowTarget(String workspaceId, Workflow workflow) {
        TypedQuery<WorkspaceWorkflow> query = em.createQuery("SELECT w FROM WorkspaceWorkflow w WHERE w.workflow = :workflow AND w.workspace.id = :workspaceId", WorkspaceWorkflow.class);
        try {
            return query.setParameter(WORKFLOW, workflow)
                    .setParameter("workspaceId", workspaceId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void removeWorkflowConstraints(WorkspaceWorkflow ww) {
        List<Workflow> abortedWorkflowList = ww.getAbortedWorkflows();
        Workflow workflow = ww.getWorkflow();
        abortedWorkflowList.forEach(this::removeWorkflowConstraints);
        removeWorkflowConstraints(workflow);
    }

    public void removeWorkflowConstraints(DocumentRevision pDocR) {
        List<Workflow> abortedWorkflowList = pDocR.getAbortedWorkflows();
        Workflow workflow = pDocR.getWorkflow();
        abortedWorkflowList.forEach(this::removeWorkflowConstraints);
        removeWorkflowConstraints(workflow);
    }

    public void removeWorkflowConstraints(PartRevision pPartR) {
        List<Workflow> abortedWorkflowList = pPartR.getAbortedWorkflows();
        Workflow workflow = pPartR.getWorkflow();
        abortedWorkflowList.forEach(this::removeWorkflowConstraints);
        removeWorkflowConstraints(workflow);
    }

    public void removeWorkflowConstraints(Workflow pWorkflow) {
        if (pWorkflow != null) {
            for (Activity activity : pWorkflow.getActivities()) {
                activity.setRelaunchActivity(null);
            }
        }
    }

    public Workflow getWorkflow(int workflowId) {
        return em.find(Workflow.class, workflowId);
    }


    public void createWorkspaceWorkflow(WorkspaceWorkflow workspaceWorkflow) {
        em.persist(workspaceWorkflow);
        em.flush();
    }

    public WorkspaceWorkflow getWorkspaceWorkflow(String workspaceId, String workspaceWorkflowId) {
        return em.find(WorkspaceWorkflow.class, new WorkspaceWorkflowKey(workspaceId, workspaceWorkflowId));
    }

    public List<WorkspaceWorkflow> getWorkspaceWorkflowList(String workspaceId) {
        TypedQuery<WorkspaceWorkflow> query = em.createQuery("SELECT w FROM WorkspaceWorkflow w WHERE w.workspace.id = :workspaceId", WorkspaceWorkflow.class);
        try {
            return query.setParameter("workspaceId", workspaceId).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void deleteWorkspaceWorkflow(WorkspaceWorkflow workspaceWorkflow) {
        removeWorkflowConstraints(workspaceWorkflow);
        em.remove(workspaceWorkflow);
        em.flush();
    }

    public void removeWorkflowConstraints(Workspace workspace) {
        removeWorkflowConstraintsOnDocuments(workspace);
        removeWorkflowConstraintsOnParts(workspace);
        removeWorkflowConstraintsOnWorkspaceWorkflow(workspace);
    }

    private void removeWorkflowConstraintsOnWorkspaceWorkflow(Workspace workspace) {
        TypedQuery<Workflow> query = em.createQuery("SELECT ww.workflow FROM WorkspaceWorkflow ww WHERE ww.workspace = :workspace AND ww.workflow IS NOT NULL",
                Workflow.class).setParameter(WORKSPACE, workspace);

        query.getResultList().forEach(this::removeWorkflowConstraints);

        query = em.createQuery("SELECT ww.abortedWorkflows FROM WorkspaceWorkflow ww WHERE ww.workspace = :workspace AND size(ww.abortedWorkflows) > 0",
                Workflow.class).setParameter(WORKSPACE, workspace);

        query.getResultList().forEach(this::removeWorkflowConstraints);
    }

    private void removeWorkflowConstraintsOnParts(Workspace workspace) {
        TypedQuery<Workflow> query = em.createQuery("SELECT p.workflow FROM PartRevision p WHERE p.partMaster.workspace = :workspace AND p.workflow IS NOT NULL",
                Workflow.class).setParameter(WORKSPACE, workspace);

        query.getResultList().forEach(this::removeWorkflowConstraints);

        query = em.createQuery("SELECT p.abortedWorkflows FROM PartRevision p WHERE p.partMaster.workspace = :workspace AND size(p.abortedWorkflows) > 0",
                Workflow.class).setParameter(WORKSPACE, workspace);

        query.getResultList().forEach(this::removeWorkflowConstraints);
    }

    private void removeWorkflowConstraintsOnDocuments(Workspace workspace) {
        TypedQuery<Workflow> query = em.createQuery("SELECT d.workflow FROM DocumentRevision d WHERE d.documentMaster.workspace = :workspace AND d.workflow IS NOT NULL",
                Workflow.class).setParameter(WORKSPACE, workspace);

        query.getResultList().forEach(this::removeWorkflowConstraints);

        query = em.createQuery("SELECT d.abortedWorkflows FROM DocumentRevision d WHERE d.documentMaster.workspace = :workspace AND size(d.abortedWorkflows) > 0",
                Workflow.class).setParameter(WORKSPACE, workspace);

        query.getResultList().forEach(this::removeWorkflowConstraints);
    }


}
