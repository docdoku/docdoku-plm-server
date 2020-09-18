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

import org.elasticsearch.common.xcontent.XContentBuilder;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentMaster;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.meta.InstanceAttribute;
import com.docdoku.plm.server.core.meta.InstanceListOfValuesAttribute;
import com.docdoku.plm.server.core.meta.Tag;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.workflow.Workflow;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Util class for documents and parts to JSON mapping
 *
 * @author Morgan Guimard
 */
public class EntityMapper {

    public static void documentIterationToJSON(XContentBuilder xcb, DocumentIteration documentIteration, Map<String, String> contentInputs) throws IOException {
        DocumentRevision documentRevision = documentIteration.getDocumentRevision();
        DocumentMaster documentMaster = documentRevision.getDocumentMaster();
        User author = documentMaster.getAuthor();
        Set<Tag> tags = documentRevision.getTags();
        List<InstanceAttribute> instanceAttributes = documentIteration.getInstanceAttributes();
        Workflow workflow = documentRevision.getWorkflow();

        setField(xcb, IndexerMapping.WORKSPACE_ID_KEY, documentIteration.getWorkspaceId());
        setField(xcb, IndexerMapping.DOCUMENT_ID_KEY, documentRevision.getDocumentMasterId());
        setField(xcb, IndexerMapping.TITLE_KEY, documentIteration.getTitle());
        setField(xcb, IndexerMapping.VERSION_KEY, documentIteration.getVersion());
        setField(xcb, IndexerMapping.TYPE_KEY, documentMaster.getType());
        setField(xcb, IndexerMapping.DESCRIPTION_KEY, documentRevision.getDescription());
        setField(xcb, IndexerMapping.ITERATION_KEY, documentIteration.getIteration());
        setField(xcb, IndexerMapping.CREATION_DATE_KEY, documentRevision.getCreationDate());
        setField(xcb, IndexerMapping.MODIFICATION_DATE_KEY, documentIteration.getModificationDate());
        setField(xcb, IndexerMapping.REVISION_NOTE_KEY, documentIteration.getRevisionNote());
        setField(xcb, IndexerMapping.FOLDER_KEY, documentRevision.getLocation().getShortName());

        addCommonJSONFields(xcb, author, tags, instanceAttributes, contentInputs, workflow);
    }

    public static void partIterationToJSON(XContentBuilder xcb, PartIteration partIteration, Map<String, String> contentInputs) throws IOException {

        PartRevision partRevision = partIteration.getPartRevision();
        PartMaster partMaster = partRevision.getPartMaster();
        User author = partMaster.getAuthor();
        Set<Tag> tags = partRevision.getTags();
        List<InstanceAttribute> instanceAttributes = partIteration.getInstanceAttributes();
        Workflow workflow = partRevision.getWorkflow();

        setField(xcb, IndexerMapping.WORKSPACE_ID_KEY, partIteration.getWorkspaceId());
        setField(xcb, IndexerMapping.PART_NUMBER_KEY, partIteration.getPartNumber());
        setField(xcb, IndexerMapping.PART_NAME_KEY, partMaster.getName());
        setField(xcb, IndexerMapping.TYPE_KEY, partMaster.getType());
        setField(xcb, IndexerMapping.VERSION_KEY, partIteration.getPartVersion());
        setField(xcb, IndexerMapping.DESCRIPTION_KEY, partRevision.getDescription());
        setField(xcb, IndexerMapping.ITERATION_KEY, partIteration.getIteration());
        setField(xcb, IndexerMapping.STANDARD_PART_KEY, partMaster.isStandardPart());
        setField(xcb, IndexerMapping.CREATION_DATE_KEY, partIteration.getCreationDate());
        setField(xcb, IndexerMapping.MODIFICATION_DATE_KEY, partIteration.getModificationDate());
        setField(xcb, IndexerMapping.REVISION_NOTE_KEY, partIteration.getIterationNote());

        addCommonJSONFields(xcb, author, tags, instanceAttributes, contentInputs, workflow);
    }

    private static void addCommonJSONFields(XContentBuilder tmp, User author, Set<Tag> tags, List<InstanceAttribute> instanceAttributes,
                                            Map<String, String> contentInputs, Workflow workflow) throws IOException {
        setField(tmp, IndexerMapping.AUTHOR_LOGIN_KEY, author.getLogin());
        setField(tmp, IndexerMapping.AUTHOR_NAME_KEY, author.getName());

        if (!tags.isEmpty()) {
            tmp.startArray(IndexerMapping.TAGS_KEY);
            for (Tag tag : tags) {
                tmp.value(tag.getLabel());
            }
            tmp.endArray();
        }

        if (!instanceAttributes.isEmpty()) {
            tmp.startArray(IndexerMapping.ATTRIBUTES_KEY);
            for (InstanceAttribute attr : instanceAttributes) {
                tmp.startObject();
                setAttrField(tmp, attr);
                tmp.endObject();
            }
            tmp.endArray();
        }

        if (!contentInputs.isEmpty()) {
            tmp.startArray(IndexerMapping.FILES_KEY);
            for (Map.Entry<String, String> contentInput : contentInputs.entrySet()) {
                tmp.startObject();
                setField(tmp, IndexerMapping.FILE_NAME_KEY, contentInput.getKey());
                setField(tmp, IndexerMapping.CONTENT_KEY, contentInput.getValue());
                tmp.endObject();
            }
            tmp.endArray();
        }

        if(workflow != null) {
            setField(tmp, IndexerMapping.WORKFLOW_KEY, workflow.getFinalLifeCycleState());
        }
    }

    private static void setAttrField(XContentBuilder object, InstanceAttribute attr) throws IOException {
        setField(object, IndexerMapping.ATTRIBUTE_NAME, attr.getNameWithoutWhiteSpace());

        Object value;

        if (attr instanceof InstanceListOfValuesAttribute) {
            InstanceListOfValuesAttribute lov = (InstanceListOfValuesAttribute) attr;
            value = !lov.getItems().isEmpty() ? lov.getItems().get(lov.getIndexValue()).getName() : "";
        } else {
            value = attr.getValue();
        }

        setField(object, IndexerMapping.ATTRIBUTE_VALUE, "" + value);

    }

    private static XContentBuilder setField(XContentBuilder object, String field, Object value) throws IOException {
        if (value != null) {
            return object.field(field, value);
        }
        return null;
    }

}
