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

package com.docdoku.plm.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.configuration.ProductStructureFilter;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.*;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.services.IIndexerManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.configuration.PSFilterVisitor;
import com.docdoku.plm.server.configuration.PSFilterVisitorCallbacks;
import com.docdoku.plm.server.dao.*;
import com.docdoku.plm.server.events.PartRevisionEvent;
import com.docdoku.plm.server.events.TagEvent;
import com.docdoku.plm.server.util.CyclicAssemblyRule;
import com.docdoku.plm.server.util.ProductUtil;

import javax.enterprise.event.Event;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProductManagerBeanTest {

    @InjectMocks
    ProductManagerBean productManagerBean = new ProductManagerBean();

    @Mock
    private ConfigurationItemDAO configurationItemDAO;
    @Mock
    private IIndexerManagerLocal indexerManager;
    @Mock
    private PartIterationDAO partIterationDAO;
    @Mock
    private PartRevisionDAO partRevisionDAO;
    @Mock
    private PartUsageLinkDAO partUsageLinkDAO;
    @Mock
    private TagDAO tagDAO;

    @Mock
    private IUserManagerLocal userManager;

    @Mock
    private Event<PartRevisionEvent> partRevisionEvent;
    @Mock
    private Event<TagEvent> tagEvent;

    @Mock
    private PSFilterVisitor psFilterVisitor;

    @Rule
    public CyclicAssemblyRule cyclicAssemblyRule;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Workspace workspace;
    private User user;
    private User user2;
    private PartMaster partMaster;
    private PartIteration partIteration;
    private PartRevision partRevision;


    @Before
    public void setup() {
        initMocks(this);
        Mockito.when(tagEvent.select(any())).thenReturn(tagEvent);
        Account account = new Account(ProductUtil.USER_2_LOGIN, ProductUtil.USER_2_NAME, ProductUtil.USER_1_MAIL, ProductUtil.USER_1_LANGUAGE, new Date(), null);
        workspace = new Workspace(ProductUtil.WORKSPACE_ID, account, "pDescription", false);
        user = new User(workspace, new Account(ProductUtil.USER_1_LOGIN, ProductUtil.USER_1_LOGIN, ProductUtil.USER_1_MAIL, ProductUtil.USER_1_LANGUAGE, new Date(), null));
        user2 = new User(workspace, new Account(ProductUtil.USER_2_LOGIN, ProductUtil.USER_2_LOGIN, ProductUtil.USER_2_MAIL, ProductUtil.USER_2_LANGUAGE, new Date(), null));
        partMaster = new PartMaster(workspace, ProductUtil.PART_ID, user);
        partRevision = new PartRevision(partMaster, ProductUtil.VERSION, user);
        partIteration = new PartIteration(partRevision, ProductUtil.ITERATION, user);
        ArrayList<PartIteration> iterations = new ArrayList<>();
        iterations.add(partIteration);

        partRevision.setPartIterations(iterations);
        partRevision.setCheckOutUser(user);
        partRevision.setCheckOutDate(new Date());
        partIteration.setPartRevision(partRevision);
    }

    @Test
    public void updatePartWithLockedAttributes() throws Exception {
        //Creation of current attributes of the iteration
        InstanceAttribute attribute = new InstanceTextAttribute("Test", "Testeur", false);
        List<InstanceAttribute> attributesOfIteration = new ArrayList<>();
        attributesOfIteration.add(attribute);
        partIteration.setInstanceAttributes(attributesOfIteration);
        partMaster.setAttributesLocked(true);

        Mockito.when(userManager.checkWorkspaceReadAccess(workspace.getId())).thenReturn(user);
        Mockito.when(userManager.checkWorkspaceWriteAccess(workspace.getId())).thenReturn(user);
        Mockito.when(partRevisionDAO.loadPartR(partRevision.getKey())).thenReturn(partRevision);

        ArrayList<PartUsageLink> partUsageLinks = new ArrayList<>();
        ArrayList<InstanceAttribute> newAttributes = new ArrayList<>();
        ArrayList<InstanceAttributeTemplate> newAttributeTemplates = new ArrayList<>();
        String[] lovNames = new String[0];

        try {
            //Test to remove attribute
            productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
            Assert.fail("updatePartIteration should have raise an exception because we have removed attributes");
        } catch (NotAllowedException notAllowedException) {
            try {
                //Test with a swipe of attribute
                newAttributes.add(new InstanceDateAttribute("Test", new Date(), false));
                productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
                Assert.fail("updateDocument should have raise an exception because we have changed the attribute type attributes");
            } catch (NotAllowedException notAllowedException2) {
                try {
                    //Test without modifying the attribute
                    newAttributes = new ArrayList<>();
                    newAttributes.add(attribute);
                    productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
                    //Test with a new value of the attribute
                    newAttributes = new ArrayList<>();
                    newAttributes.add(new InstanceTextAttribute("Test", "newValue", false));
                    productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
                } catch (NotAllowedException notAllowedException3) {
                    Assert.fail("updateDocument shouldn't have raised an exception because we haven't change the number of attribute or the type");
                }
            }
        }
        Mockito.verify(indexerManager, Mockito.never()).indexPartIteration(Mockito.any(PartIteration.class));

    }

    @Test
    public void updatePartWithUnlockedAttributes() throws Exception {
        //Creation of current attributes of the iteration
        InstanceAttribute attribute = new InstanceTextAttribute("Test", "Testeur", false);
        List<InstanceAttribute> attributesOfIteration = new ArrayList<>();
        attributesOfIteration.add(attribute);
        partIteration.setInstanceAttributes(attributesOfIteration);
        partMaster.setAttributesLocked(false);

        Mockito.when(userManager.checkWorkspaceReadAccess(workspace.getId())).thenReturn(user);
        Mockito.when(userManager.checkWorkspaceWriteAccess(workspace.getId())).thenReturn(user);
        Mockito.when(partRevisionDAO.loadPartR(partRevision.getKey())).thenReturn((partRevision));

        ArrayList<PartUsageLink> partUsageLinks = new ArrayList<>();
        ArrayList<InstanceAttribute> newAttributes = new ArrayList<>();
        ArrayList<InstanceAttributeTemplate> newAttributeTemplates = new ArrayList<>();
        String[] lovNames = new String[0];

        try {
            //Test to remove attribute
            productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
            //Test with a swipe of attribute
            newAttributes.add(new InstanceDateAttribute("Test", new Date(), false));
            productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
            //Test without modifying the attribute
            newAttributes = new ArrayList<>();
            newAttributes.add(attribute);
            productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);
            //Test with a new value of the attribute
            newAttributes = new ArrayList<>();
            newAttributes.add(new InstanceTextAttribute("Test", "newValue", false));
            productManagerBean.updatePartIteration(partIteration.getKey(), "Iteration note", null, partUsageLinks, newAttributes, newAttributeTemplates, new DocumentRevisionKey[]{}, null, lovNames);

        } catch (NotAllowedException notAllowedException) {
            Assert.fail("updateDocument shouldn't have raised an exception because the attributes are not frozen");
        }
        Mockito.verify(indexerManager, Mockito.never()).indexPartIteration(Mockito.any(PartIteration.class));
    }

    /**
     * test the add of new tags to a part that doesn't have any tag
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws UserNotActiveException
     * @throws PartRevisionNotFoundException
     * @throws AccessRightException
     */
    @Test
    public void addTagToPartWithNoTags() throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        PartRevisionKey partRevisionKey = partRevision.getKey();

        String[] tags = new String[3];
        tags[0] = "Important";
        tags[1] = "ToCheck";
        tags[2] = "ToDelete";

        Mockito.when(userManager.checkWorkspaceReadAccess(ProductUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(userManager.checkWorkspaceWriteAccess(ProductUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(partRevisionDAO.loadPartR(partRevision.getKey())).thenReturn((partRevision));
        Mockito.when(tagDAO.findAllTags(user.getWorkspaceId())).thenReturn(new Tag[0]);

        PartRevision partRevisionResult = productManagerBean.saveTags(partRevisionKey, tags);

        Assert.assertEquals(3, partRevisionResult.getTags().size());

        int i = 0;
        for (Tag tag : partRevisionResult.getTags()) {
            Assert.assertEquals(tag.getLabel(), tags[i++]);
        }

        Mockito.verify(indexerManager, Mockito.times(1)).indexPartIteration(Mockito.any(PartIteration.class));
    }


    @Test
    public void removeTagFromPart() throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        Set<Tag> tags = new LinkedHashSet<>();
        tags.add(new Tag(workspace, "Important"));
        tags.add(new Tag(workspace, "ToRemove"));
        tags.add(new Tag(workspace, "Urgent"));
        partRevision.setTags(tags);

        Mockito.when(userManager.checkWorkspaceReadAccess(ProductUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(userManager.checkWorkspaceWriteAccess(ProductUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(partRevisionDAO.loadPartR(partRevision.getKey())).thenReturn((partRevision));
        Mockito.when(partRevisionDAO.loadPartR(partRevision.getKey())).thenReturn((partRevision));

        PartRevision partRevisionResult = productManagerBean.removeTag(partRevision.getKey(), "Important");
        Mockito.verify(indexerManager, Mockito.times(1)).indexPartIteration(partRevisionResult.getLastIteration());
        Assert.assertEquals(2, partRevisionResult.getTags().size());
        Assert.assertFalse(partRevisionResult.getTags().contains(new Tag(workspace, "Important")));
        Assert.assertTrue(partRevisionResult.getTags().contains(new Tag(workspace, "Urgent")));
        Assert.assertTrue(partRevisionResult.getTags().contains(new Tag(workspace, "ToRemove")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullTagToOnePart() throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        partRevision.setTags(null);
        PartRevisionKey partRevisionKey = partRevision.getKey();

        Mockito.when(userManager.checkWorkspaceReadAccess(ProductUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(userManager.checkWorkspaceWriteAccess(ProductUtil.WORKSPACE_ID)).thenReturn(user);
        Mockito.when(partRevisionDAO.loadPartR(partRevisionKey)).thenReturn((partRevision));

        try {
            productManagerBean.saveTags(partRevisionKey, null);
        } catch (IllegalArgumentException e) {
            Mockito.verify(indexerManager, Mockito.never()).indexPartIteration(Mockito.any(PartIteration.class));
            throw e;
        }
    }

    @Test
    public void checkCyclicDetection() throws EntityConstraintException, PartMasterNotFoundException, UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, NotAllowedException, WorkspaceNotEnabledException {
        cyclicAssemblyRule = new CyclicAssemblyRule("user1");
        Mockito.when(userManager.checkWorkspaceReadAccess(ArgumentMatchers.anyString())).thenReturn(cyclicAssemblyRule.getUser());
        Mockito.doThrow(EntityConstraintException.class).when(psFilterVisitor).visit(any(String.class), any(ProductStructureFilter.class), any(PartMaster.class), Mockito.eq(-1), any(PSFilterVisitorCallbacks.class));

        thrown.expect(EntityConstraintException.class);

        productManagerBean.checkCyclicAssemblyForPartIteration(cyclicAssemblyRule.getP1().getLastRevision().getLastIteration());
        Mockito.verify(indexerManager, Mockito.never()).indexPartIteration(Mockito.any(PartIteration.class));
    }

    @Test(expected = NotAllowedException.class)
    public void getPartIterationCheckedOutByOther() throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, PartIterationNotFoundException, NotAllowedException, WorkspaceNotEnabledException {
        Mockito.when(userManager.checkWorkspaceReadAccess(partRevision.getKey().getPartMaster().getWorkspace())).thenReturn(user2);
        Mockito.when(partIterationDAO.loadPartI(partIteration.getKey())).thenReturn(partIteration);

        productManagerBean.getPartIteration(partIteration.getKey());
    }

    @Test(expected = NotAllowedException.class)
    public void updatePartIterationCheckedOutByOther() throws ListOfValuesNotFoundException, PartMasterNotFoundException, EntityConstraintException, WorkspaceNotFoundException, UserNotFoundException, NotAllowedException, UserNotActiveException, PartUsageLinkNotFoundException, AccessRightException, PartRevisionNotFoundException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException, PartIterationNotFoundException {

        Mockito.when(userManager.checkWorkspaceReadAccess(partRevision.getKey().getPartMaster().getWorkspace())).thenReturn(user2);
        Mockito.when(partRevisionDAO.loadPartR(partRevision.getKey())).thenReturn(partRevision);
        productManagerBean.updatePartIteration(partIteration.getKey(), null, null, null, null, null, null, null, null);
    }

}
