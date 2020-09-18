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

import io.searchbox.core.Update;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.query.DocumentSearchQuery;
import com.docdoku.plm.server.core.query.PartSearchQuery;
import com.docdoku.plm.server.core.query.SearchQuery;
import com.docdoku.plm.server.indexer.util.EntityMapper;
import com.docdoku.plm.server.indexer.util.IndexerMapping;
import com.docdoku.plm.server.indexer.util.IndicesUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds elasticsearch requests
 *
 * @author Morgan Guimard
 */
@Stateless(name = "IndexerQueryBuilder")
public class IndexerQueryBuilder {

    @Inject
    private IndexerTextExtractor textExtractor;

    @Inject
    private IndicesUtils indicesUtils;

    private static final String FUZZINESS = "AUTO";

    /**
     * Creates an index request for given document
     *
     * @param documentIteration
     * @return
     * @throws IOException
     */
    public Update.Builder updateRequest(DocumentIteration documentIteration) throws IOException {
        Map<String, String> contentInputs = textExtractor.getContentInputs(documentIteration.getAttachedFiles());
        try (XContentBuilder xcb = XContentFactory.jsonBuilder()) {
            xcb.startObject()
                    .field("doc_as_upsert", true)
                    .startObject("doc");
            EntityMapper.documentIterationToJSON(xcb, documentIteration, contentInputs);
            xcb.endObject().endObject();
            return new Update.Builder(Strings.toString(xcb))
                    .index(indicesUtils.getIndexName(documentIteration.getWorkspaceId(), IndexerMapping.INDEX_DOCUMENTS))
                    .type(IndexerMapping.TYPE)
                    .id(indicesUtils.formatDocId(documentIteration.getKey().toString()));
        }
    }
    /**
     * Creates an index request for given part
     *
     * @param partIteration
     * @return
     * @throws IOException
     */
    public Update.Builder updateRequest(PartIteration partIteration) throws IOException {
        Map<String, String> contentInputs = textExtractor.getContentInputs(partIteration.getAttachedFiles());
        try (XContentBuilder xcb = XContentFactory.jsonBuilder()) {
            xcb.startObject()
                    .field("doc_as_upsert", true)
                    .startObject("doc");
            EntityMapper.partIterationToJSON(xcb, partIteration, contentInputs);
            xcb.endObject().endObject();
            return new Update.Builder(Strings.toString(xcb))
                    .index(indicesUtils.getIndexName(partIteration.getWorkspaceId(), IndexerMapping.INDEX_PARTS))
                    .type(IndexerMapping.TYPE)
                    .id(indicesUtils.formatDocId(partIteration.getKey().toString()));
        }
    }


