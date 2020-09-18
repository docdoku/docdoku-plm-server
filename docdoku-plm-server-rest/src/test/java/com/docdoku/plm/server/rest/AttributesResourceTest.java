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
import org.mockito.MockitoAnnotations;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.meta.InstanceAttribute;
import com.docdoku.plm.server.core.meta.InstanceTextAttribute;
import com.docdoku.plm.server.core.services.IProductManagerLocal;
import com.docdoku.plm.server.rest.dto.InstanceAttributeDTO;
import com.docdoku.plm.server.rest.dto.InstanceAttributeType;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class AttributesResourceTest {

    @InjectMocks
    private AttributesResource attributesResource = new AttributesResource();

    @Mock
    private IProductManagerLocal productManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        attributesResource.init();
    }

    @Test
    public void getPartIterationsAttributesTest() throws ApplicationException {
        String workspaceId = "wks";

        List<InstanceAttribute> list = new ArrayList<>();
        InstanceAttribute attribute = new InstanceTextAttribute("name","value",false);
        list.add(attribute);
        list.add(null);
        Mockito.when(productManager.getPartIterationsInstanceAttributesInWorkspace(workspaceId))
                .thenReturn(list);

        Response res = attributesResource.getPartIterationsAttributes(workspaceId);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));

        ArrayList result = (ArrayList) entity;
        Assert.assertEquals(1, result.size());

        Object o = result.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(InstanceAttributeDTO.class));
        InstanceAttributeDTO attr = (InstanceAttributeDTO) o;
        Assert.assertEquals(attribute.getName(), attr.getName());
        Assert.assertEquals(null, attr.getValue());
        Assert.assertEquals(false, attr.isLocked());
        Assert.assertEquals(false, attr.isMandatory());
        Assert.assertEquals(InstanceAttributeType.TEXT, attr.getType());

    }

    @Test
    public void getPathDataAttributesTest() throws ApplicationException{
        String workspaceId = "wks";

        List<InstanceAttribute> list = new ArrayList<>();
        InstanceAttribute attribute = new InstanceTextAttribute("name","value",false);
        list.add(attribute);
        list.add(null);
        Mockito.when(productManager.getPathDataInstanceAttributesInWorkspace(workspaceId))
                .thenReturn(list);

        Response res = attributesResource.getPathDataAttributes(workspaceId);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));

        ArrayList result = (ArrayList) entity;
        Assert.assertEquals(1, result.size());

        Object o = result.get(0);
        Assert.assertTrue(o.getClass().isAssignableFrom(InstanceAttributeDTO.class));
        InstanceAttributeDTO attr = (InstanceAttributeDTO) o;
        Assert.assertEquals(attribute.getName(), attr.getName());
        Assert.assertEquals(null, attr.getValue());
        Assert.assertEquals(false, attr.isLocked());
        Assert.assertEquals(false, attr.isMandatory());
        Assert.assertEquals(InstanceAttributeType.TEXT, attr.getType());
    }
}
