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
import org.mockito.Mockito;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.exceptions.PlatformHealthException;
import com.docdoku.plm.server.core.services.IPlatformHealthManagerLocal;
import com.docdoku.plm.server.rest.dto.PlatformHealthDTO;

import static org.mockito.MockitoAnnotations.initMocks;

public class PlatformResourceTest {

    @InjectMocks
    private PlatformResource platformResource = new PlatformResource();

    @Mock
    private IPlatformHealthManagerLocal platformHealthManager;

    @Before
    public void setup() throws Exception {
        initMocks(this);
    }

    @Test
    public void getPlatformHealthStatusTest() throws ApplicationException {
        Mockito.doNothing().when(platformHealthManager).runHealthCheck();
        PlatformHealthDTO platformHealthStatus = platformResource.getPlatformHealthStatus();
        Assert.assertEquals("ok", platformHealthStatus.getStatus());

        Mockito.doThrow(new PlatformHealthException("ooops")).when(platformHealthManager).runHealthCheck();
        try {
            platformResource.getPlatformHealthStatus();
            Assert.fail("Should have thrown");
        } catch (PlatformHealthException e){
            Assert.assertEquals("ooops",e.getMessage());
        }

    }

}
