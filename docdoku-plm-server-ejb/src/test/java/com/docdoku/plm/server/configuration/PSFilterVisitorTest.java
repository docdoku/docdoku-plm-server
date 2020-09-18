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

package com.docdoku.plm.server.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.configuration.ProductConfigSpec;
import com.docdoku.plm.server.core.exceptions.EntityConstraintException;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.exceptions.PartMasterNotFoundException;
import com.docdoku.plm.server.core.meta.RevisionStatus;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.configuration.filter.LatestCheckedInPSFilter;
import com.docdoku.plm.server.configuration.filter.LatestReleasedPSFilter;
import com.docdoku.plm.server.configuration.spec.DateBasedEffectivityConfigSpec;
import com.docdoku.plm.server.configuration.spec.LotBasedEffectivityConfigSpec;
import com.docdoku.plm.server.configuration.spec.SerialNumberBasedEffectivityConfigSpec;
import com.docdoku.plm.server.dao.PartMasterDAO;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static com.docdoku.plm.server.util.ProductUtil.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest(PSFilterVisitor.class)
public class PSFilterVisitorTest {

    @InjectMocks
    private PSFilterVisitor psFilterVisitor = new PSFilterVisitor();

    @Mock
    private PartMasterDAO partMasterDAO;

    private PSFilterVisitorCallbacks callbacks;

    @Before
    public void setUp() throws Exception {

        initMocks(this);
        createTestableParts();
        buildBasicStructure();
        this.initDaoBahavior();
        callbacks = this.createCallBack();
    }

    @Test
    public void visitLatestCheckedInFilterTest() throws Exception {

        //#### TEST VISIT METHOD WITH PART MASTER AS PARAMETER
        //Notice : refer to BASIC STRUCTURE in ProductUtil.java

        //~ TEST : With simple structure ( No diverge )
        Component result = psFilterVisitor.visit(WORKSPACE_ID, new LatestCheckedInPSFilter(false), getPartMasterWith("PART-006"), -1, callbacks);
        assertNotNull(result);
        assertEquals("B",result.getRetainedIteration().getVersion());
        assertEquals("PART-006",result.getPartMaster().getNumber());

        assertEquals(3, result.getComponents().size());
        assertEquals(1, result.getComponents().get(0).getComponents().size());
        assertEquals("A",result.getComponents().get(0).getRetainedIteration().getPartVersion());
        assertEquals("PART-012",result.getComponents().get(0).getPartMaster().getNumber());

        assertEquals(3, result.getComponents().get(1).getComponents().size());
        assertEquals("B",result.getComponents().get(1).getRetainedIteration().getPartVersion());
        assertEquals("PART-002",result.getComponents().get(1).getPartMaster().getNumber());

        assertEquals(0, result.getComponents().get(2).getComponents().size());
        assertEquals("A",result.getComponents().get(2).getRetainedIteration().getPartVersion());
        assertEquals("PART-004",result.getComponents().get(2).getPartMaster().getNumber());

        //~ TEST : Complex structured parts ( No diverge )

        //Only one iteration on last checked out revision
        assertEquals("E",getPartMasterWith("PART-001").getLastRevision().getVersion());
        assertTrue(getPartMasterWith("PART-001").getLastRevision().isCheckedOut());
        assertEquals(1,getPartMasterWith("PART-001").getLastRevision().getPartIterations().size());
        try {

            psFilterVisitor.visit(WORKSPACE_ID, new LatestCheckedInPSFilter(false), getPartMasterWith("PART-001"), -1, callbacks);
            fail();
        }catch (NotAllowedException e){

            assertTrue(true);
        }

        //Add new iteration with same members than last iteration
        addIterationTo("PART-001",
                getPartMasterWith("PART-001").getLastRevision().createNextIteration(user));

        assertEquals("E",getPartMasterWith("PART-001").getLastRevision().getVersion());
        assertTrue(getPartMasterWith("PART-001").getLastRevision().isCheckedOut());
        assertEquals(2,getPartMasterWith("PART-001").getLastRevision().getPartIterations().size());

        result = psFilterVisitor.visit(WORKSPACE_ID, new LatestCheckedInPSFilter(false), getPartMasterWith("PART-001"), -1, callbacks);
        assertNotNull(result);
        assertEquals(1, result.getRetainedIteration().getIteration());

        //#### TEST VISIT METHOD WITH PART LINKS AS PARAMETER
    }

