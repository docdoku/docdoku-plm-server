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

package com.docdoku.plm.server.configuration.spec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.meta.RevisionStatus;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartLink;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static com.docdoku.plm.server.util.ProductUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class ProductBaselineCreationConfigSpecTest {

    private ProductBaselineCreationConfigSpec pB2CS;
    private PartMaster partMaster;

    @Before
    public void setUp() throws Exception {

        createTestableParts();
        generateSomeReleasedRevisionWithSubstitutesFor("PART-001");
        partMaster = getPartMasterWith("PART-001");
        List<PartIteration> iterations = new ArrayList<>();

        for(PartRevision revision : partMaster.getAllReleasedRevisions()){

            iterations.addAll(revision.getPartIterations());
        }

        pB2CS = new ProductBaselineCreationConfigSpec(ProductBaselineType.RELEASED,
                iterations,
                Collections.singletonList(""), Collections.singletonList(""));
    }

    @Test
    public void filterPartIterationTest(){

        //Test with RELEASED TYPE
        PartIteration result = pB2CS.filterPartIteration(partMaster);
        assertNotNull(result);
        areThoseOfRevision("K", Collections.singletonList(result),partMaster.getNumber());
        assertFalse(pB2CS.getRetainedPartIterations().isEmpty());
        assertEquals(1,pB2CS.getRetainedPartIterations().size());
        assertTrue(pB2CS.getRetainedPartIterations().contains(result));

        //Change partMaster
        //Expected null cause have not released revision
        partMaster = getPartMasterWith("PART-006");
        result = pB2CS.filterPartIteration(partMaster);
        assertNull(result);

        //add one released version
        addIterationTo(
                partMaster.getNumber(),
                partMaster.getLastRevision().createNextIteration(user));
        partMaster.getLastRevision().setStatus(RevisionStatus.RELEASED);

        result = pB2CS.filterPartIteration(partMaster);
        assertNotNull(result);
        assertEquals(partMaster.getLastReleasedRevision().getLastIteration(),result);

        //Test with LATEST TYPE
        pB2CS = new ProductBaselineCreationConfigSpec(ProductBaselineType.LATEST,
                new ArrayList<>(),
                Collections.singletonList(""), Collections.singletonList(""));
        partMaster = getPartMasterWith("PART-005");
        result = pB2CS.filterPartIteration(partMaster);
        assertNotNull(result);
        assertEquals(partMaster.getLastRevision().getLastCheckedInIteration(),result);
    }

    @Test
    public void filterPartLinkTest(){

        //PartLinks in list are optionals but not in the optionalUsageLinks list of pB2CS instance
        PartMaster partMaster = getPartMasterWith("PART-001");
        List<PartLink> links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));

        PartLink result =  pB2CS.filterPartLink(links);
        assertNotNull(result);
        assertEquals(links.get(links.size()-1),result);
        assertTrue(pB2CS.getRetainedOptionalUsageLinks().isEmpty());
        assertTrue(pB2CS.getRetainedSubstituteLinks().isEmpty());

        //Now, add those optional links to the optionalUsageLinks list of pB2CS instance
        //Expected the nominal link because we didn't set the substitutesUsageLinks list of pB2CS instance
        // nominal link <=> last partLink into given list

        pB2CS = new ProductBaselineCreationConfigSpec(ProductBaselineType.LATEST,
                new ArrayList<>(),
                Collections.singletonList(""),
                Collections.singletonList("u0-u0-u0-u0-u0"));

        result =  pB2CS.filterPartLink(links);
        assertNotNull(result);
        assertEquals(links.get(links.size() - 1), result);
        assertFalse(pB2CS.getRetainedOptionalUsageLinks().isEmpty());
        assertTrue(pB2CS.getRetainedSubstituteLinks().isEmpty());

        //Now, add substitutes of those optional links
        pB2CS = new ProductBaselineCreationConfigSpec(ProductBaselineType.LATEST,
                new ArrayList<>(),
                Collections.singletonList("u0-u0-u0-u0-s0"),
                Collections.singletonList("u0-u0-u0-u0-u0"));

        result =  pB2CS.filterPartLink(links);
        assertNotNull(result);
        assertEquals("PART-018-Substitute", result.getReferenceDescription());
        assertFalse(pB2CS.getRetainedOptionalUsageLinks().isEmpty());
        assertFalse(pB2CS.getRetainedSubstituteLinks().isEmpty());
    }
}