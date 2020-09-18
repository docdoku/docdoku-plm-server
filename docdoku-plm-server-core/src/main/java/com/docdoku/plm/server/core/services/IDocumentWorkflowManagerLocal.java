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

import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.workflow.TaskKey;
import com.docdoku.plm.server.core.workflow.Workflow;

/**
 *
 * @author Taylor Labejof
 * @version 2.0, 15/10/14
 * @since   V2.0
 */
public interface IDocumentWorkflowManagerLocal {
    Workflow getCurrentWorkflow(DocumentRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, AccessRightException, WorkflowNotFoundException, WorkspaceNotEnabledException;
    Workflow[] getAbortedWorkflow(DocumentRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException;

    DocumentRevision approveTaskOnDocument(String workspaceId, TaskKey pTaskKey, DocumentRevisionKey documentRevisionKey, String pComment, String pSignature) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, WorkflowNotFoundException, AccessRightException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException;
    DocumentRevision rejectTaskOnDocument(String workspaceId, TaskKey pTaskKey, DocumentRevisionKey documentRevisionKey, String pComment, String pSignature) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, WorkflowNotFoundException, AccessRightException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException;
}
