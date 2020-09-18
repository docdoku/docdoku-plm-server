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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static com.docdoku.plm.server.util.ProductUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class LatestReleasedPSFilterTest {

    private LatestReleasedPSFilter latestReleasedPSFilter;

    @Before
    public void setup() throws Exception {

        createTestableParts();
        latestReleasedPSFilter = new LatestReleasedPSFilter(false);
    }

    @Test
    public void filterTestWithPartMasterAsParameterTest(){

        PartMaster partMaster = getPartMasterWith("PART-001");
        String[] members = {};

        // released <=> true
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision B
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision C
        addRevisionWithPartLinkTo(partMaster, members, true, false);  // revision D

        List<PartIteration> result =  latestReleasedPSFilter.filter(partMaster);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        areThoseOfRevision("D", result, partMaster.getNumber());

        //--------------------------

        partMaster = getPartMasterWith("PART-002");
        partMaster.getLastRevision().release(user);
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision B
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision C
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision D

        result =  latestReleasedPSFilter.filter(partMaster);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        areThoseOfRevision("A", result, "PART-002");

        //--------------------------

        partMaster = getPartMasterWith("PART-003");
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision B
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision C
        addRevisionWithPartLinkTo(partMaster, members, false, false); // revision D

        result =  latestReleasedPSFilter.filter(partMaster);

        assertTrue(result.isEmpty());

        //--------------------------

        partMaster = getPartMasterWith("PART-004");
        partMaster.getLastRevision().release(user);
        addRevisionWithPartLinkTo(partMaster, members, true, false); // revision B
        addRevisionWithPartLinkTo(partMaster, members, true, false); // revision C
        addRevisionWithPartLinkTo(partMaster, members, false, false);// revision D

        result =  latestReleasedPSFilter.filter(partMaster);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        areThoseOfRevision("C", result, "PART-004");
    }

    @Test
    public void filterTestWithListPartLinkAsParameterTest(){

        generateSomeReleasedRevisionWithSubstitutesFor("PART-001");
        //diverge not enable
        PartMaster partMaster = getPartMasterWith("PART-001");

        List<PartLink> links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));
        List<PartLink> result  = latestReleasedPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());

        //enable diverge
        latestReleasedPSFilter = new LatestReleasedPSFilter(true);
        result  = latestReleasedPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(5,result.size());
        assertEquals("PART-016-UsageLink",result.get(0).getReferenceDescription());
        assertEquals("PART-018-Substitute",result.get(1).getReferenceDescription());
        assertEquals("PART-015-Substitute",result.get(2).getReferenceDescription());
        assertEquals("PART-009-Substitute",result.get(3).getReferenceDescription());
        assertEquals("PART-012-Substitute",result.get(4).getReferenceDescription());
    }
}