    @Test
    public void visitLatestReleasedFilterTest() throws Exception {

        //#### TEST VISIT METHOD WITH PART MASTER AS PARAMETER
        //~ TEST : None Released
        try{

            psFilterVisitor.visit(WORKSPACE_ID, new LatestReleasedPSFilter(false), getPartMasterWith(defaultPartsNumber_list[5]), -1, callbacks);
            Assert.fail();
        }catch (NotAllowedException e){

            Assert.assertTrue(true);
        }
    }

    //############################## HELPER METHODS ##############################

    private PSFilterVisitorCallbacks createCallBack(){

        return new PSFilterVisitorCallbacks() {
            @Override
            public void onIndeterminateVersion(PartMaster partMaster, List<PartIteration> partIterations) throws NotAllowedException {
                throw new NotAllowedException("NotAllowedException48");
            }

            @Override
            public void onUnresolvedVersion(PartMaster partMaster) throws NotAllowedException {
                throw new NotAllowedException("NotAllowedException49", partMaster.getNumber());
            }

            @Override
            public void onIndeterminatePath(List<PartLink> pCurrentPath, List<PartIteration> pCurrentPathPartIterations) throws NotAllowedException {
                throw new NotAllowedException("NotAllowedException50");
            }

            @Override
            public void onUnresolvedPath(List<PartLink> pCurrentPath, List<PartIteration> partIterations) throws NotAllowedException {
                throw new NotAllowedException("NotAllowedException51");
            }

            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                return true;
            }
        };
    }

    private void initDaoBahavior() throws PartMasterNotFoundException {

        for(String partNumber :  defaultPartsNumber_list){

            when(partMasterDAO.loadPartM(new PartMasterKey(WORKSPACE_ID,partNumber))).thenReturn(getPartMasterWith(partNumber));
        }
    }


    @Test
    public void visit_should_filter_serial_number_effectivity() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        ProductConfigSpec filter = new SerialNumberBasedEffectivityConfigSpec("2", configurationItem);

        // Create Mock ConfigurationItem with custom PartMaster
        PartMaster designItem = createPartMaster(configurationItem, "1", "A", true, ProductBaselineType.EFFECTIVE_SERIAL_NUMBER);
        //When
        visit(workspaceId, filter, designItem);

        //Then
        Assert.assertNotNull(filter);
        Assert.assertNotNull(filter.getRetainedPartIterations());
        Assert.assertEquals(filter.getRetainedPartIterations().size(), 2);
    }

    @Test
    public void visit_should_filter_lot_effectivity() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        ProductConfigSpec filter = new LotBasedEffectivityConfigSpec("50", configurationItem);

        // Create Mock ConfigurationItem with custom PartMaster
        PartMaster designItem = createPartMaster(configurationItem, "1", "A", true, ProductBaselineType.EFFECTIVE_LOT_ID);
        //When
        visit(workspaceId, filter, designItem);

        //Then
        Assert.assertNotNull(filter);
        Assert.assertNotNull(filter.getRetainedPartIterations());
        Assert.assertEquals(filter.getRetainedPartIterations().size(), 2);
    }

    @Test
    public void visit_should_filter_date_effectivity() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -2);
        ProductConfigSpec filter = new DateBasedEffectivityConfigSpec(calendar.getTime(), configurationItem);

        // Create Mock ConfigurationItem with custom PartMaster
        PartMaster designItem = createPartMaster(configurationItem, "1", "A", true, ProductBaselineType.EFFECTIVE_DATE);
        //When
        visit(workspaceId, filter, designItem);

        //Then
        Assert.assertNotNull(filter);
        Assert.assertNotNull(filter.getRetainedPartIterations());
        Assert.assertEquals(filter.getRetainedPartIterations().size(), 2);
    }

    @Test(expected = NotAllowedException.class)
    public void visit_should_not_work_without_partrevision() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        ProductConfigSpec filter = new SerialNumberBasedEffectivityConfigSpec("2", configurationItem);

        // Create Mock ConfigurationItem with custom PartMaster
        User user = configurationItem.getAuthor();
        Workspace workspace = configurationItem.getWorkspace();
        PartMaster designItem = new PartMaster(workspace, "1", user);
        //When
        visit(workspaceId, filter, designItem); //should throw NotAllowedException
    }

    @Test(expected = NotAllowedException.class)
    public void visit_should_not_work_without_effective_partlink() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        ProductConfigSpec filter = new LotBasedEffectivityConfigSpec("50", configurationItem);

        // Create Mock ConfigurationItem with custom PartMaster
        PartMaster designItem = createPartMaster(configurationItem, "1", "A", false, ProductBaselineType.EFFECTIVE_LOT_ID);
        designItem.getPartRevisions().get(0).getPartIterations().get(0).setComponents(new ArrayList<PartUsageLink>() {{
            PartUsageLink partUsageLink = createPartUsageLinkWithoutEffectivity(configurationItem);
            add(partUsageLink);
        }});
        //When
        visit(workspaceId, filter, designItem); //should throw NotAllowedException
    }

    @Test(expected = NotAllowedException.class)
    public void visit_should_not_work_without_effectivity() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        ProductConfigSpec filter = new SerialNumberBasedEffectivityConfigSpec("2", configurationItem);

        // Create Mock ConfigurationItem with custom PartMaster
        User user = configurationItem.getAuthor();
        Workspace workspace = configurationItem.getWorkspace();
        PartMaster designItem = new PartMaster(workspace, "1", user);
        PartRevision partRevision = new PartRevision(designItem, "A", configurationItem.getAuthor());
        partRevision.setStatus(RevisionStatus.RELEASED);
        designItem.setPartRevisions(new ArrayList<PartRevision>() {{
            add(partRevision);
        }});
        //When
        visit(workspaceId, filter, designItem); //should throw NotAllowedException
    }

    @Test
    public void visit_should_return_last_effective_part_revision() throws EntityConstraintException, NotAllowedException, PartMasterNotFoundException {
        //Given
        String workspaceId = "workspace01";
        ConfigurationItem configurationItem = createConfigurationItem(workspaceId);
        ProductConfigSpec filter = new LotBasedEffectivityConfigSpec("50", configurationItem);
        User user = configurationItem.getAuthor();
        Workspace workspace = configurationItem.getWorkspace();
        PartMaster designItem = new PartMaster(workspace, "1", user);
        designItem.setPartRevisions(new ArrayList<PartRevision>() {{
            add(createPartRevision(configurationItem, designItem, "1", "A", false, ProductBaselineType.EFFECTIVE_LOT_ID));
            add(createPartRevision(configurationItem, designItem, "99", "D", false, ProductBaselineType.EFFECTIVE_LOT_ID));
        }});

        //When
        visit(workspaceId, filter, designItem);

        //Then
        Assert.assertNotNull(filter);
        Assert.assertNotNull(filter.getRetainedPartIterations());
        Assert.assertEquals(filter.getRetainedPartIterations().size(), 1);
        Assert.assertEquals(filter.getRetainedPartIterations()
                .stream()
                .filter(partIteration -> partIteration.getPartVersion().equals("D"))
                .count(), 1);

        //Given
        ProductConfigSpec filter2 = new LotBasedEffectivityConfigSpec("50", configurationItem);
        PartMaster designItem2 = new PartMaster(workspace, "1", user);
        designItem2.setPartRevisions(new ArrayList<PartRevision>() {{
            add(createPartRevision(configurationItem, designItem, "99", "D", false, ProductBaselineType.EFFECTIVE_LOT_ID));
            add(createPartRevision(configurationItem, designItem, "1", "A", false, ProductBaselineType.EFFECTIVE_LOT_ID));
        }});

        //When
        visit(workspaceId, filter2, designItem2);

        //Then
        Assert.assertNotNull(filter2);
        Assert.assertNotNull(filter2.getRetainedPartIterations());
        Assert.assertEquals(filter2.getRetainedPartIterations().size(), 1);
        Assert.assertEquals(filter2.getRetainedPartIterations()
                .stream()
                .filter(partIteration -> partIteration.getPartVersion().equals("A"))
                .count(), 1);
    }

    private void visit(String workspaceId, ProductConfigSpec filter, PartMaster designItem) throws PartMasterNotFoundException, EntityConstraintException, NotAllowedException {
        psFilterVisitor.visit(workspaceId, filter, designItem, -1, createCallBack());
    }

    /**
     * Create a PartMaster for effectivity tests
     */
    private PartMaster createPartMaster(ConfigurationItem configurationItem, String pNumber, String pVersion, boolean createPartLink, ProductBaselineType productBaselineType) throws PartMasterNotFoundException {
        User user = configurationItem.getAuthor();
        Workspace workspace = configurationItem.getWorkspace();
        PartMaster partMaster = new PartMaster(workspace, pNumber, user);
        partMaster.setPartRevisions(new ArrayList<PartRevision>() {{
            add(createPartRevision(configurationItem, partMaster, pNumber, pVersion, createPartLink, productBaselineType));
        }});
        return partMaster;
    }

    private PartRevision createPartRevision(ConfigurationItem configurationItem, PartMaster partMaster, String pNumber, String pVersion, boolean createPartLink, ProductBaselineType productBaselineType) throws PartMasterNotFoundException {
        PartRevision partRevision = new PartRevision(partMaster, pVersion, configurationItem.getAuthor());
        partRevision.setStatus(RevisionStatus.RELEASED);
        partRevision.setPartIterations(new ArrayList<PartIteration>() {{
            add(createPartIteration(configurationItem, partRevision, pNumber, createPartLink, productBaselineType));
        }});

        Set<Effectivity> effectivities = new HashSet<>();
        switch (productBaselineType) {
            case EFFECTIVE_DATE:
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -4);
                Date date1 = calendar.getTime();
                calendar.add(Calendar.DATE, 6);
                Date date2 = calendar.getTime();
                effectivities.add(new DateBasedEffectivity("DateEffectivity-" + pVersion, configurationItem, date1, date2));
                break;
            case EFFECTIVE_LOT_ID:
                effectivities.add(new LotBasedEffectivity("LotIdEffectivity-" + pVersion, configurationItem, "45", "56"));
                break;
            case EFFECTIVE_SERIAL_NUMBER:
                effectivities.add(new SerialNumberBasedEffectivity("SerialNumberEffectivity-" + pVersion, configurationItem, "1", "3"));
                break;
            default:
        }
        partRevision.setEffectivities(effectivities);

        return partRevision;
    }

    private PartIteration createPartIteration(ConfigurationItem configurationItem, PartRevision partRevision, String pNumber, boolean createPartLink, ProductBaselineType productBaselineType) throws PartMasterNotFoundException {
        PartIteration partIteration = new PartIteration(partRevision, configurationItem.getAuthor());
        if (createPartLink) {
            partIteration.setComponents(new ArrayList<PartUsageLink>() {{
                String newPNumber = String.valueOf(Integer.parseInt(pNumber) + 1);
                add(createPartUsageLink(configurationItem, newPNumber, productBaselineType));
            }});
        }
        return partIteration;
    }

    private PartUsageLink createPartUsageLink(ConfigurationItem configurationItem, String pNumber, ProductBaselineType productBaselineType) throws PartMasterNotFoundException {
        PartMaster partMaster = createPartMaster(configurationItem, pNumber, "B", false, productBaselineType);
        Mockito.when(partMasterDAO.loadPartM(new PartMasterKey(configurationItem.getWorkspaceId(), pNumber))).thenReturn(partMaster);
        PartUsageLink partUsageLink = new PartUsageLink();
        partUsageLink.setComponent(partMaster);
        return partUsageLink;
    }

    private PartUsageLink createPartUsageLinkWithoutEffectivity(ConfigurationItem configurationItem) throws PartMasterNotFoundException {
        PartUsageLink partUsageLink = new PartUsageLink();
        User user = configurationItem.getAuthor();
        Workspace workspace = configurationItem.getWorkspace();
        PartMaster partMaster = new PartMaster(workspace, "2", user);
        PartRevision partRevision = new PartRevision(partMaster, "A", configurationItem.getAuthor());
        partRevision.setStatus(RevisionStatus.RELEASED);
        partMaster.setPartRevisions(new ArrayList<PartRevision>() {{
            add(partRevision);
        }});
        partUsageLink.setComponent(partMaster);
        Mockito.when(partMasterDAO.loadPartM(new PartMasterKey(configurationItem.getWorkspaceId(), "2"))).thenReturn(partMaster);
        return partUsageLink;
    }

    private ConfigurationItem createConfigurationItem(String workspaceId) {
        Workspace workspace = new Workspace(workspaceId);
        String login = "user1";
        User user = new User(workspace, new Account(login, login, login + "@docdoku.com", "en", new Date(), null));
        return new ConfigurationItem(user, workspace, "product1", "description");
    }
}