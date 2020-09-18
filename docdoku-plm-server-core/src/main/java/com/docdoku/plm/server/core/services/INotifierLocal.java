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
package com.docdoku.plm.server.core.services;

import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.meta.Tag;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.workflow.Task;
import com.docdoku.plm.server.core.workflow.WorkspaceWorkflow;

import java.util.Collection;

/**
 * @author Florent Garin
 */
public interface INotifierLocal {

    // Basic account notifications
    void sendPasswordRecovery(Account account, String recoveryUUID);

    void sendCredential(Account account);

    // Admin level notifications
    void sendWorkspaceIndexationSuccess(Account account, String workspaceId, String extraMessage);

    void sendWorkspaceIndexationFailure(Account account, String workspaceId, String extraMessage);

    void sendWorkspaceDeletionNotification(Account admin, String workspaceId);

    void sendWorkspaceDeletionErrorNotification(Account admin, String workspaceId);

    void sendBulkIndexationSuccess(Account account);

    void sendBulkIndexationFailure(Account account, String failureMessage);

    // User level notifications
    void sendStateNotification(String workspaceId, Collection<User> pSubscribers, DocumentRevision pDocumentRevision);

    void sendIterationNotification(String workspaceId, Collection<User> pSubscribers, DocumentRevision pDocumentRevision);

    void sendApproval(String workspaceId, Collection<Task> pRunningTasks, DocumentRevision pDocumentRevision);

    void sendApproval(String workspaceId, Collection<Task> runningTasks, PartRevision partRevision);

    void sendApproval(String workspaceId, Collection<Task> runningTasks, WorkspaceWorkflow workspaceWorkflow);

    void sendPartRevisionWorkflowRelaunchedNotification(String workspaceId, PartRevision partRevision);

    void sendDocumentRevisionWorkflowRelaunchedNotification(String workspaceId, DocumentRevision pDocumentRevision);

    void sendWorkspaceWorkflowRelaunchedNotification(String workspaceId, WorkspaceWorkflow workspaceWorkflow);

    void sendTaggedNotification(String workspaceId, Collection<User> pSubscribers, DocumentRevision pDocR, Tag pTag);

    void sendUntaggedNotification(String workspaceId, Collection<User> pSubscribers, DocumentRevision pDocR, Tag pTag);

    void sendTaggedNotification(String workspaceId, Collection<User> pSubscribers, PartRevision pPartR, Tag pTag);

    void sendUntaggedNotification(String workspaceId, Collection<User> pSubscribers, PartRevision pPartR, Tag pTag);

}
