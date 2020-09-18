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

import org.dozer.DozerBeanMapperSingletonWrapper;
import org.dozer.Mapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.meta.ListOfValues;
import com.docdoku.plm.server.core.meta.ListOfValuesKey;
import com.docdoku.plm.server.core.services.ILOVManagerLocal;
import com.docdoku.plm.server.rest.dto.ListOfValuesDTO;
import com.docdoku.plm.server.rest.dto.NameValuePairDTO;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

public class LOVResourceTest {

    @InjectMocks
    private LOVResource lovResource = new LOVResource();

    @Mock
    private ILOVManagerLocal lovManager;
    private String workspaceId = "wks";
    private Workspace workspace = new Workspace(workspaceId);


    @Before
    public void setup() throws Exception {
        initMocks(this);
        lovResource.init();
    }

    @Test
    public void getLOVsTest() throws ApplicationException {
        ListOfValues lov1 = new ListOfValues(workspace, "lov1");
        ListOfValues lov2 = new ListOfValues(workspace, "lov2");
        List<ListOfValues> lovList = Arrays.asList(lov1, lov2);
        Mockito.when(lovManager.findLOVFromWorkspace(workspaceId))
                .thenReturn(lovList);
        Response res = lovResource.getLOVs(workspaceId);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList list = (ArrayList) entity;
        Assert.assertEquals(lovList.size(), list.size());
    }

    @Test
    public void createLOVTest() throws ApplicationException, UnsupportedEncodingException {
        ListOfValuesDTO lovDTO = new ListOfValuesDTO();
        lovDTO.setName("lov");
        List<NameValuePairDTO> values = new ArrayList<>();
        NameValuePairDTO value = new NameValuePairDTO();
        value.setName("k");
        value.setValue("v");
        values.add(value);
        lovDTO.setValues(values);

        Mapper mapper = DozerBeanMapperSingletonWrapper.getInstance();
        ListOfValues lov = mapper.map(lovDTO, ListOfValues.class);
        Mockito.doNothing().when(lovManager)
                .createLov(workspaceId, lov.getName(), lov.getValues());
        Response res = lovResource.createLOV(workspaceId, lovDTO);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());
    }

    @Test
    public void getLOVTest() throws ApplicationException {
        String name = "foo";
        ListOfValuesKey lovKey = new ListOfValuesKey(workspaceId, name);
        ListOfValues lov = new ListOfValues(workspace, name);
        Mockito.when(lovManager.findLov(lovKey))
                .thenReturn(lov);
        ListOfValuesDTO result = lovResource.getLOV(workspaceId, name);
        Assert.assertEquals(lov.getName(), result.getName());
    }

    @Test
    public void updateLOVTest() throws ApplicationException {
        String name = "foo";
        ListOfValuesDTO lovDTO = new ListOfValuesDTO();
        lovDTO.setWorkspaceId(workspaceId);
        lovDTO.setName(name);
        List<NameValuePairDTO> values = new ArrayList<>();
        NameValuePairDTO value = new NameValuePairDTO();
        value.setName("k");
        value.setValue("v");
        values.add(value);
        lovDTO.setValues(values);
        Mapper mapper = DozerBeanMapperSingletonWrapper.getInstance();
        ListOfValues lov = mapper.map(lovDTO, ListOfValues.class);

        Mockito.when(lovManager.updateLov(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(lov);

        ListOfValuesDTO result = lovResource.updateLOV(workspaceId, name, lovDTO);
        Assert.assertEquals(lovDTO.getName(), result.getName());
        Assert.assertEquals(lovDTO.getValues().get(0).getName(), result.getValues().get(0).getName());
        Assert.assertEquals(lovDTO.getValues().get(0).getValue(), result.getValues().get(0).getValue());
    }

    @Test
    public void deleteLOVTest() throws ApplicationException {
        String name = "foo";
        ListOfValuesKey lovKey = new ListOfValuesKey(workspaceId, name);
        Mockito.doNothing().when(lovManager).deleteLov(lovKey);
        Response response = lovResource.deleteLOV(workspaceId, name);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

}
