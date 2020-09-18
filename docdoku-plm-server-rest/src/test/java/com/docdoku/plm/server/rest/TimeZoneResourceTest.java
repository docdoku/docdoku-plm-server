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

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import static org.mockito.MockitoAnnotations.initMocks;

public class TimeZoneResourceTest {

    @InjectMocks
    private TimeZoneResource timeZoneResource = new TimeZoneResource();


    @Before
    public void setup() throws Exception {
        initMocks(this);
    }

    @Test
    public void getTimeZonesTest() {
        List<String> timeZones = timeZoneResource.getTimeZones();
        Assert.assertFalse(timeZones.isEmpty());
        List<String> availableTimeZones = Arrays.asList(TimeZone.getAvailableIDs());
        Collection intersection = CollectionUtils.intersection(timeZones, availableTimeZones);
        Assert.assertEquals(timeZones.size(),intersection.size());
    }

}
