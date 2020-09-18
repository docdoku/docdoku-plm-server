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

package com.docdoku.plm.server.indexer.util;

import com.docdoku.plm.server.core.util.Tools;
import com.docdoku.plm.server.config.IndexerConfig;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for Search & Index operations using Elasticsearch API.
 *
 * @author Taylor Labejof
 */

@Stateless(name = "IndicesUtils")
public class IndicesUtils{

    @Inject
    IndexerConfig config;

    private static final Logger LOGGER = Logger.getLogger(IndicesUtils.class.getName());

    public String getIndexName(String indexName, String type){
        return config.getPrefixIndex() + IndexerMapping.INDEX_SEPARATOR +
               IndexerMapping.INDEX_PREFIX + IndexerMapping.INDEX_SEPARATOR +
                formatIndexName(indexName) + IndexerMapping. INDEX_SEPARATOR + type;
    }

    /**
     * Convert the workspaceId to a Elastic Search index name
     *
     * @param workspaceId Id to convert
     * @return The workspaceId without uppercase and space
     */
    private String formatIndexName(String workspaceId) {
        try {
            return URLEncoder.encode(Tools.unAccent(workspaceId), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.FINEST, null, e);
            return null;
        }
    }

    /**
     * Convert the workspaceId to a Elastic Search index name
     *
     * @param id document Id to convert
     * @return The document id without uppercase and space
     */
    public String formatDocId(String id){
        try {
            return URLEncoder.encode(Tools.unAccent(id), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.FINEST, null, e);
            return null;
        }
    }
}
