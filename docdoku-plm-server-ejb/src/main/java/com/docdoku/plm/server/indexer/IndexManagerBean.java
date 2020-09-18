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
import io.searchbox.core.*;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.template.PutTemplate;
import io.searchbox.params.SearchType;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.docdoku.plm.server.core.exceptions.IndexerNotAvailableException;
import com.docdoku.plm.server.core.exceptions.IndexerRequestException;
import com.docdoku.plm.server.config.IndexerConfig;
import com.docdoku.plm.server.indexer.util.IndexerMapping;
import com.docdoku.plm.server.indexer.util.IndicesUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for indexes and templates creation
 *
 * @author Morgan Guimard
 */
@Singleton(name = "IndexManagerBean")
public class IndexManagerBean {

    @Inject
    private IndexerConfig config;

    @Inject
    private IndicesUtils indicesUtils;

    @Inject
    private JestClient esClient;

    private static final Logger LOGGER = Logger.getLogger(IndexManagerBean.class.getName());

    public IndexManagerBean() {
    }

    @PostConstruct
    public void init(){
        try {
            initTemplate(IndexerMapping.COMMON_INDEX_NAME, IndexerMapping.COMMON_TEMPLATE);
            initTemplate(IndexerMapping.DOCUMENTS_INDEX_NAME, IndexerMapping.DOCUMENT_TEMPLATE);
            initTemplate(IndexerMapping.PARTS_INDEX_NAME, IndexerMapping.PART_TEMPLATE);
        } catch (IndexerNotAvailableException | IndexerRequestException e) {
            LOGGER.log(Level.WARNING, "Error while creating templates", e);
        }
    }

    /**
     * Create documents and parts indexes
     *
     * @param workspaceId
     * @throws IndexerRequestException
     * @throws IndexerNotAvailableException
     */
    public void createIndices(String workspaceId) throws IndexerRequestException, IndexerNotAvailableException {
        createIndex(indicesUtils.getIndexName(workspaceId, IndexerMapping.INDEX_DOCUMENTS));
        createIndex(indicesUtils.getIndexName(workspaceId, IndexerMapping.INDEX_PARTS));
    }

    /**
     *  Delete documents and parts indexes
     *
     * @param workspaceId
     * @throws IndexerNotAvailableException
     * @throws IndexerRequestException
     */
    public void deleteIndices(String workspaceId) throws IndexerNotAvailableException, IndexerRequestException {
        deleteIndex(indicesUtils.getIndexName(workspaceId, IndexerMapping.INDEX_DOCUMENTS));
        deleteIndex(indicesUtils.getIndexName(workspaceId, IndexerMapping.INDEX_PARTS));
    }

    /**
     * Removes an entry from elasticsearch
     *
     * @param indexName
     * @param id
     * @throws IndexerNotAvailableException
     * @throws IndexerRequestException
     */
    public void executeRemove(String indexName, String id) throws IndexerNotAvailableException, IndexerRequestException {
        Delete deleteRequest = new Delete.Builder(id)
                .index(indexName)
                .type(IndexerMapping.TYPE)
                .build();

        DocumentResult result;

        try {
            result = esClient.execute(deleteRequest);

            if (!result.isSucceeded()) {
                LOGGER.log(Level.WARNING, "Cannot delete document " + id + " in index " + indexName + ": " + result.getErrorMessage());
                throw new IndexerRequestException(result.getErrorMessage());
            }

        } catch (IOException e) {
            throw new IndexerNotAvailableException();
        }
    }

    /**
     * Execute a search query
     *
     * @param indexName
     * @param query
     * @param from
     * @param size
     * @return
     * @throws IndexerNotAvailableException
     * @throws IndexerRequestException
     */
    public SearchResult executeSearch(String indexName, QueryBuilder query, int from, int size)
            throws IndexerNotAvailableException, IndexerRequestException {
        try {
            LOGGER.log(Level.INFO, "ElasticSearchQuery:\n" + query.toString());
            SearchResult searchResult = esClient.execute(new Search.Builder(
                            new SearchSourceBuilder()
                                    .query(query)
                                    .from(from)
                                    .size(size)
                                    .toString())
                            .addIndex(indexName)
                            .addType(IndexerMapping.TYPE)
                            .setSearchType(SearchType.QUERY_THEN_FETCH)
                            .build()
            );

            if (!searchResult.isSucceeded()) {
                String reason = searchResult.getErrorMessage();
                LOGGER.log(Level.SEVERE, reason);
                throw new IndexerRequestException(reason);
            }

            return searchResult;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Search request failed: " + e.getMessage());
            throw new IndexerNotAvailableException();
        }
    }

