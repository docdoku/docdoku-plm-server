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

package com.docdoku.plm.server.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.initMocks;

public class ChangeItemsResourceTest {

    @InjectMocks
    private ChangeItemsResource changeItemsResource = new ChangeItemsResource();

    @Mock
    private ChangeIssuesResource issues;

    @Mock
    private ChangeRequestsResource requests;

    @Mock
    private ChangeOrdersResource orders;

    @Mock
    private MilestonesResource milestones;

    @Before
    public void setup() throws Exception {
        initMocks(this);
    }

    @Test
    public void issuesTest() {
        ChangeIssuesResource _issues = changeItemsResource.issues();
        Assert.assertEquals(issues, _issues);
    }

    @Test
    public void requestsTest() {
        ChangeRequestsResource _requests = changeItemsResource.requests();
        Assert.assertEquals(requests, _requests);
    }

    @Test
    public void ordersTest() {
        ChangeOrdersResource _orders = changeItemsResource.orders();
        Assert.assertEquals(orders, _orders);
    }

    @Test
    public void milestonesTest() {
        MilestonesResource _milestones = changeItemsResource.milestones();
        Assert.assertEquals(milestones, _milestones);
    }

}
