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
import com.docdoku.plm.server.core.configuration.ProductConfiguration;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.security.ACLPermission;
import com.docdoku.plm.server.core.services.IProductBaselineManagerLocal;
import com.docdoku.plm.server.core.services.IProductManagerLocal;
import com.docdoku.plm.server.rest.dto.ACLDTO;
import com.docdoku.plm.server.rest.dto.ACLEntryDTO;
import com.docdoku.plm.server.rest.dto.baseline.ProductConfigurationDTO;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.mockito.MockitoAnnotations.initMocks;

public class ProductConfigurationResourceTest {

    @InjectMocks
    private ProductConfigurationsResource productConfigurationsResource = new ProductConfigurationsResource();

    @Mock
    private IProductBaselineManagerLocal productBaselineService;

    @Mock
    private IProductManagerLocal productService;
    private String workspaceId = "wks";
    private String ciId = "SomeProductId";
    private ConfigurationItemKey ciKey = new ConfigurationItemKey(workspaceId, ciId);

    @Before
    public void setup() throws Exception {
        initMocks(this);
        productConfigurationsResource.init();
    }

    @Test
    public void getAllConfigurationsTest() throws ApplicationException {
        List<ProductConfiguration> configList = new ArrayList<>();
        ProductConfiguration config1 = new ProductConfiguration();
        ProductConfiguration config2 = new ProductConfiguration();
        ConfigurationItem ci  = new ConfigurationItem();
        ci.setId(ciId);
        config1.setName("c1");
        config2.setName("c2");
        config1.setConfigurationItem(ci);
        config2.setConfigurationItem(ci);
        configList.add(0, config1);
        configList.add(1, config2);
        Mockito.when(productBaselineService.getAllProductConfigurations(workspaceId))
                .thenReturn(configList);

        Response res = productConfigurationsResource.getAllConfigurations(workspaceId);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        List entities = (ArrayList) entity;
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void getConfigurationsForProductTest() throws ApplicationException {

        List<ProductConfiguration> configList = new ArrayList<>();
        ProductConfiguration config1 = new ProductConfiguration();
        ProductConfiguration config2 = new ProductConfiguration();
        ConfigurationItem ci  = new ConfigurationItem();
        ci.setId(ciId);
        config1.setName("c1");
        config2.setName("c2");
        config1.setConfigurationItem(ci);
        config2.setConfigurationItem(ci);
        configList.add(0, config1);
        configList.add(1, config2);


        Mockito.when(productBaselineService.getAllProductConfigurationsByConfigurationItemId(ciKey))
                .thenReturn(configList);
        Response res = productConfigurationsResource.getConfigurationsForProduct(workspaceId, ciId);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        List entities = (ArrayList) entity;
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void getConfigurationTest() throws ApplicationException {
        int pcId = 1;
        ProductConfiguration conf = new ProductConfiguration();
        ConfigurationItem ci  = new ConfigurationItem();
        ci.setId(ciId);
        conf.setName("c1");
        conf.setId(pcId);
        conf.setConfigurationItem(ci);

        Set<String> substitutes = new HashSet<>();
        String substitutePath = "something";
        substitutes.add(substitutePath);
        conf.setSubstituteLinks(substitutes);

        Set<String> optionals = new HashSet<>();
        String optionalPath = "something";
        optionals.add(optionalPath);
        conf.setOptionalUsageLinks(optionals);


        Mockito.when(productBaselineService.getProductConfiguration(ciKey, pcId))
                .thenReturn(conf);

        PartMaster component = new PartMaster();
        component.setNumber("foo");

        List<PartLink> partLinks = Collections.singletonList(new PartUsageLink(component, 1, null, false ));
        List<PartLink> optionalLinks = Collections.singletonList(new PartUsageLink(component, 1, null, false ));

        Mockito.when(productService.decodePath(ciKey, substitutePath))
                .thenReturn(partLinks);

        Mockito.when(productService.decodePath(ciKey, substitutePath))
                .thenReturn(optionalLinks);

        ProductConfigurationDTO productConfigurationDTO = productConfigurationsResource.getConfiguration(workspaceId, ciId, pcId);
        Assert.assertNotNull(productConfigurationDTO);
        Assert.assertEquals(conf.getName(), productConfigurationDTO.getName());

    }

    @Test
    public void createConfigurationTest() throws ApplicationException {
        String name = "name";
        String description = "description";
        ProductConfigurationDTO pProductConfigurationDTO = new ProductConfigurationDTO();
        pProductConfigurationDTO.setId(1);
        pProductConfigurationDTO.setName(name);
        pProductConfigurationDTO.setDescription(description);
        pProductConfigurationDTO.setConfigurationItemId(ciId);
        ACLDTO acl = new ACLDTO();
        acl.setUserEntries(new ArrayList<>());
        acl.setGroupEntries(new ArrayList<>());
        pProductConfigurationDTO.setAcl(acl);


        Map<String, String> userEntries = new HashMap<>();
        Map<String, String> userGroupEntries = new HashMap<>();
        ProductConfiguration conf = new ProductConfiguration();
        ConfigurationItem configurationItem = new ConfigurationItem();
        configurationItem.setId(ciId);
        conf.setConfigurationItem(configurationItem);

        Mockito.when(productBaselineService.createProductConfiguration(ciKey, name, description,
                pProductConfigurationDTO.getSubstituteLinks(), pProductConfigurationDTO.getOptionalUsageLinks(),
                userEntries, userGroupEntries)).thenReturn(conf);

        ProductConfigurationDTO configurationDTO = productConfigurationsResource.createConfiguration(workspaceId, pProductConfigurationDTO);
        Assert.assertNotNull(configurationDTO);
    }

    @Test
    public void updateConfigurationACLTest() throws ApplicationException {
        ACLDTO acl = new ACLDTO();
        List<ACLEntryDTO> userEntries = new ArrayList<>();
        List<ACLEntryDTO> userGroupEntries = new ArrayList<>();
        acl.setUserEntries(userEntries);
        acl.setGroupEntries(userGroupEntries);

        int productConfigurationId = 1;

        Mockito.doNothing().when(productBaselineService)
                .updateACLForConfiguration(ciKey, productConfigurationId, acl.getUserEntriesMap(), acl.getUserGroupEntriesMap());

        Mockito.doNothing().when(productBaselineService)
                .removeACLFromConfiguration(ciKey, productConfigurationId);

        Response res = productConfigurationsResource.updateConfigurationACL(workspaceId, ciId, productConfigurationId, acl);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
        Assert.assertNull(res.getEntity());

        ACLEntryDTO entry = new ACLEntryDTO();
        entry.setKey("key");
        entry.setValue(ACLPermission.FULL_ACCESS);
        userEntries.add(entry);
        res = productConfigurationsResource.updateConfigurationACL(workspaceId, ciId, productConfigurationId, acl);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), res.getStatus());
        Assert.assertNull(res.getEntity());
    }

    @Test
    public void deleteProductConfigurationTest() throws ApplicationException {
        int productConfigurationId = 1;
        Mockito.doNothing().when(productBaselineService).deleteProductConfiguration(ciKey, productConfigurationId);
        Response response = productConfigurationsResource.deleteProductConfiguration(workspaceId, ciId, productConfigurationId);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

}