    /**
     * Create the search query builder from given document search query
     *
     * @param documentSearchQuery
     * @return
     */
    public QueryBuilder getSearchQueryBuilder(DocumentSearchQuery documentSearchQuery) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> documentQueries = createQueries(documentSearchQuery);
        documentQueries.forEach(boolQuery::must);
        return boolQuery;
    }

    /**
     * Create the search query builder from given part search query
     *
     * @param partSearchQuery
     * @return
     */
    public QueryBuilder getSearchQueryBuilder(PartSearchQuery partSearchQuery) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> partQueries = createQueries(partSearchQuery);
        partQueries.forEach(boolQuery::must);
        return boolQuery;
    }


    private List<QueryBuilder> createQueries(DocumentSearchQuery documentSearchQuery) {
        List<QueryBuilder> queries = new ArrayList<>();

        String docMId = documentSearchQuery.getDocMId();
        String title = documentSearchQuery.getTitle();
        String folder = documentSearchQuery.getFolder();

        if (docMId != null && !docMId.isEmpty()) {
            queries.add(QueryBuilders.multiMatchQuery(docMId, IndexerMapping.DOCUMENT_ID_KEY));
        }

        if (title != null && !title.isEmpty()) {
            queries.add(QueryBuilders.multiMatchQuery(title, IndexerMapping.TITLE_KEY).fuzziness(FUZZINESS));
        }

        if (folder != null && !folder.isEmpty()) {
            queries.add(QueryBuilders.multiMatchQuery(folder, IndexerMapping.FOLDER_KEY).fuzziness(FUZZINESS));
        }

        queries.addAll(createCommonQueries(documentSearchQuery));

        return queries;
    }

    private List<QueryBuilder> createQueries(PartSearchQuery partSearchQuery) {
        List<QueryBuilder> queries = new ArrayList<>();

        String partNumber = partSearchQuery.getPartNumber();
        String partName = partSearchQuery.getName();

        if (partNumber != null && !partNumber.isEmpty()) {
            queries.add(QueryBuilders.multiMatchQuery(partNumber, IndexerMapping.PART_NUMBER_KEY));
        }

        if (partName != null && !partName.isEmpty()) {
            queries.add(QueryBuilders.multiMatchQuery(partName, IndexerMapping.PART_NAME_KEY).fuzziness(FUZZINESS));
        }

        queries.addAll(createCommonQueries(partSearchQuery));

        return queries;
    }

    private List<QueryBuilder> createCommonQueries(SearchQuery searchQuery) {

        String[] tags = searchQuery.getTags();
        SearchQuery.AbstractAttributeQuery[] attributes = searchQuery.getAttributes();

        String queryString = searchQuery.getQueryString();

        List<QueryBuilder> queries = new ArrayList<>();

        if (searchQuery.getVersion() != null) {
            queries.add(QueryBuilders.termQuery(IndexerMapping.VERSION_KEY, searchQuery.getVersion()));
        }
        if (searchQuery.getAuthor() != null) {
            BoolQueryBuilder authorQuery = QueryBuilders.boolQuery();
            authorQuery.should(QueryBuilders.multiMatchQuery(searchQuery.getAuthor(), IndexerMapping.AUTHOR_NAME_KEY).fuzziness(FUZZINESS));
            authorQuery.should(QueryBuilders.multiMatchQuery(searchQuery.getAuthor(), IndexerMapping.AUTHOR_LOGIN_KEY).fuzziness(FUZZINESS));
            queries.add(authorQuery);
        }
        if (searchQuery.getType() != null) {
            queries.add(QueryBuilders.multiMatchQuery(searchQuery.getType(), IndexerMapping.TYPE_KEY).fuzziness(FUZZINESS));
        }

        if (searchQuery.getCreationDateFrom() != null) {
            queries.add(QueryBuilders.rangeQuery(IndexerMapping.CREATION_DATE_KEY).from(searchQuery.getCreationDateFrom()));
        }

        if (searchQuery.getCreationDateTo() != null) {
            queries.add(QueryBuilders.rangeQuery(IndexerMapping.CREATION_DATE_KEY).to(searchQuery.getCreationDateTo()));
        }

        if (searchQuery.getModificationDateFrom() != null) {
            queries.add(QueryBuilders.rangeQuery(IndexerMapping.MODIFICATION_DATE_KEY).from(searchQuery.getModificationDateFrom()));
        }

        if (searchQuery.getModificationDateTo() != null) {
            queries.add(QueryBuilders.rangeQuery(IndexerMapping.MODIFICATION_DATE_KEY).to(searchQuery.getModificationDateTo()));
        }

        if (searchQuery.getContent() != null) {
            queries.add(QueryBuilders.nestedQuery(IndexerMapping.FILES_KEY, QueryBuilders.matchQuery(IndexerMapping.FILES_KEY + '.' + IndexerMapping.CONTENT_KEY, searchQuery.getContent()), ScoreMode.Avg));
        }

        if (tags != null && tags.length > 0) {
            queries.add(QueryBuilders.termsQuery(IndexerMapping.TAGS_KEY, tags));
        }

        if (attributes != null) {
            Stream.of(attributes)
                    .collect(Collectors.groupingBy(SearchQuery.AbstractAttributeQuery::getNameWithoutWhiteSpace))
                    .forEach((attributeName, attributeList) -> addAttributeToQueries(queries, attributeName, attributeList));
        }

        if (queryString != null && !queryString.isEmpty()) {
            List<QueryBuilder> queryStringQueries = new ArrayList<>();
            QueryBuilder queryStringQuery = QueryBuilders.queryStringQuery(queryString);
            queryStringQueries.add(queryStringQuery);
            queryStringQueries.add(QueryBuilders.nestedQuery(IndexerMapping.FILES_KEY, queryStringQuery, ScoreMode.None));
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            queryStringQueries.forEach(boolQuery::should);
            queries.add(boolQuery);
        }

        return queries;
    }

    private void addAttributeToQueries(List<QueryBuilder> queries, String attributeName, List<SearchQuery.AbstractAttributeQuery> attributeList) {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.must(QueryBuilders.nestedQuery(IndexerMapping.ATTRIBUTES_KEY,
                QueryBuilders.termQuery(IndexerMapping.ATTRIBUTES_KEY + "." + IndexerMapping.ATTRIBUTE_NAME, attributeName), ScoreMode.None));

        List<NestedQueryBuilder> nestedQueries = new ArrayList<>();
        BoolQueryBuilder valuesQuery = QueryBuilders.boolQuery();

        for (SearchQuery.AbstractAttributeQuery attr : attributeList) {
            String attributeValue = attr.toString();
            if (attributeValue != null && !attributeValue.isEmpty()) {
                nestedQueries.add(QueryBuilders.nestedQuery(IndexerMapping.ATTRIBUTES_KEY,
                        QueryBuilders.termQuery(IndexerMapping.ATTRIBUTES_KEY + "." + IndexerMapping.ATTRIBUTE_VALUE, attributeValue), ScoreMode.None));

            }
        }

        // Use 'should' on same attribute name, and 'must' for different attribute names
        // Only request for attribute name if no values
        // Use bool must if only one value passed
        // Compound should queries if many values (but must not be empty)
        if (!nestedQueries.isEmpty()) {
            if (nestedQueries.size() == 1) {
                boolQuery.must(nestedQueries.get(0));
            } else {
                nestedQueries.forEach(valuesQuery::should);
                boolQuery.must(valuesQuery);
                boolQuery.mustNot(QueryBuilders.nestedQuery(IndexerMapping.ATTRIBUTES_KEY,
                        QueryBuilders.termQuery(IndexerMapping.ATTRIBUTES_KEY + "." + IndexerMapping.ATTRIBUTE_VALUE, ""), ScoreMode.None));
            }
        }

        queries.add(boolQuery);

    }

}
