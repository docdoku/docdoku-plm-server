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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static com.docdoku.plm.server.util.ProductUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class ReleasedPSFilterTest {

    private ReleasedPSFilter releasedPSFilter;

    @Before
    public void setup() throws Exception {

        createTestableParts();
        releasedPSFilter = new ReleasedPSFilter(false);
        generateSomeReleasedRevisionWithSubstitutesFor("PART-001");
    }

    @Test
    public void filterTestWithPartMasterAsParameter(){

        List<PartIteration> result = releasedPSFilter.filter(getPartMasterWith("PART-001"));
        assertFalse(result.isEmpty());
        assertEquals(6,result.size());

        assertEquals("K",result.get(0).getPartVersion());
        assertEquals("J",result.get(1).getPartVersion());
        assertEquals("H",result.get(2).getPartVersion());
        assertEquals("G",result.get(3).getPartVersion());
        assertEquals("D",result.get(4).getPartVersion());
        assertEquals("B", result.get(5).getPartVersion());
    }

    @Test
    public void filterTestWithListPartLinkAsParameterTest(){

        //diverge not enable
        PartMaster partMaster = getPartMasterWith("PART-001");

        List<PartLink> links =  new ArrayList<>((partMaster.getLastRevision().getLastIteration().getComponents()));
        List<PartLink> result  = releasedPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(1,result.size());

        //enable diverge
        releasedPSFilter = new ReleasedPSFilter(true);
        result  = releasedPSFilter.filter(links);

        assertFalse(result.isEmpty());
        assertEquals(5,result.size());
        assertEquals("PART-016-UsageLink",result.get(0).getReferenceDescription());
        assertEquals("PART-018-Substitute",result.get(1).getReferenceDescription());
        assertEquals("PART-015-Substitute",result.get(2).getReferenceDescription());
        assertEquals("PART-009-Substitute",result.get(3).getReferenceDescription());
        assertEquals("PART-012-Substitute",result.get(4).getReferenceDescription());
    }
}