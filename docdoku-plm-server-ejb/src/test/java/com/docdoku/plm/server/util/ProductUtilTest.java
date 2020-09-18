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

package com.docdoku.plm.server.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static com.docdoku.plm.server.util.ProductUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class ProductUtilTest {

    @Before
    public void setUp() throws Exception {

        initMocks(this);
        createTestableParts();
    }

    @Test
    public void createTestablePartsTest() {

        //Verify we've got right parts with expected assembly
        //Have a look into ProductUtil.java if you want to see the expected parts assembly
        //Only six first are enough to test all default parts
        //BEGIN : CHECKING PARTS ASSEMBLY

        assertNotNull(existingPart_list);
        assertEquals(20, existingPart_list.size());
        assertEquals(defaultPartsNumber_list[0], existingPart_list.get(0).getNumber());
        //Part1
        assertEquals(1, existingPart_list.get(0).getPartRevisions().size());
        assertEquals(1, existingPart_list.get(0).getLastRevision().getPartIterations().size());
        assertEquals(2, existingPart_list.get(0).getLastRevision().getLastIteration().getComponents().size());

        //Part2
        assertEquals(1, existingPart_list.get(1).getPartRevisions().size());
        assertEquals(1, existingPart_list.get(1).getLastRevision().getPartIterations().size());
        assertEquals(2, existingPart_list.get(1).getLastRevision().getLastIteration().getComponents().size());

        //Part3
        assertEquals(1,
                existingPart_list.get(2)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(2)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(0,
                existingPart_list.get(2)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        //Part4
        assertEquals(0,
                existingPart_list.get(3)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().size()
        );

        //Part5
        assertEquals(1,
                existingPart_list.get(4)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(4)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(4)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(0,
                existingPart_list.get(4)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        //Part6
        assertEquals(1,
                existingPart_list.get(5)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(5)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(5)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(5)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(1,
                existingPart_list.get(5)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );

        assertEquals(0,
                existingPart_list.get(5)
                        .getLastRevision()
                        .getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().get(0)
                        .getComponent().getLastRevision().getLastIteration()
                        .getComponents().size()
        );
        //END : CHECKING PARTS ASSEMBLY
    }

    @Test
    public void getPartMetaDataTest() {

        //allow test if metadata was created correctly
        assertNotNull(existingPart_list);
        assertEquals(20, existingPart_list.size());
        assertEquals(defaultPartsNumber_list.length, existingPart_list.size());
        assertEquals(defaultPartsNumber_list[0], existingPart_list.get(0).getNumber());
        metadataAsSameAsPartNumberList(defaultPartsNumber_list.length - 1);
    }

    @Test
    public void addRevisionToPartWithTest(){

        assertNotNull(existingPart_list);
        assertEquals(20, existingPart_list.size());
        assertEquals(defaultPartsNumber_list.length, existingPart_list.size());
        assertEquals(defaultPartsNumber_list[0], existingPart_list.get(0).getNumber());
        canAddNewRevisionToPart(defaultPartsNumber_list[0]);
    }

    @Test
    public void addIterationToRevisionTest(){

        assertNotNull(existingPart_list);
        assertEquals(20, existingPart_list.size());
        assertEquals(defaultPartsNumber_list.length, existingPart_list.size());
        assertEquals(defaultPartsNumber_list[0], existingPart_list.get(0).getNumber());
        canAddNewIterationToRevision(defaultPartsNumber_list[0]);
    }

    private void canAddNewIterationToRevision(String forPartNumber){

        PartRevision revision = getPartRevisionWith("A", forPartNumber);
        PartIteration it = new PartIteration();
        it.setIteration(1);

        //exiting iteration;
        addIterationTo(forPartNumber, it);
        assertEquals(1, getPartIterationsOf(forPartNumber).size());

        //non existing iteration
        it.setIteration(12);
        addIterationTo(forPartNumber, it);
        assertEquals(2, getPartIterationsOf(forPartNumber).size());
    }

    private void canAddNewRevisionToPart(String partNumber){

        PartRevision partRevision = new PartRevision();
        partRevision.setVersion("A");
        addRevisionToPartWith(partNumber, partRevision, false);

        //already have version
        assertEquals(1, getPartRevisionsOf(partNumber).size());

        //not have this version
        partRevision.setVersion("B");
        addRevisionToPartWith(partNumber, partRevision, false);
        assertEquals(2, getPartRevisionsOf(partNumber).size());
    }

    private void metadataAsSameAsPartNumberList(int lastIndex) {

        if (lastIndex < 0) {

            return;
        }
        assertNotNull(getPartMasterWith(defaultPartsNumber_list[lastIndex]));
        boolean same_instance = getPartMasterWith(defaultPartsNumber_list[lastIndex]) instanceof PartMaster;
        assertTrue(same_instance);
        PartMaster partMaster_extractFromMetaData = getPartMasterWith(defaultPartsNumber_list[lastIndex]);

        assertNotNull(existingPart_list.get(lastIndex));
        PartMaster partMaster_extractFromList = existingPart_list.get(lastIndex);

        assertEquals(partMaster_extractFromList, partMaster_extractFromMetaData);

        //check revision
        assertNotNull(getPartRevisionsOf(defaultPartsNumber_list[lastIndex]));
        same_instance = (getPartRevisionsOf(defaultPartsNumber_list[lastIndex]) instanceof List);
        assertTrue(same_instance);
        List<PartRevision> partMaster_revisions_extractFromMetaData = getPartRevisionsOf(defaultPartsNumber_list[lastIndex]);

        assertNotNull(partMaster_extractFromList.getPartRevisions());
        List<PartRevision> partMaster_revisions_extractFromList = partMaster_extractFromList.getPartRevisions();

        assertEquals(partMaster_revisions_extractFromList.size(),partMaster_revisions_extractFromMetaData.size());

        //check iteration
        assertNotNull(getPartIterationsOf(defaultPartsNumber_list[lastIndex]));
        same_instance = (getPartIterationsOf(defaultPartsNumber_list[lastIndex]) instanceof List);
        assertTrue(same_instance);
        List<PartIteration> partMaster_iterations_extractFromMetaData = getPartIterationsOf(defaultPartsNumber_list[lastIndex]);

        //cause only one iteration by revision by default
        assertNotNull(partMaster_extractFromList.getLastRevision().getPartIterations());
        List<PartIteration> partMaster_iterations_extractFromList =  partMaster_extractFromList.getLastRevision().getPartIterations();
        assertEquals(partMaster_iterations_extractFromMetaData.size(), partMaster_iterations_extractFromList.size());
        metadataAsSameAsPartNumberList(lastIndex - 1);

        //check revision version
        String partMaster_revision_version_extractFromMetaData = partMaster_extractFromMetaData.getLastRevision().getVersion();
        String partMaster_revision_version_extractFromList = partMaster_extractFromList.getLastRevision().getVersion();

        assertEquals( partMaster_revision_version_extractFromMetaData, partMaster_revision_version_extractFromList );

        //check revision instance
        PartRevision partRevision_instance_fromMetadata = getPartRevisionWith(partMaster_revision_version_extractFromList, partMaster_extractFromList.getNumber());

        assertNotNull(partRevision_instance_fromMetadata);
        assertEquals(partRevision_instance_fromMetadata, partMaster_extractFromList.getLastRevision());
    }
}