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

import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.query.DocumentSearchQuery;
import com.docdoku.plm.server.core.query.PartSearchQuery;

import java.util.List;

public interface IIndexerManagerLocal {

    void createWorkspaceIndex(String workspaceId) throws WorkspaceAlreadyExistsException;

    void deleteWorkspaceIndex(String workspaceId) throws AccountNotFoundException;

    void indexDocumentIteration(DocumentIteration documentIteration);

    void indexDocumentIterations(List<DocumentIteration> documentIterations);

    void indexPartIteration(PartIteration partIteration);

    void indexPartIterations(List<PartIteration> partIterations);

    void removeDocumentIterationFromIndex(DocumentIteration documentIteration);

    void removePartIterationFromIndex(PartIteration partIteration);

    List<DocumentRevision> searchDocumentRevisions(DocumentSearchQuery documentSearchQuery, int from, int size) throws AccountNotFoundException, IndexerNotAvailableException, IndexerRequestException;

    List<PartRevision> searchPartRevisions(PartSearchQuery partSearchQuery, int from, int size) throws AccountNotFoundException, IndexerNotAvailableException, IndexerRequestException;

    void indexAllWorkspacesData() throws AccountNotFoundException;

    void indexWorkspaceData(String workspaceId) throws WorkspaceNotFoundException, AccountNotFoundException, AccessRightException;

    boolean ping();
}
