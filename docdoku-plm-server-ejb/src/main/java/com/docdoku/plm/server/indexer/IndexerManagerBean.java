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

package com.docdoku.plm.server.indexer;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.core.*;
import org.elasticsearch.index.query.QueryBuilder;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentMaster;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.query.DocumentSearchQuery;
import com.docdoku.plm.server.core.query.PartSearchQuery;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.i18n.PropertiesLoader;
import com.docdoku.plm.server.dao.DocumentMasterDAO;
import com.docdoku.plm.server.dao.PartMasterDAO;
import com.docdoku.plm.server.dao.WorkspaceDAO;
import com.docdoku.plm.server.indexer.util.IndexerMapping;
import com.docdoku.plm.server.indexer.util.IndicesUtils;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.*;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Morgan Guimard
 */
@Stateless(name = "IndexerManagerBean")
@Local(IIndexerManagerLocal.class)
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
public class IndexerManagerBean implements IIndexerManagerLocal {

    @Inject
    private JestClient esClient;

    @Inject
    private DocumentMasterDAO documentMasterDAO;

    @Inject
    private PartMasterDAO partMasterDAO;

    @Inject
    private WorkspaceDAO workspaceDAO;

    @Inject
    private IAccountManagerLocal accountManager;

    @Inject
    private INotifierLocal mailer;

    @Inject
    private IContextManagerLocal contextManager;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private IndexerResultsMapper indexerResultsMapper;

    @Inject
    private IndexerQueryBuilder indexerQueryBuilder;

    @Inject
    private IndexManagerBean indexManager;

    @Inject
    private IndicesUtils indicesUtils;

    private static final String I18N_CONF = "/com/docdoku/plm/server/core/i18n/LocalStrings";
    private static final Logger LOGGER = Logger.getLogger(IndexerManagerBean.class.getName());
    private static final Integer BULK_SIZE = 50;

    /**
     * Check for indexer availability
     *
     * @return ping result
     */
    @Override
    public boolean ping() {
        try {
            JestResult result = esClient.execute(new Health.Builder().build());
            return result.isSucceeded();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Create the indices for given workspace.
     *
     * @param workspaceId workspace id
     * @throws WorkspaceAlreadyExistsException if indices already exist
     */
    @Override
    @RolesAllowed({UserGroupMapping.ADMIN_ROLE_ID, UserGroupMapping.REGULAR_USER_ROLE_ID})
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void createWorkspaceIndex(String workspaceId) throws WorkspaceAlreadyExistsException {
        try {
            if(indexManager.indicesExist(workspaceId)){
                throw new WorkspaceAlreadyExistsException(workspaceId);
            }
            indexManager.createIndices(workspaceId);
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "Cannot create index for workspace [" + workspaceId + "]", e);
        }
    }

    /**
     * Delete the indices for given workspace
     *
     * @param workspaceId workspace id
     * @throws AccountNotFoundException
     */
    @Override
    @RolesAllowed({UserGroupMapping.ADMIN_ROLE_ID, UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void deleteWorkspaceIndex(String workspaceId) throws AccountNotFoundException {
        Account account = accountManager.getMyAccount();
        try {
            indexManager.deleteIndices(workspaceId);
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "Cannot delete index for workspace [" + workspaceId + "]. Consider deleting it manually.");
            LOGGER.log(Level.FINE, null, e);
            mailer.sendWorkspaceIndexationFailure(account, workspaceId, getString("IndexerNotAvailableException", account.getLocale()));
        }
    }

    /**
     * Index the given document iteration
     *
     * @param documentIteration document to index
     */
    @Override
    @Asynchronous
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void indexDocumentIteration(DocumentIteration documentIteration) {
        try {
            Update.Builder update = indexerQueryBuilder.updateRequest(documentIteration);
            DocumentResult execute = indexManager.executeUpdate(update);

            if (execute.isSucceeded()) {
                LOGGER.log(Level.INFO, "Document iteration [" + documentIteration.getKey() + "] indexed");
            } else {
                LOGGER.log(Level.WARNING, "The document " + documentIteration.getKey() + " cannot be indexed : \n" + execute.getErrorMessage());
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot build request for document: " + documentIteration.getKey(), e);
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "The document " + documentIteration.getKey() + " cannot be indexed.", e);
        }
    }

    /**
     * Index the given document iterations
     *
     * @param documentIterations documents to index
     */
    @Override
    @Asynchronous
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void indexDocumentIterations(List<DocumentIteration> documentIterations) {
        Bulk.Builder bulk = new Bulk.Builder();

        documentIterations.stream()
                .filter(documentIteration -> documentIteration.getCheckInDate() != null)
                .forEach(documentIteration -> addToBulk(documentIteration, bulk));
        try {
            indexManager.sendBulk(bulk);
        }  catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.FINE, null, e);
        }
    }

