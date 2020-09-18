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


/**
 *
 * @author Ludovic BAREL on 10/18.
 *
 * */
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

import static org.junit.Assert.*;
import static com.docdoku.plm.server.util.ProductUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class LatestCheckedInPSFilterTest {

    private LatestCheckedInPSFilter latestCheckedInPSFilter ;

    @Before
    public void setup() throws Exception {

        createTestableParts();
        latestCheckedInPSFilter = new LatestCheckedInPSFilter(false);
    }

    @Test
    public void filterTestWithPartMasterAsParameterTest(){

        setPartForLatestCheckInTest();
        PartMaster partMaster = getPartMasterWith("PART-001");
        List<PartIteration> result =  latestCheckedInPSFilter.filter(partMaster);

        assertFalse(result.isEmpty());
        areThoseOfRevision("D", result, partMaster.getNumber());

        //---------------------------

        partMaster.getLastRevision().setCheckOutUser(user);
        result =  latestCheckedInPSFilter.filter(partMaster);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filterTestWithListPartLinkAsParameterTest(){

        PartMaster partMaster = getPartMasterWith("PART-001");

        List<PartLink> links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));
        List<PartLink> result  = latestCheckedInPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        assertEquals("PART-008-UsageLink",result.get(0).getReferenceDescription());

        //----------------------

        latestCheckedInPSFilter = new LatestCheckedInPSFilter(true);

        String[] members_008 = {"PART-004","PART-015"};

        //released <=> true, checkout <=> true
        addRevisionWithPartLinkTo(getPartMasterWith("PART-008"), members_008, false, false);

        String[] members =  {"PART-007","PART-008"};
        addRevisionWithPartLinkTo(partMaster, members, false, false);

        links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));
        result  = latestCheckedInPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());
        assertEquals("PART-008-UsageLink",result.get(0).getReferenceDescription());

        //----------------------

        String[] substitutes_008 = {"PART-005", "PART-002","PART-004"};// check structure in ProductUtil.java
        addSubstituteInLastIterationOfLastRevisionTo(partMaster, substitutes_008, "PART-008");

        links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));
        result  = latestCheckedInPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(4,result.size());
        assertEquals("PART-008-UsageLink",result.get(0).getReferenceDescription());
        assertEquals("PART-005-Substitute",result.get(1).getReferenceDescription());
        assertEquals("PART-002-Substitute",result.get(2).getReferenceDescription());
        assertEquals("PART-004-Substitute",result.get(3).getReferenceDescription());
    }

    //############################################ HELPERS METHODS ############################################

    private void setPartForLatestCheckInTest(){

        // Notice : each parts created by ProductUtil class have a least one revision with one iteration
        PartMaster part = getPartMasterWith("PART-001");

        //Create REV-001
        PartRevision revision = part.createNextRevision(user);

        // checkedout <=> true
        addRevisionToPartWith("PART-001", revision, true);
        addIterationTo("PART-001", revision.createNextIteration(user));
        addIterationTo("PART-001", revision.createNextIteration(user));

        //Create REV-002
        revision = part.createNextRevision(user);
        addRevisionToPartWith("PART-001", revision, true);
        addIterationTo("PART-001", revision.createNextIteration(user));

        //Create REV-003
        revision = part.createNextRevision(user);
        addRevisionToPartWith("PART-001", revision, false);
        addIterationTo("PART-001", revision.createNextIteration(user));
    }
}