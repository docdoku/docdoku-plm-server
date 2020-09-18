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

package com.docdoku.plm.server.configuration.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartLink;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.docdoku.plm.server.util.ProductUtil.*;
import static com.docdoku.plm.server.util.ProductUtil.addIterationTo;
import static com.docdoku.plm.server.util.ProductUtil.user;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePartIterationPSFilterTest {

    private UpdatePartIterationPSFilter updatePartIterationPSFilter;

    @Before
    public void setup() throws Exception {

        createTestableParts();
        updatePartIterationPSFilter = new UpdatePartIterationPSFilter(getPartMasterWith("PART-002").getLastRevision().getLastIteration());
    }

    @Test
    public void filterTestWithPartMasterAsParameterTest(){

        PartMaster partMaster = getPartMasterWith("PART-002");
        //same PartMasterkey
        List<PartIteration> result = updatePartIterationPSFilter.filter(partMaster);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(partMaster.getLastRevision().getLastIteration(), result.get(0));

        //-------------
        generateStructureForTest();

        //last not checked out
        partMaster = getPartMasterWith("PART-001");
        result = updatePartIterationPSFilter.filter(partMaster);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(partMaster.getLastRevision().getLastIteration(), result.get(0));

        //-------------

        // last checked out with only one iteration
        partMaster.getLastRevision().setCheckOutUser(user);
        result = updatePartIterationPSFilter.filter(partMaster);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(partMaster.getLastRevision().getLastIteration(), result.get(0));

        //add an iteration to last checked out revision
        addIterationTo(
                partMaster.getNumber(),
                partMaster.getLastRevision().createNextIteration(user));

        assertTrue(partMaster.getLastRevision().isCheckedOut());

        result = updatePartIterationPSFilter.filter(partMaster);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getIteration());
        assertEquals(2, result.get(1).getIteration());
    }

    @Test
    public void filterTestWithListPartLinkAsParameterTest(){

        generateSomeReleasedRevisionWithSubstitutesFor("PART-001");
        PartMaster partMaster = getPartMasterWith("PART-001");

        List<PartLink> links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));
        List<PartLink> result  = updatePartIterationPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(5,result.size());
        assertEquals("PART-016-UsageLink", result.get(0).getReferenceDescription());
        assertEquals("PART-018-Substitute", result.get(1).getReferenceDescription());
        assertEquals("PART-015-Substitute", result.get(2).getReferenceDescription());
        assertEquals("PART-009-Substitute", result.get(3).getReferenceDescription());
        assertEquals("PART-012-Substitute", result.get(4).getReferenceDescription());
    }

    private void generateStructureForTest(){

        // checkedout <=> true

        PartMaster part = getPartMasterWith("PART-001");
        PartRevision revision = part.createNextRevision(user);

        addRevisionToPartWith("PART-001", revision, true);
        addIterationTo("PART-001", revision.createNextIteration(user));
        addIterationTo("PART-001", revision.createNextIteration(user));

        revision = part.createNextRevision(user);
        addRevisionToPartWith("PART-001", revision, true);
        addIterationTo("PART-001", revision.createNextIteration(user));

        revision = part.createNextRevision(user);
        addRevisionToPartWith("PART-001", revision, false);
        addIterationTo("PART-001", revision.createNextIteration(user));
    }
}