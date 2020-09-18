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

package com.docdoku.plm.server.products;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.configuration.ProductBaseline;
import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.ConfigurationItem;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PathToPathLink;
import com.docdoku.plm.server.ProductManagerBean;
import com.docdoku.plm.server.UserManagerBean;
import com.docdoku.plm.server.configuration.PSFilterVisitor;
import com.docdoku.plm.server.dao.*;
import com.docdoku.plm.server.util.BaselineRule;

import javax.ejb.SessionContext;
import javax.persistence.TypedQuery;
import java.security.Principal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class ProductBaselineManagerBeanTest {

    @InjectMocks
    private ProductBaselineManagerBean productBaselineService = new ProductBaselineManagerBean();
    @Mock
    private SessionContext ctx;
    @Mock
    private Principal principal;
    @Mock
    private UserManagerBean userManager;
    @Mock
    private ConfigurationItemDAO configurationItemDAO;
    @Mock
    private PartIterationDAO partIterationDAO;
    @Mock
    private ProductManagerBean productService;
    @Mock
    private PartCollectionDAO partCollectionDAO;
    @Mock
    private DocumentCollectionDAO documentCollectionDAO;
    @Mock
    private ProductBaselineDAO productBaselineDAO;
    @Mock
    private PathToPathLinkDAO pathToPathLinkDAO;

    @Mock
    private PSFilterVisitor psFilterVisitor;

    @Rule
    public BaselineRule baselineRuleNotReleased;
    @Rule
    public BaselineRule baselineRuleReleased;
    @Rule
    public BaselineRule baselineRuleLatest;
    @Mock
    private TypedQuery<PathToPathLink> mockedQuery;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        //Mockito.when(ctx.getCallerPrincipal()).thenReturn(principal);
        //Mockito.when(principal.getName()).thenReturn("user1");
    }

    @Test
    public void createReleasedBaseline() throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, NotAllowedException, UserNotActiveException, PartIterationNotFoundException, PartRevisionNotReleasedException, EntityConstraintException, PartMasterNotFoundException, CreationException, BaselineNotFoundException, PathToPathLinkAlreadyExistsException, WorkspaceNotEnabledException {

        //Given
        baselineRuleReleased = new BaselineRule("myBaseline", ProductBaselineType.RELEASED, "description", "workspace01", "user1", "part01", "product01", true);
        doReturn(new User()).when(userManager).checkWorkspaceWriteAccess(anyString());
        Mockito.when(userManager.checkWorkspaceWriteAccess(anyString())).thenReturn(baselineRuleReleased.getUser());

        //Mockito.when(productService.getRootPartUsageLink(Matchers.any())).thenReturn(baselineRuleReleased.getRootPartUsageLink());
        //Mockito.when(mockedQuery.setParameter(Matchers.anyString(), Matchers.any())).thenReturn(mockedQuery);

        // Create Mock ConfigurationItem
        ConfigurationItem configurationItem = new ConfigurationItem(baselineRuleReleased.getUser(), baselineRuleReleased.getWorkspace(), baselineRuleReleased.getConfigurationItemKey().getId(), "description");
        configurationItem.setDesignItem(baselineRuleReleased.getPartMaster());
        Mockito.when(configurationItemDAO.loadConfigurationItem(baselineRuleReleased.getConfigurationItemKey())).thenReturn(configurationItem);

        //When
        ProductBaseline baseline = productBaselineService.createBaseline(baselineRuleReleased.getConfigurationItemKey(), baselineRuleReleased.getName(), baselineRuleReleased.getType(), baselineRuleReleased.getDescription(), new ArrayList<>(), baselineRuleReleased.getSubstituteLinks(), baselineRuleReleased.getOptionalUsageLinks(), null, null, null, false);

        //Then
        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getDescription(), baselineRuleReleased.getDescription());
        Assert.assertEquals(baseline.getType(), baselineRuleReleased.getType());
        Assert.assertEquals(baseline.getConfigurationItem().getWorkspaceId(), baselineRuleReleased.getWorkspace().getId());

    }

    @Test(expected = NotAllowedException.class)
    public void createReleasedBaselineUsingPartNotReleased() throws Exception{

        //Given
        baselineRuleNotReleased = new BaselineRule("myBaseline", ProductBaselineType.RELEASED, "description", "workspace01", "user1", "part01", "product01", false);

        doReturn(new User()).when(userManager).checkWorkspaceWriteAccess(anyString());
        Mockito.when(userManager.checkWorkspaceWriteAccess(anyString())).thenReturn(baselineRuleNotReleased.getUser());

        // Create Mock ConfigurationItem
        ConfigurationItem configurationItem = new ConfigurationItem(baselineRuleNotReleased.getUser(), baselineRuleNotReleased.getWorkspace(), baselineRuleNotReleased.getConfigurationItemKey().getId(), "description");
        configurationItem.setDesignItem(baselineRuleNotReleased.getPartMaster());
        Mockito.doThrow(NotAllowedException.class).when(psFilterVisitor).visit(any(), any(), any(PartMaster.class), anyInt(), any());
        Mockito.when(configurationItemDAO.loadConfigurationItem(baselineRuleNotReleased.getConfigurationItemKey())).thenReturn(configurationItem);

        //When
        productBaselineService.createBaseline(baselineRuleNotReleased.getConfigurationItemKey(), baselineRuleNotReleased.getName(), baselineRuleNotReleased.getType(), baselineRuleNotReleased.getDescription(),new ArrayList<>(), baselineRuleNotReleased.getSubstituteLinks(), baselineRuleNotReleased.getOptionalUsageLinks(), null, null, null, false);

    }

    @Test
    public void createLatestBaseline() throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, EntityConstraintException, UserNotActiveException, NotAllowedException, PartIterationNotFoundException, PartRevisionNotReleasedException, PartMasterNotFoundException, CreationException, BaselineNotFoundException, PathToPathLinkAlreadyExistsException, WorkspaceNotEnabledException {

        //Given
        baselineRuleLatest = new BaselineRule("myBaseline", ProductBaselineType.LATEST, "description", "workspace01", "user1", "part01", "product01", true);
        doReturn(new User()).when(userManager).checkWorkspaceWriteAccess(anyString());
        Mockito.when(userManager.checkWorkspaceWriteAccess(anyString())).thenReturn(baselineRuleLatest.getUser());

        //Mockito.when(productService.getRootPartUsageLink(Matchers.any())).thenReturn(baselineRuleLatest.getRootPartUsageLink());
        //Mockito.when(mockedQuery.setParameter(Matchers.anyString(), Matchers.any())).thenReturn(mockedQuery);

        // Create Mock ConfigurationItem
        ConfigurationItem configurationItem = new ConfigurationItem(baselineRuleLatest.getUser(), baselineRuleLatest.getWorkspace(), baselineRuleLatest.getConfigurationItemKey().getId(), "description");
        configurationItem.setDesignItem(baselineRuleLatest.getPartMaster());
        Mockito.when(configurationItemDAO.loadConfigurationItem(baselineRuleLatest.getConfigurationItemKey())).thenReturn(configurationItem);

        //When
        ProductBaseline baseline = productBaselineService.createBaseline(baselineRuleLatest.getConfigurationItemKey(), baselineRuleLatest.getName(), baselineRuleLatest.getType(), baselineRuleLatest.getDescription(), new ArrayList<>(), baselineRuleLatest.getSubstituteLinks(), baselineRuleLatest.getOptionalUsageLinks(), null, null, null, false);

        //Then
        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getDescription(), baselineRuleLatest.getDescription());
        Assert.assertEquals(baseline.getType(), baselineRuleLatest.getType());
        Assert.assertEquals(baseline.getConfigurationItem().getWorkspaceId(), baselineRuleLatest.getWorkspace().getId());

    }

    @Test
    public void createLatestBaselineWithCheckedPart() throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, NotAllowedException, UserNotActiveException, PartIterationNotFoundException, PartRevisionNotReleasedException, EntityConstraintException, PartMasterNotFoundException, CreationException, BaselineNotFoundException, PathToPathLinkAlreadyExistsException, WorkspaceNotEnabledException {

        //Given
        baselineRuleReleased = new BaselineRule("myBaseline", ProductBaselineType.LATEST , "description", "workspace01", "user1", "part01", "product01", true, false);
        doReturn(new User()).when(userManager).checkWorkspaceWriteAccess(anyString());
        Mockito.when(userManager.checkWorkspaceWriteAccess(anyString())).thenReturn(baselineRuleReleased.getUser());
        //Mockito.when(partIterationDAO.loadPartI(baselineRuleReleased.getPartMaster().getLastReleasedRevision().getIteration(1).getKey())).thenReturn(baselineRuleReleased.getPartMaster().getLastReleasedRevision().getIteration(1));
        //Mockito.when(productService.getRootPartUsageLink(Matchers.any())).thenReturn(baselineRuleReleased.getRootPartUsageLink());
        //Mockito.when(mockedQuery.setParameter(Matchers.anyString(), Matchers.any())).thenReturn(mockedQuery);

        // Create Mock ConfigurationItem
        ConfigurationItem configurationItem = new ConfigurationItem(baselineRuleReleased.getUser(), baselineRuleReleased.getWorkspace(), baselineRuleReleased.getConfigurationItemKey().getId(), "description");
        configurationItem.setDesignItem(baselineRuleReleased.getPartMaster());
        Mockito.when(configurationItemDAO.loadConfigurationItem(baselineRuleReleased.getConfigurationItemKey())).thenReturn(configurationItem);

        //When
        ProductBaseline baseline = productBaselineService.createBaseline(baselineRuleReleased.getConfigurationItemKey(), baselineRuleReleased.getName(), baselineRuleReleased.getType(), baselineRuleReleased.getDescription(), new ArrayList<>(), baselineRuleReleased.getSubstituteLinks(), baselineRuleReleased.getOptionalUsageLinks(), null, null, null, false);

        //Then
        Assert.assertNotNull(baseline);
        Assert.assertEquals(baselineRuleReleased.getDescription(), baseline.getDescription());
        Assert.assertEquals(ProductBaselineType.LATEST, baseline.getType());
        Assert.assertEquals(baselineRuleReleased.getWorkspace().getId(), baseline.getConfigurationItem().getWorkspaceId());

    }

}
