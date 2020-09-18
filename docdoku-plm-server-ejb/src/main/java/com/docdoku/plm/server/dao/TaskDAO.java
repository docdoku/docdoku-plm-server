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

import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.exceptions.TaskNotFoundException;
import com.docdoku.plm.server.core.workflow.Task;
import com.docdoku.plm.server.core.workflow.TaskKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;



@RequestScoped
public class TaskDAO {

    @Inject
    private EntityManager em;

    public TaskDAO() {
    }
    
    public Task loadTask(TaskKey pTaskKey) throws TaskNotFoundException {
        Task task = em.find(Task.class,pTaskKey);
        if (task == null) {
            throw new TaskNotFoundException(pTaskKey);
        } else {
            return task;
        }
    }
    
    public Task[] findTasks(User pUser){
        Task[] tasks;
        TypedQuery<Task> query = em.createQuery("SELECT DISTINCT t FROM Task t WHERE t.worker = :user", Task.class);
        query.setParameter("user",pUser);
        List<Task> listTasks = query.getResultList();
        tasks = new Task[listTasks.size()];
        for(int i=0;i<listTasks.size();i++) {
            tasks[i] = listTasks.get(i);
        }
        
        return tasks;
    }

    public Task[] findAssignedTasks(String workspaceId, String userLogin){
        Task[] tasks;
        TypedQuery<Task> query = em.createNamedQuery("Task.findAssignedTasks", Task.class);
        query.setParameter("login", userLogin);
        query.setParameter("workspaceId",workspaceId);
        List<Task> listTasks = query.getResultList();
        tasks = new Task[listTasks.size()];
        for(int i=0;i<listTasks.size();i++) {
            tasks[i] = listTasks.get(i);
        }

        return tasks;
    }

    public Task[] findInProgressTasks(String workspaceId, String userLogin){
        Task[] tasks;
        TypedQuery<Task> query = em.createNamedQuery("Task.findInProgressTasks", Task.class);
        query.setParameter("login", userLogin);
        query.setParameter("workspaceId",workspaceId);
        List<Task> listTasks = query.getResultList();
        tasks = new Task[listTasks.size()];
        for(int i=0;i<listTasks.size();i++) {
            tasks[i] = listTasks.get(i);
        }

        return tasks;
    }

}
