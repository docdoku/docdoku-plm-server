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

public class IndexerMapping {

    public static final String INDEX_PREFIX = "docdoku-plm";
    public static final String INDEX_SEPARATOR = "-";
    public static final String INDEX_DOCUMENTS = "documents";
    public static final String INDEX_PARTS = "parts";
    public static final String TYPE = "_doc";

    // patterns: ["docdoku-plm-*"]
    public static final String COMMON_INDEX_NAME = INDEX_PREFIX + INDEX_SEPARATOR + "common";
    public static final String COMMON_TEMPLATE = "/com/docdoku/plm/server/indexer/common-template.json";

    // patterns: ["docdoku-plm-*-documents"]
    public static final String DOCUMENTS_INDEX_NAME = INDEX_PREFIX + INDEX_SEPARATOR + INDEX_DOCUMENTS;
    public static final String DOCUMENT_TEMPLATE = "/com/docdoku/plm/server/indexer/document-template.json";

    // patterns: ["docdoku-plm-*-parts"]
    public static final String PARTS_INDEX_NAME = INDEX_PREFIX + INDEX_SEPARATOR + INDEX_PARTS;
    public static final String PART_TEMPLATE = "/com/docdoku/plm/server/indexer/part-template.json";

    public static final String WORKSPACE_ID_KEY = "workspaceId";
    public static final String ITERATION_KEY = "iteration";
    public static final String VERSION_KEY = "version";
    public static final String AUTHOR_LOGIN_KEY = "authorLogin";
    public static final String AUTHOR_NAME_KEY = "authorName";
    public static final String FILE_NAME_KEY = "fileName";
    public static final String CREATION_DATE_KEY = "creationDate";
    public static final String MODIFICATION_DATE_KEY = "modificationDate";
    public static final String TYPE_KEY = "type";
    public static final String DOCUMENT_ID_KEY = "docMId";
    public static final String PART_NUMBER_KEY = "partNumber";
    public static final String PART_NAME_KEY = "partName";
    public static final String TITLE_KEY = "title";
    public static final String DESCRIPTION_KEY = "description";
    public static final String REVISION_NOTE_KEY = "revisionNote";
    public static final String WORKFLOW_KEY = "workflow";
    public static final String FOLDER_KEY = "folder";
    public static final String TAGS_KEY = "tags";
    public static final String ATTRIBUTES_KEY = "attributes";
    public static final String ATTRIBUTE_NAME = "attr_name";
    public static final String ATTRIBUTE_VALUE = "attr_value";
    public static final String FILES_KEY = "files";
    public static final String CONTENT_KEY = "content";
    public static final String STANDARD_PART_KEY = "standardPart";

}
