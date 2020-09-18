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

import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.product.PartRevisionKey;
import com.docdoku.plm.server.core.workflow.TaskKey;
import com.docdoku.plm.server.core.workflow.Workflow;

/**
 *
 * @author Taylor Labejof
 * @version 2.0, 15/10/14
 * @since   V2.0
 */
public interface IPartWorkflowManagerLocal {
    Workflow getCurrentWorkflow(PartRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException;
    Workflow[] getAbortedWorkflow(PartRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException;

    PartRevision approveTaskOnPart(String pWorkspaceId, TaskKey pTaskKey, PartRevisionKey partRevisionKey, String pComment, String pSignature) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, WorkflowNotFoundException, WorkspaceNotEnabledException, AccessRightException, PartRevisionNotFoundException;
    PartRevision rejectTaskOnPart(String pWorkspaceId, TaskKey pTaskKey, PartRevisionKey partRevisionKey, String pComment, String pSignature) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, WorkflowNotFoundException, WorkspaceNotEnabledException, AccessRightException, PartRevisionNotFoundException;
}
