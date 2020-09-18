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
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.configuration.BaselinedPart;
import com.docdoku.plm.server.core.configuration.PartCollection;
import com.docdoku.plm.server.core.configuration.ProductBaseline;
import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.services.IProductBaselineManagerLocal;
import com.docdoku.plm.server.core.services.IProductManagerLocal;
import com.docdoku.plm.server.rest.dto.baseline.ProductBaselineCreationDTO;
import com.docdoku.plm.server.rest.dto.baseline.ProductBaselineDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.times;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProductBaselinesResourceTest {

    @InjectMocks
    private ProductBaselinesResource productBaselineResource = new ProductBaselinesResource();

    @Mock
    private IProductManagerLocal productService;

    @Mock
    private IProductBaselineManagerLocal productBaselineService;

    private String workspaceId = "wks";
    private String name = "name";
    private String description = "description";
    private String ciId = "ciId";
    private Workspace workspace = new Workspace(workspaceId);
    private ConfigurationItemKey configurationItemKey = new ConfigurationItemKey(workspaceId, ciId);
    private Account account = new Account("foo");
    private User author = new User(workspace, account);
    private PartMaster partMaster = new PartMaster();
    private PartRevision partRevision = new PartRevision(partMaster,author);
    private PartIteration partIteration = new PartIteration(partRevision, author);
    private ConfigurationItem configurationItem = new ConfigurationItem(author, workspace, ciId, description);
    private ProductBaseline baseline = new ProductBaseline(author, configurationItem, name, ProductBaselineType.LATEST, description);
    private List<ProductBaseline> baselines = Collections.singletonList(baseline);
    private BaselinedPart baselinedPart = new BaselinedPart(new PartCollection(), partIteration);
    private List<BaselinedPart> baselinedPartList = Collections.singletonList(baselinedPart);
    private List<String> pathToPathLinkTypes = Collections.singletonList("foo");
    private PathToPathLink p2pLink = new PathToPathLink("type","source","target","description");
    private List<PathToPathLink> pathToPathLinks = Collections.singletonList(p2pLink);
    private String path = "path";

    @Before
    public void setup() throws Exception {
        initMocks(this);
        productBaselineResource.init();
        baseline.setId(1);
        baseline.addSubstituteLink(path);
        baseline.addOptionalUsageLink(path);
        baseline.setPathToPathLinks(pathToPathLinks);
        configurationItem.setDesignItem(partMaster);
        partMaster.setPartRevisions(Collections.singletonList(partRevision));
        partRevision.setPartIterations(Collections.singletonList(partIteration));
    }

    @Test
    public void getAllProductBaselinesTest() throws ApplicationException {

        Mockito.when(productBaselineService.getAllBaselines(workspaceId))
                .thenReturn(baselines);

        Response response = productBaselineResource.getAllProductBaselines(workspaceId);

        Object body = response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertTrue(body.getClass().isAssignableFrom(ArrayList.class));
        List objects = (List) body;
        Assert.assertFalse(objects.isEmpty());
        Object o = objects.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(ProductBaselineDTO.class));
        ProductBaselineDTO dto = (ProductBaselineDTO) o;
        Assert.assertEquals(baseline.getName(), dto.getName());
    }

    @Test
    public void getProductBaselinesForProductTest() throws ApplicationException {

        Mockito.when(productBaselineService.getBaselines(configurationItemKey))
                .thenReturn(baselines);

        Response response = productBaselineResource.getProductBaselinesForProduct(workspaceId, ciId);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Mockito.verify(productBaselineService, times(1)).getBaselines(configurationItemKey);
    }

    @Test
    public void createProductBaselineTest() throws ApplicationException {

        ProductBaselineCreationDTO dto = new ProductBaselineCreationDTO();
        dto.setConfigurationItemId(ciId);
        dto.setBaselinedParts(new ArrayList<>());
        dto.setType(ProductBaselineType.LATEST);
        dto.setName(name);
        dto.setDescription(description);
        List<PartIterationKey> partIterationKeys = dto.getBaselinedParts().stream()
                .map(part -> new PartIterationKey(workspaceId, part.getNumber(), part.getVersion(), part.getIteration())).collect(Collectors.toList());


        Mockito.when(productBaselineService.createBaseline(configurationItemKey, name, ProductBaselineType.LATEST, description, partIterationKeys,
                dto.getSubstituteLinks(), dto.getOptionalUsageLinks(),
                null, null, null, true))
                .thenReturn(baseline);

        ProductBaselineDTO productBaseline = productBaselineResource.createProductBaseline(workspaceId, dto, true);

        Mockito.verify(productBaselineService, times(0)).getObsoletePartRevisionsInBaseline(workspaceId, baseline.getId());
        Assert.assertNotNull(productBaseline);


        Mockito.when(productBaselineService.createBaseline(configurationItemKey, name, ProductBaselineType.LATEST, description, partIterationKeys,
                dto.getSubstituteLinks(), dto.getOptionalUsageLinks(),
                null, null, null, false))
                .thenReturn(baseline);

        productBaselineResource.createProductBaseline(workspaceId, dto, false);

        Mockito.verify(productBaselineService, times(1)).getObsoletePartRevisionsInBaseline(workspaceId, baseline.getId());

    }


    @Test
    public void deleteProductBaselineTest() throws ApplicationException {
        Response response = productBaselineResource.deleteProductBaseline(workspaceId, ciId, baseline.getId());
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        Mockito.verify(productBaselineService, times(1)).deleteBaseline(workspaceId, baseline.getId());
    }


    @Test
    public void getProductBaselineTest() throws ApplicationException {

        PartUsageLink partLink = new PartUsageLink();
        List<PartLink> partLinks = Collections.singletonList(partLink);
        partLink.setComponent(partMaster);

        Mockito.when(productBaselineService.getBaseline(baseline.getId()))
               .thenReturn(baseline);

        Mockito.when(productService.decodePath(configurationItemKey, path))
                .thenReturn(partLinks);

        PartUsageLink link = new PartUsageLink();
        link.setComponent(partMaster);
        List<PartLink> sourcePath = Collections.singletonList(link);
        Mockito.when(productService.decodePath(configurationItemKey, p2pLink.getSourcePath()))
                .thenReturn(sourcePath);

        List<PartLink> targetPath = Collections.singletonList(link);
        Mockito.when(productService.decodePath(configurationItemKey, p2pLink.getTargetPath()))
                .thenReturn(targetPath);

        ProductBaselineDTO productBaseline = productBaselineResource.getProductBaseline(workspaceId, ciId, baseline.getId());
        Assert.assertNotNull(productBaseline);
    }


    @Test
    public void getProductBaselinePartsTest() throws ApplicationException {
        String q = "query";
        int maxResults = 8;

        Mockito.when(productBaselineService.getBaselinedPartWithReference(baseline.getId(), q, maxResults))
                .thenReturn(baselinedPartList);

        Response response = productBaselineResource.getProductBaselineParts(workspaceId, ciId, baseline.getId(), q);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }


    @Test
    public void getPathToPathLinkTypesInBaselineTest() throws ApplicationException {
        Mockito.when(productBaselineService.getPathToPathLinkTypes(workspaceId, ciId, baseline.getId()))
                .thenReturn(pathToPathLinkTypes);

        Response response = productBaselineResource.getPathToPathLinkTypesInBaseline(workspaceId, ciId, baseline.getId());
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPathToPathLinkInProductBaselineTest () throws ApplicationException {
        String sourcePathAsString = "source";
        String targetPathAsString = "dest";

        Mockito.when(productBaselineService.getPathToPathLinkFromSourceAndTarget(workspaceId, ciId, baseline.getId(),
                sourcePathAsString, targetPathAsString))
                .thenReturn(pathToPathLinks);

        PartUsageLink link = new PartUsageLink();
        link.setComponent(partMaster);
        List<PartLink> sourcePath = Collections.singletonList(link);
        Mockito.when(productService.decodePath(configurationItemKey, p2pLink.getSourcePath()))
            .thenReturn(sourcePath);

        List<PartLink> targetPath = Collections.singletonList(link);
        Mockito.when(productService.decodePath(configurationItemKey, p2pLink.getTargetPath()))
            .thenReturn(targetPath);

        Response response = productBaselineResource.getPathToPathLinkInProductBaseline(workspaceId, ciId, baseline.getId(), sourcePathAsString, targetPathAsString);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }



}
