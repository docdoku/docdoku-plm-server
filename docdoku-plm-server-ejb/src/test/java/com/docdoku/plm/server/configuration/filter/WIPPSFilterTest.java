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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Charles Fallourd
 * @version 2.5, 13/01/16
 */
@RunWith(MockitoJUnitRunner.class)
public class WIPPSFilterTest {

    private WIPPSFilter filter;
    private PartMaster partMaster;
    private List<PartRevision> partRevisions;

    @Before
    public void setup() {
        User user = Mockito.spy(new User());
        //Mockito.when(user.getLogin()).thenReturn("test");
        filter = new WIPPSFilter(user);
        partMaster = Mockito.spy(new PartMaster());
        partRevisions = new ArrayList<>();
        Mockito.when(partMaster.getPartRevisions()).thenReturn(partRevisions);
    }


    @Test
    public void testFilterNoIterationAccessible() throws Exception {
        PartRevision partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(null).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);


        //Should return empty list of partIteration if only one partRevision with no accessible iteration
        Assert.assertTrue(filter.filter(partMaster).isEmpty());

        partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(null).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);
        partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(null).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);
        partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(null).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);

        //Should still return empty list of part iteration with multiple partRevision
        Assert.assertTrue(filter.filter(partMaster).isEmpty());
    }

    @Test
    public void testFilterLastRevisionNoIteration() throws Exception {
        PartRevision partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(Mockito.mock(PartIteration.class)).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);

        Assert.assertEquals(1, filter.filter(partMaster).size());

        partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(null).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);

        Assert.assertEquals(1, filter.filter(partMaster).size());

        partRevision = Mockito.spy(new PartRevision());
        Mockito.doReturn(null).when(partRevision).getLastAccessibleIteration(Mockito.any());
        partRevisions.add(partRevision);

        Assert.assertEquals(1, filter.filter(partMaster).size());
    }
}
