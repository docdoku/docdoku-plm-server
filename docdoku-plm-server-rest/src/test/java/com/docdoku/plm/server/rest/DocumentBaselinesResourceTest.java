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

package com.docdoku.plm.server.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.configuration.DocumentBaseline;
import com.docdoku.plm.server.core.configuration.DocumentBaselineType;
import com.docdoku.plm.server.core.configuration.DocumentCollection;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.services.IDocumentBaselineManagerLocal;
import com.docdoku.plm.server.rest.dto.baseline.BaselinedDocumentDTO;
import com.docdoku.plm.server.rest.dto.baseline.DocumentBaselineDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.MockitoAnnotations.initMocks;

public class DocumentBaselinesResourceTest {

    @InjectMocks
    private DocumentBaselinesResource documentBaselinesResource = new DocumentBaselinesResource();

    @Mock
    private IDocumentBaselineManagerLocal documentBaselineService;

    private String workspaceId = "wks";

    @Before
    public void setup() throws Exception {
        initMocks(this);
        documentBaselinesResource.init();
    }

    @Test
    public void getDocumentBaselinesTest() throws ApplicationException {
        DocumentBaseline baseline1 = new DocumentBaseline();
        DocumentBaseline baseline2 = new DocumentBaseline();
        List<DocumentBaseline> list = Arrays.asList(baseline1, baseline2);
        Mockito.when(documentBaselineService.getBaselines(workspaceId))
                .thenReturn(list);
        Response res = documentBaselinesResource.getDocumentBaselines(workspaceId);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList result = (ArrayList) entity;
        Assert.assertEquals(list.size(), result.size());
    }

    @Test
    public void createDocumentBaselineTest() throws ApplicationException {
        DocumentBaseline baseline = new DocumentBaseline();
        baseline.setId(42);
        DocumentBaselineDTO documentBaselineDTO = new DocumentBaselineDTO();
        documentBaselineDTO.setName("foo");
        documentBaselineDTO.setType(DocumentBaselineType.LATEST);
        BaselinedDocumentDTO doc = new BaselinedDocumentDTO();
        doc.setDocumentMasterId("blah");
        doc.setVersion("A");
        doc.setIteration(1);
        List<BaselinedDocumentDTO> docs = Collections.singletonList(doc);
        documentBaselineDTO.setBaselinedDocuments(docs);
        List<BaselinedDocumentDTO> baselinedDocumentsDTO = documentBaselineDTO.getBaselinedDocuments();
        List<DocumentRevisionKey> documentRevisionKeys = baselinedDocumentsDTO.stream()
                .map(document -> new DocumentRevisionKey(workspaceId, document.getDocumentMasterId(), document.getVersion()))
                .collect(Collectors.toList());
        DocumentCollection docCollection = new DocumentCollection();

        Mockito.when(documentBaselineService.createBaseline(workspaceId, documentBaselineDTO.getName(),
                documentBaselineDTO.getType(), documentBaselineDTO.getDescription(), documentRevisionKeys))
                .thenReturn(baseline);

        Mockito.when(documentBaselineService.getBaselineLight(workspaceId, baseline.getId()))
                .thenReturn(baseline);

        Mockito.when(documentBaselineService.getACLFilteredDocumentCollection(workspaceId, baseline.getId()))
                .thenReturn(docCollection);


        Response res = documentBaselinesResource.createDocumentBaseline(workspaceId, documentBaselineDTO);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());
    }

    @Test
    public void deleteBaselineTest() throws ApplicationException {
        int baselineId = 42;
        Mockito.doNothing().when(documentBaselineService).deleteBaseline(workspaceId, baselineId);
        Response res = documentBaselinesResource.deleteBaseline(workspaceId, baselineId);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
    }

    @Test
    public void getBaselineTest() throws ApplicationException {
        DocumentBaseline baseline = new DocumentBaseline();
        baseline.setId(42);
        DocumentBaselineDTO documentBaselineDTO = new DocumentBaselineDTO();
        documentBaselineDTO.setName("foo");
        documentBaselineDTO.setType(DocumentBaselineType.LATEST);
        BaselinedDocumentDTO doc = new BaselinedDocumentDTO();
        doc.setDocumentMasterId("blah");
        doc.setVersion("A");
        doc.setIteration(1);
        List<BaselinedDocumentDTO> docs = Collections.singletonList(doc);
        documentBaselineDTO.setBaselinedDocuments(docs);

        DocumentCollection docCollection = new DocumentCollection();

        Mockito.when(documentBaselineService.getBaselineLight(workspaceId, baseline.getId()))
                .thenReturn(baseline);

        Mockito.when(documentBaselineService.getACLFilteredDocumentCollection(workspaceId, baseline.getId()))
                .thenReturn(docCollection);
        DocumentBaselineDTO result = documentBaselinesResource.getBaseline(workspaceId, baseline.getId());
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), baseline.getId());


    }

    @Test
    public void getBaselineLightTest() throws ApplicationException {
        DocumentBaseline baseline = new DocumentBaseline();
        baseline.setId(42);
        Mockito.when(documentBaselineService.getBaselineLight(workspaceId, baseline.getId()))
                .thenReturn(baseline);
        DocumentBaselineDTO baselineLight = documentBaselinesResource.getBaselineLight(workspaceId, baseline.getId());
        Assert.assertNotNull(baselineLight);

    }

    @Test
    public void exportDocumentFilesTest() throws ApplicationException {
        int baselineId = 42;
        DocumentBaseline baseline = new DocumentBaseline();
        baseline.setId(baselineId);
        Mockito.when(documentBaselineService.getBaselineLight(workspaceId, baselineId))
                .thenReturn(baseline);
        Response res = documentBaselinesResource.exportDocumentFiles(workspaceId, baselineId);
        Assert.assertNotNull(res.getEntity());
        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
    }

}