    /**
     * Index the given part iteration
     *
     * @param partIteration part to index
     */
    @Override
    @Asynchronous
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void indexPartIteration(PartIteration partIteration) {
        try {
            Update.Builder update = indexerQueryBuilder.updateRequest(partIteration);
            DocumentResult execute = indexManager.executeUpdate(update);

            if (execute.isSucceeded()) {
                LOGGER.log(Level.INFO, "Part iteration [" + partIteration.getKey() + "] indexed");
            } else {
                LOGGER.log(Level.WARNING, "The part " + partIteration.getKey() + " cannot be indexed : \n" + execute.getErrorMessage());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot build request for part: " + partIteration.getKey(), e);
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "The part " + partIteration.getKey() + " cannot be indexed.", e);
        }
    }

    /**
     * Index the given part iterations
     *
     * @param partIterations parts to index
     */
    @Override
    @Asynchronous
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void indexPartIterations(List<PartIteration> partIterations) {
        Bulk.Builder bulk = new Bulk.Builder();

        partIterations.stream()
                .filter(partIteration -> partIteration.getCheckInDate() != null)
                .forEach(partIteration -> addToBulk(partIteration, bulk));

        try {
            indexManager.sendBulk(bulk);
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.FINE, null, e);
        }
    }

    /**
     * Delete a document from index
     *
     * @param documentIteration document to remove
     */
    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void removeDocumentIterationFromIndex(DocumentIteration documentIteration) {
        try {
            String indexName = indicesUtils.getIndexName(documentIteration.getWorkspaceId(), IndexerMapping.INDEX_DOCUMENTS);
            indexManager.executeRemove(indexName, indicesUtils.formatDocId(documentIteration.getKey().toString()));
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "Cannot delete document " + documentIteration + ": The Elasticsearch cluster does not seem to respond");
        }
    }

    /**
     * Delete a part from index
     *
     * @param partIteration part to remove
     */
    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void removePartIterationFromIndex(PartIteration partIteration) {
        try {
            String indexName = indicesUtils.getIndexName(partIteration.getWorkspaceId(), IndexerMapping.INDEX_PARTS);
            indexManager.executeRemove(indexName, indicesUtils.formatDocId(partIteration.getKey().toString()));
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "Cannot delete part iteration " + partIteration + ": The Elasticsearch cluster does not seem to respond");
        }
    }

    /**
     * Run search on document revisions
     *
     * @param documentSearchQuery
     * @param from
     * @param size
     * @return
     * @throws AccountNotFoundException
     * @throws IndexerRequestException
     * @throws IndexerNotAvailableException
     */
    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public List<DocumentRevision> searchDocumentRevisions(DocumentSearchQuery documentSearchQuery, int from, int size)
            throws AccountNotFoundException, IndexerRequestException, IndexerNotAvailableException {
        String indexName = indicesUtils.getIndexName(documentSearchQuery.getWorkspaceId(), IndexerMapping.INDEX_DOCUMENTS);
        QueryBuilder query = indexerQueryBuilder.getSearchQueryBuilder(documentSearchQuery);
        SearchResult searchResult = indexManager.executeSearch(indexName, query, from, size);
        return indexerResultsMapper.processSearchResult(searchResult, documentSearchQuery);

    }

    /**
     * Run search on part revisions
     *
     * @param partSearchQuery
     * @param from
     * @param size
     * @return
     * @throws AccountNotFoundException
     * @throws IndexerRequestException
     * @throws IndexerNotAvailableException
     */
    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public List<PartRevision> searchPartRevisions(PartSearchQuery partSearchQuery, int from, int size)
            throws AccountNotFoundException, IndexerRequestException, IndexerNotAvailableException {
        String indexName = indicesUtils.getIndexName(partSearchQuery.getWorkspaceId(), IndexerMapping.INDEX_PARTS);
        QueryBuilder query = indexerQueryBuilder.getSearchQueryBuilder(partSearchQuery);
        SearchResult searchResult = indexManager.executeSearch(indexName, query, from, size);
        return indexerResultsMapper.processSearchResult(searchResult, partSearchQuery);
    }

    /**
     * Index all data from workspaces
     *
     * @throws AccountNotFoundException
     */
    @Override
    @RolesAllowed({UserGroupMapping.ADMIN_ROLE_ID})
    @Asynchronous
    public void indexAllWorkspacesData() {
        List<Workspace> workspaces = workspaceDAO.getAll();
        workspaces.forEach(workspace -> doIndexWorkspaceData(workspace.getId()));
    }

    /**
     * Index data from a given workspace
     *
     * @param workspaceId workspace to index
     * @throws WorkspaceNotFoundException
     * @throws AccountNotFoundException
     * @throws AccessRightException
     */
    @Override
    @RolesAllowed({UserGroupMapping.ADMIN_ROLE_ID, UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Asynchronous
    public void indexWorkspaceData(String workspaceId) {
        doIndexWorkspaceData(workspaceId);
    }

    private void doIndexWorkspaceData(String workspaceId){
        Account account;

        try {
            account = accountManager.getMyAccount();
        } catch (AccountNotFoundException e) {
            LOGGER.severe("Account not found");
            return;
        }

        if (contextManager.isCallerInRole(UserGroupMapping.REGULAR_USER_ROLE_ID)) {
            try {
                userManager.checkAdmin(workspaceId);
            } catch (AccessRightException | AccountNotFoundException | WorkspaceNotFoundException e) {
                LOGGER.severe("Not an admin");
                return;
            }
        }

        try {
            // force recreate
            if(indexManager.indicesExist(workspaceId)) {
                indexManager.deleteIndices(workspaceId);
            }

            indexManager.createIndices(workspaceId);

            List<BulkResult> bulkErrors = new ArrayList<>();

            bulkErrors.addAll(indexWorkspaceDocuments(workspaceId));
            bulkErrors.addAll(indexWorkspaceParts(workspaceId));

            List<String> errors = bulkErrors.stream().map(JestResult::getErrorMessage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if(errors.isEmpty()){
                mailer.sendBulkIndexationSuccess(account);
            }else{
                String failureMessage = String.join(", ", errors);
                LOGGER.log(Level.WARNING, "Failures while bulk indexing workspace [" + workspaceId + "]: \n" + failureMessage);
                mailer.sendBulkIndexationFailure(account, failureMessage);
            }

        } catch (IndexerRequestException | IndexerNotAvailableException e) {
            LOGGER.log(Level.WARNING, "The workspace " + workspaceId + " cannot be indexed.", e);
        }
    }


    private List<BulkResult> indexWorkspaceParts(String workspaceId) throws IndexerRequestException, IndexerNotAvailableException {

        Long countByWorkspace = partMasterDAO.getCountByWorkspace(workspaceId);
        int numberOfPage = (int) Math.ceil(countByWorkspace.doubleValue() / BULK_SIZE.doubleValue());

        List<BulkResult> results = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < numberOfPage; pageIndex++) {
            int offset = pageIndex * BULK_SIZE;
            results.add(sendPartsBulk(workspaceId, offset));
        }

        return results.stream().filter(bulkResult -> !bulkResult.getFailedItems().isEmpty()).collect(Collectors.toList());

    }

    private BulkResult sendPartsBulk(String workspaceId, int offset) throws IndexerRequestException, IndexerNotAvailableException {
        Bulk.Builder bulk = new Bulk.Builder();
        List<PartMaster> paginatedByWorkspace = partMasterDAO.getPaginatedByWorkspace(workspaceId, BULK_SIZE, offset);
        paginatedByWorkspace.stream()
                .flatMap(partM -> partM.getPartRevisions().stream().map(PartRevision::getPartIterations))
                .flatMap(Collection::stream).forEach(partIteration -> addToBulk(partIteration, bulk));

        return indexManager.sendBulk(bulk);
    }

    private List<BulkResult> indexWorkspaceDocuments(String workspaceId) throws IndexerRequestException, IndexerNotAvailableException {

        Long countByWorkspace = documentMasterDAO.getCountByWorkspace(workspaceId);
        int numberOfPage = (int) Math.ceil(countByWorkspace.doubleValue() / BULK_SIZE.doubleValue());

        List<BulkResult> results = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < numberOfPage; pageIndex++) {
            int offset = pageIndex * BULK_SIZE;
            results.add(sendDocumentsBulk(workspaceId, offset));
        }
        return results.stream().filter(bulkResult -> !bulkResult.getFailedItems().isEmpty()).collect(Collectors.toList());

    }

    private BulkResult sendDocumentsBulk(String workspaceId, int offset) throws IndexerRequestException, IndexerNotAvailableException {

        Bulk.Builder bulk = new Bulk.Builder();
        List<DocumentMaster> paginatedByWorkspace = documentMasterDAO.getPaginatedByWorkspace(workspaceId, BULK_SIZE, offset);

        paginatedByWorkspace.stream()
                .flatMap(docM -> docM.getDocumentRevisions().stream().map(DocumentRevision::getDocumentIterations))
                .flatMap(Collection::stream).forEach(documentIteration -> addToBulk(documentIteration, bulk));

        return indexManager.sendBulk(bulk);
    }

    private void addToBulk(DocumentIteration documentIteration, Bulk.Builder bulk) {
        try {
            bulk.addAction(indexerQueryBuilder.updateRequest(documentIteration).build());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to construct update query for document: " + documentIteration.getKey() + " \n " + e.getMessage());
            LOGGER.log(Level.FINE, null, e);
        }
    }

    private void addToBulk(PartIteration partIteration, Bulk.Builder bulk) {
        try {
            bulk.addAction(indexerQueryBuilder.updateRequest(partIteration).build());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to construct update query for part: " + partIteration.getKey() + " \n " + e.getMessage());
            LOGGER.log(Level.FINE, null, e);
        }
    }

    // todo remove localization and account manager usage from this class
    private String getString(String key, Locale locale) {
        Properties properties = PropertiesLoader.loadLocalizedProperties(locale, I18N_CONF, getClass());
        return properties.getProperty(key);
    }

}

