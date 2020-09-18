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

package com.docdoku.plm.server.rest.util;

import com.docdoku.plm.server.core.query.DocumentSearchQuery;
import com.docdoku.plm.server.core.query.PartSearchQuery;
import com.docdoku.plm.server.core.query.SearchQuery;
import com.docdoku.plm.server.core.util.DateUtils;

import javax.ws.rs.core.MultivaluedMap;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchQueryParser {

    private static final Logger LOGGER = Logger.getLogger(SearchQueryParser.class.getName());
    private static final String ATTRIBUTES_DELIMITER = ";";
    private static final String ATTRIBUTES_SPLITTER = ":";
    private static final String ATTRIBUTES_DELIMITER_REGEX = "(?<!\\\\)" + ATTRIBUTES_DELIMITER;
    private static final String ATTRIBUTES_SPLITTER_REGEX = "(?<!\\\\)" + ATTRIBUTES_SPLITTER;

    private SearchQueryParser() {
        super();
    }

    public static DocumentSearchQuery parseDocumentStringQuery(String workspaceId, MultivaluedMap<String, String> query) {

        String stringQuery = null;
        String pDocMId = null;
        String pTitle = null;
        String pVersion = null;
        String pAuthor = null;
        String pType = null;
        Date pCreationDateFrom = null;
        Date pCreationDateTo = null;
        Date pModificationDateFrom = null;
        Date pModificationDateTo = null;
        List<DocumentSearchQuery.AbstractAttributeQuery> pAttributes = new ArrayList<>();
        String[] pTags = null;
        String pContent = null;
        String folder = null;
        boolean fetchHeadOnly = false;


        for (String filter : query.keySet()) {
            List<String> values = query.get(filter);
            if (values.size() == 1) {
                String value = values.get(0);
                switch (filter) {
                    case "q":
                        stringQuery = value;
                        break;
                    case "id":
                        pDocMId = value;
                        break;
                    case "title":
                        pTitle = value;
                        break;
                    case "version":
                        pVersion = value;
                        break;
                    case "author":
                        pAuthor = value;
                        break;
                    case "type":
                        pType = value;
                        break;
                    case "folder":
                        folder = value;
                        break;
                    case "createdFrom":
                        try {
                            pCreationDateFrom = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "createdTo":
                        try {
                            pCreationDateTo = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "modifiedFrom":
                        try {
                            pModificationDateFrom = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "modifiedTo":
                        try {
                            pModificationDateTo = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "tags":
                        if (null != value) {
                            pTags = value.split(",");
                        }
                        break;
                    case "content":
                        pContent = value;
                        break;
                    case "attributes":
                        if (null != value) {
                            pAttributes = parseAttributeStringQuery(value);
                        }
                        break;
                    case "fetchHeadOnly":
                        fetchHeadOnly = Boolean.valueOf(value);
                        break;
                    default:
                        break;

                }
            }

        }

        DocumentSearchQuery.AbstractAttributeQuery[] pAttributesArray = pAttributes.toArray(new DocumentSearchQuery.AbstractAttributeQuery[0]);

        return new DocumentSearchQuery(workspaceId, stringQuery, pDocMId, pTitle, pVersion, pAuthor, pType,
                pCreationDateFrom, pCreationDateTo, pModificationDateFrom, pModificationDateTo,
                pAttributesArray, pTags, pContent, folder, fetchHeadOnly);

    }

    public static PartSearchQuery parsePartStringQuery(String workspaceId, MultivaluedMap<String, String> query) {
        String stringQuery = null;
        String pNumber = null;
        String pName = null;
        String pVersion = null;
        String pAuthor = null;
        String pType = null;
        Date pCreationDateFrom = null;
        Date pCreationDateTo = null;
        Date pModificationDateFrom = null;
        Date pModificationDateTo = null;
        List<PartSearchQuery.AbstractAttributeQuery> pAttributes = new ArrayList<>();
        String[] pTags = null;
        Boolean standardPart = null;
        String content = null;
        boolean fetchHeadOnly = false;

        for (String filter : query.keySet()) {
            List<String> values = query.get(filter);
            if (values.size() == 1) {
                String value = values.get(0);
                switch (filter) {
                    case "q":
                        stringQuery = value;
                        break;
                    case "number":
                        pNumber = value;
                        break;
                    case "name":
                        pName = value;
                        break;
                    case "version":
                        pVersion = value;
                        break;
                    case "author":
                        pAuthor = value;
                        break;
                    case "type":
                        pType = value;
                        break;
                    case "createdFrom":
                        try {
                            pCreationDateFrom = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "createdTo":
                        try {
                            pCreationDateTo = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "modifiedFrom":
                        try {
                            pModificationDateFrom = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "modifiedTo":
                        try {
                            pModificationDateTo = DateUtils.parse(value);
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, null, e);
                        }
                        break;
                    case "tags":
                        if (null != value) {
                            pTags = value.split(",");
                        }
                        break;
                    case "standardPart":
                        standardPart = Boolean.valueOf(value);
                        break;
                    case "content":
                        content = value;
                        break;
                    case "attributes":
                        if (null != value) {
                            pAttributes = parseAttributeStringQuery(value);
                        }
                        break;
                    case "fetchHeadOnly":
                        fetchHeadOnly = Boolean.valueOf(value);
                        break;
                }
            }
        }

        PartSearchQuery.AbstractAttributeQuery[] pAttributesArray = pAttributes.toArray(new PartSearchQuery.AbstractAttributeQuery[0]);

        return new PartSearchQuery(workspaceId, stringQuery, pNumber, pName, pVersion, pAuthor, pType,
                pCreationDateFrom, pCreationDateTo, pModificationDateFrom, pModificationDateTo,
                pAttributesArray, pTags, standardPart, content, fetchHeadOnly);

    }

    static List<SearchQuery.AbstractAttributeQuery> parseAttributeStringQuery(String attributeQuery) {
        List<SearchQuery.AbstractAttributeQuery> pAttributes = new ArrayList<>();
        String[] attributesString = attributeQuery.split(ATTRIBUTES_DELIMITER_REGEX);

        for (String attributeString : attributesString) {

            int firstColon = attributeString.indexOf(ATTRIBUTES_SPLITTER);
            String attributeType = attributeString.substring(0, firstColon);
            attributeString = attributeString.substring(firstColon + 1);


            Matcher matcher = Pattern.compile(ATTRIBUTES_SPLITTER_REGEX).matcher(attributeString);

            if (matcher.find()) {
                int secondColon = matcher.start();

                String attributeName = attributeString.substring(0, secondColon);
                String attributeValue = attributeString.substring(secondColon + 1);

                switch (attributeType) {
                    case "BOOLEAN":
                        SearchQuery.BooleanAttributeQuery baq = new SearchQuery.BooleanAttributeQuery(attributeName, Boolean.valueOf(attributeValue));
                        pAttributes.add(baq);
                        break;
                    case "DATE":
                        SearchQuery.DateAttributeQuery daq = new SearchQuery.DateAttributeQuery();
                        daq.setName(attributeName);
                        try {
                            daq.setDate(DateUtils.parse(attributeValue));
                            pAttributes.add(daq);
                        } catch (ParseException e) {
                            LOGGER.log(Level.FINEST, null, e);
                        }
                        break;
                    case "TEXT":
                        SearchQuery.TextAttributeQuery taq = new SearchQuery.TextAttributeQuery(attributeName, attributeValue);
                        pAttributes.add(taq);
                        break;
                    case "NUMBER":
                        try {
                            SearchQuery.NumberAttributeQuery naq = new SearchQuery.NumberAttributeQuery(attributeName, NumberFormat.getInstance().parse(attributeValue).floatValue());
                            pAttributes.add(naq);
                        } catch (ParseException e) {
                            LOGGER.log(Level.INFO, null, e);
                        }
                        break;
                    case "URL":
                        SearchQuery.URLAttributeQuery uaq = new SearchQuery.URLAttributeQuery(attributeName, attributeValue);
                        pAttributes.add(uaq);
                        break;

                    case "LOV":
                        SearchQuery.LovAttributeQuery laq = new SearchQuery.LovAttributeQuery(attributeName, attributeValue);
                        pAttributes.add(laq);
                        break;

                    default:
                        break;
                }

            } else {
                throw new IllegalStateException("Can't parse attribute: " + attributeQuery);
            }
        }
            return pAttributes;
    }
}
