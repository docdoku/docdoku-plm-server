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
import org.mockito.MockitoAnnotations;
import com.docdoku.plm.server.rest.file.*;

/**
 * @author
 */
public class FileResourceTest {


    @InjectMocks
    private FileResource resource = new FileResource();

    @Mock
    private DocumentBinaryResource documentBinaryResource;

    @Mock
    private PartBinaryResource partBinaryResource;

    @Mock
    private DocumentTemplateBinaryResource documentTemplateBinaryResource;

    @Mock
    private PartTemplateBinaryResource partTemplateBinaryResource;

    @Mock
    private ProductInstanceBinaryResource productInstanceBinaryResource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void documentFileTest() {
        DocumentBinaryResource _documentBinaryResource = resource.documentFile();
        Assert.assertEquals(documentBinaryResource, _documentBinaryResource);
    }

    @Test
    public void partFileTest() {
        PartBinaryResource _partBinaryResource = resource.partFile();
        Assert.assertEquals(partBinaryResource, _partBinaryResource);
    }

    @Test
    public void documentTemplateFileTest() {
        DocumentTemplateBinaryResource _documentTemplateBinaryResource = resource.documentTemplateFile();
        Assert.assertEquals(documentTemplateBinaryResource, _documentTemplateBinaryResource);
    }

    @Test
    public void partTemplateFileTest() {
        PartTemplateBinaryResource _partTemplateBinaryResource = resource.partTemplateFile();
        Assert.assertEquals(partTemplateBinaryResource, _partTemplateBinaryResource);
    }

    @Test
    public void productInstanceFileTest() {
        ProductInstanceBinaryResource _productInstanceBinaryResource = resource.productInstanceFile();
        Assert.assertEquals(productInstanceBinaryResource, _productInstanceBinaryResource);
    }
}