    /**
     * Sends an bulk query
     *
     * @param bulk
     * @return
     * @throws IndexerNotAvailableException
     * @throws IndexerRequestException
     */
    public BulkResult sendBulk(Bulk.Builder bulk) throws IndexerNotAvailableException, IndexerRequestException {
        try {
            return esClient.execute(bulk.build());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send bulk request \n " + e.getMessage());
            LOGGER.log(Level.WARNING, "Search request failed: " + e.getMessage());
            throw new IndexerNotAvailableException();
        }
    }

    /**
     * Executes an update request
     *
     * @param update
     * @return
     * @throws IndexerNotAvailableException
     * @throws IndexerRequestException
     */
    public DocumentResult executeUpdate(Update.Builder update) throws IndexerNotAvailableException, IndexerRequestException {
        try {
            return esClient.execute(update.build());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send bulk request \n " + e.getMessage());
            LOGGER.log(Level.WARNING, "Search request failed: " + e.getMessage());
            throw new IndexerNotAvailableException();
        }
    }

    /**
     * Know if an index exists or not
     *
     * @param workspaceId
     * @return
     * @throws IndexerNotAvailableException
     */
    public boolean indicesExist(String workspaceId) throws IndexerNotAvailableException {
        return indexExists(indicesUtils.getIndexName(workspaceId, IndexerMapping.INDEX_DOCUMENTS)) ||
                indexExists(indicesUtils.getIndexName(workspaceId, IndexerMapping.INDEX_PARTS));
    }


    private boolean indexExists(String indexName) throws IndexerNotAvailableException {
        IndicesExists indicesExistsRequest = new IndicesExists.Builder(indexName).build();
        try {
            JestResult execute = esClient.execute(indicesExistsRequest);
            return execute.isSucceeded();
        } catch (IOException e) {
            throw new IndexerNotAvailableException();
        }
    }

    private void initTemplate(String name, String resourcePath) throws IndexerNotAvailableException, IndexerRequestException {
        try(InputStream inputStream = getClass().getResourceAsStream(resourcePath)){
            byte[] bytes = IOUtils.toByteArray(inputStream);
            String source = new String(bytes, StandardCharsets.UTF_8);

            JestResult result;
            try {
                result = esClient.execute(new PutTemplate.Builder(name, source).build());
            } catch (IOException e) {
                throw new IndexerNotAvailableException();
            }

            if (!result.isSucceeded()) {
                LOGGER.log(Level.WARNING, "Cannot create template " + name +": " + result.getErrorMessage());
            }else{
                LOGGER.log(Level.INFO, "Template " + name + " created");
            }
        } catch (IOException e) {
            throw new IndexerRequestException(e.getMessage());
        }
    }

    private void createIndex(String indexName) throws IndexerNotAvailableException, IndexerRequestException {
        Settings settings = Settings.builder().build();
        JestResult result;

        try {
            result = esClient.execute(new CreateIndex.Builder(indexName).settings(settings.toString()).build());
        } catch (IOException e) {
            throw new IndexerNotAvailableException();
        }

        if (!result.isSucceeded()) {
            LOGGER.log(Level.WARNING, "Cannot create index" + indexName + ": " + result.getErrorMessage());
            throw new IndexerRequestException(result.getErrorMessage());
        }
    }

    private void deleteIndex(String indexName) throws IndexerNotAvailableException, IndexerRequestException {
        if(!indexExists(indexName)){
            LOGGER.log(Level.FINE, "Index " + indexName + " cannot be deleted as it does not exists");
            return;
        }
        try {
            DocumentResult result = esClient.execute(new Delete.Builder(indexName).build());
            if (!result.isSucceeded()) {
                LOGGER.log(Level.WARNING, "Cannot delete index [" + indexName + "] : " + result.getErrorMessage());
                throw new IndexerRequestException(result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new IndexerNotAvailableException();
        }
    }

}
