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

import io.searchbox.core.SearchResult;
import com.docdoku.plm.server.core.document.DocumentIterationKey;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.DocumentRevisionNotFoundException;
import com.docdoku.plm.server.core.product.PartIterationKey;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.product.PartRevisionKey;
import com.docdoku.plm.server.core.query.DocumentSearchQuery;
import com.docdoku.plm.server.core.query.PartSearchQuery;
import com.docdoku.plm.server.dao.DocumentRevisionDAO;
import com.docdoku.plm.server.dao.PartRevisionDAO;
import com.docdoku.plm.server.indexer.util.IndexerMapping;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process elasticsearch results
 *
 * @author Morgan Guimard
 */
@Stateless(name = "IndexerResultsMapper")
public class IndexerResultsMapper {

    private static final Logger LOGGER = Logger.getLogger(IndexerResultsMapper.class.getName());

    @Inject
    private DocumentRevisionDAO documentRevisionDAO;

    @Inject
    private PartRevisionDAO partRevisionDAO;

    public IndexerResultsMapper(){
    }

    /**
     * Map results from elasticsearch to document revisions
     *
     * @param searchResult
     * @param documentSearchQuery
     * @return
     */
    public List<DocumentRevision> processSearchResult(SearchResult searchResult, DocumentSearchQuery documentSearchQuery) {
        List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
        Set<DocumentIterationKey> documentIterationKeys = new HashSet<>();

        if (hits != null) {
            for (SearchResult.Hit<Map, Void> hit : hits) {
                Map<?, ?> source = hit.source;
                documentIterationKeys.add(getDocumentIterationKey(source));
            }
        }

        LOGGER.log(Level.INFO, "Results: " + documentIterationKeys.size());
        return documentIterationKeysToDocumentRevisions(documentSearchQuery.isFetchHeadOnly(), documentIterationKeys);
    }

    /**
     * Map results from elasticsearch to part revisions
     *
     * @param searchResult
     * @param partSearchQuery
     * @return
     */
    public List<PartRevision> processSearchResult(SearchResult searchResult, PartSearchQuery partSearchQuery) {
        List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
        Set<PartIterationKey> partIterationKeys = new HashSet<>();

        if (hits != null) {
            for (SearchResult.Hit<Map, Void> hit : hits) {
                Map<?, ?> source = hit.source;
                partIterationKeys.add(getPartIterationKey(source));
            }
        }

        LOGGER.log(Level.INFO, "Results: " + partIterationKeys.size());
        return partIterationKeysToPartRevisions(partSearchQuery.isFetchHeadOnly(), partIterationKeys);
    }

    private List<DocumentRevision> documentIterationKeysToDocumentRevisions(boolean fetchHeadOnly, Set<DocumentIterationKey> documentIterationKeys) {
        Set<DocumentRevision> documentRevisions = new HashSet<>();

        for (DocumentIterationKey documentIterationKey : documentIterationKeys) {
            DocumentRevision documentRevision = getDocumentRevision(documentIterationKey.getDocumentRevision());
            if (documentRevision != null && !documentRevisions.contains(documentRevision)) {
                if (fetchHeadOnly) {
                    if (documentRevision.getLastCheckedInIteration().getKey().equals(documentIterationKey)) {
                        documentRevisions.add(documentRevision);
                    }
                } else {
                    documentRevisions.add(documentRevision);
                }
            }
        }

        return new ArrayList<>(documentRevisions);
    }

    private List<PartRevision> partIterationKeysToPartRevisions(boolean fetchHeadOnly, Set<PartIterationKey> partIterationKeys) {
        Set<PartRevision> partRevisions = new HashSet<>();

        for (PartIterationKey partIterationKey : partIterationKeys) {
            PartRevision partRevision = getPartRevision(partIterationKey.getPartRevision());
            if (partRevision != null && !partRevisions.contains(partRevision)) {
                if (fetchHeadOnly) {
                    if (partRevision.getLastCheckedInIteration().getKey().equals(partIterationKey)) {
                        partRevisions.add(partRevision);
                    }
                } else {
                    partRevisions.add(partRevision);
                }
            }
        }

        return new ArrayList<>(partRevisions);
    }

    private DocumentRevision getDocumentRevision(DocumentRevisionKey documentRevisionKey) {
        try {
            return documentRevisionDAO.loadDocR(documentRevisionKey);
        } catch (DocumentRevisionNotFoundException e) {
            LOGGER.log(Level.INFO, "Cannot infer document revision from key [" + documentRevisionKey + "]", e);
            return null;
        }
    }

    private PartRevision getPartRevision(PartRevisionKey partRevisionKey) {
        PartRevision partRevision = partRevisionDAO.loadPartR(partRevisionKey);

        if (partRevision == null) {
            LOGGER.log(Level.INFO, "Cannot infer part revision from key [" + partRevisionKey + "]");
        }
        return partRevision;
    }

    private DocumentIterationKey getDocumentIterationKey(Map<?, ?> source) {
        return new DocumentIterationKey(
                extractValue(source, IndexerMapping.WORKSPACE_ID_KEY),
                extractValue(source, IndexerMapping.DOCUMENT_ID_KEY),
                extractValue(source, IndexerMapping.VERSION_KEY),
                Double.valueOf(extractValue(source, IndexerMapping.ITERATION_KEY)).intValue()
        );
    }

    private  PartIterationKey getPartIterationKey(Map<?, ?> source) {
        return new PartIterationKey(
                extractValue(source, IndexerMapping.WORKSPACE_ID_KEY),
                extractValue(source, IndexerMapping.PART_NUMBER_KEY),
                extractValue(source, IndexerMapping.VERSION_KEY),
                Double.valueOf(extractValue(source, IndexerMapping.ITERATION_KEY)).intValue()
        );
    }

    private String extractValue(Map<?, ?> source, String key) {
        Object ret = source.get(key);
        if (ret instanceof List) {
            return ((List) ret).get(0).toString();
        } else {
            return ret.toString();
        }
    }

}
