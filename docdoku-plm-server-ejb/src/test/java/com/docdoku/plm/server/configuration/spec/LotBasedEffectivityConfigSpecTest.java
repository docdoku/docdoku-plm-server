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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.configuration.ProductConfiguration;
import com.docdoku.plm.server.core.product.ConfigurationItem;
import com.docdoku.plm.server.core.product.DateBasedEffectivity;
import com.docdoku.plm.server.core.product.LotBasedEffectivity;
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class LotBasedEffectivityConfigSpecTest {

    private LotBasedEffectivityConfigSpec LBECS;
    @Mock private ConfigurationItem configurationItem,tmp_CI;
    @Mock private ProductConfiguration configuration;
    @Mock private LotBasedEffectivity LBE;

    @Before
    public void setUp() {

        LBECS = mock(LotBasedEffectivityConfigSpec.class,CALLS_REAL_METHODS);
        initMocks(this);
        //when(configurationItem.getId()).thenReturn("ITEM-001");
        //when(tmp_CI.getId()).thenReturn("ITEM-002");
        Whitebox.setInternalState(LBECS, "lotId", "5");
        when(configuration.getConfigurationItem()).thenReturn(configurationItem);
    }

    @Test
    public void isEffectiveTest(){

        //--------------- TEST : Different Configuration item ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(LBECS,"configurationItem",tmp_CI);
        when(LBE.getConfigurationItem()).thenReturn(configurationItem);
        //#### END CONFIGURATION

        boolean result = LBECS.isEffective(LBE);

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION

        //--------------- TEST : StartLotId less than lotId ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(LBECS,"configurationItem",configurationItem);
        when(LBE.getStartLotId()).thenReturn("15");
        //#### END CONFIGURATION

        result = LBECS.isEffective(LBE);

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION


        //--------------- TEST : EndLotId greater than lotId ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(LBECS,"configurationItem",configurationItem);
        when(LBE.getStartLotId()).thenReturn("1");
        when(LBE.getEndLotId()).thenReturn("4");
        //#### END CONFIGURATION

        result = LBECS.isEffective(LBE);

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION

        //--------------- TEST : Everything is right  ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(LBECS,"configurationItem",configurationItem);
        when(LBE.getStartLotId()).thenReturn("1");
        when(LBE.getEndLotId()).thenReturn("10");
        //#### END CONFIGURATION

        result = LBECS.isEffective(LBE);

        //#### BEGIN VERIFICATION
        Assert.assertTrue(result);
        //#### END VERIFICATION

        //--------------- TEST : Not serial based effectivity ---------------
        result = LBECS.isEffective(new DateBasedEffectivity());

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION
    }

    @Test
    public void test_constructor_field_from_parent(){

        //Check if configuration item was set correctly
        LotBasedEffectivityConfigSpec snb = new LotBasedEffectivityConfigSpec("A",configurationItem);
        Assert.assertNotNull(snb.getConfigurationItem());
        snb = new LotBasedEffectivityConfigSpec("A",configuration);
        Assert.assertNotNull(snb.getConfigurationItem());

        //Test getters and setters
        snb.setLotId("BX15ZX");
        Assert.assertEquals("BX15ZX",snb.getLotId());
    }

}