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
import com.docdoku.plm.server.core.product.SerialNumberBasedEffectivity;
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class SerialNumberBasedEffectivityConfigSpecTest {

    private SerialNumberBasedEffectivityConfigSpec SNBECS;
    @Mock private ConfigurationItem configurationItem,tmp_CI;
    @Mock private SerialNumberBasedEffectivity SNBE;
    @Mock private ProductConfiguration configuration;

    @Before
    public void setUp() {

        SNBECS = mock(SerialNumberBasedEffectivityConfigSpec.class,CALLS_REAL_METHODS);
        initMocks(this);
        //when(configurationItem.getId()).thenReturn("ITEM-001");
        //when(tmp_CI.getId()).thenReturn("ITEM-002");
        Whitebox.setInternalState(SNBECS,"number","20");
        when(configuration.getConfigurationItem()).thenReturn(configurationItem);
    }

    @Test
    public void isEffectiveTest(){

        //--------------- TEST : Different Configuration item ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(SNBECS,"configurationItem",tmp_CI);
        when(SNBE.getConfigurationItem()).thenReturn(configurationItem);
        //#### END CONFIGURATION

        boolean result = SNBECS.isEffective(SNBE);

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION

        //--------------- TEST : StartNumber less than number ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(SNBECS,"configurationItem",configurationItem);
        when(SNBE.getStartNumber()).thenReturn("30");
        //#### END CONFIGURATION

        result = SNBECS.isEffective(SNBE);

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION

        //--------------- TEST : EndNumber greater than number ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(SNBECS,"configurationItem",configurationItem);
        when(SNBE.getStartNumber()).thenReturn("15");
        when(SNBE.getEndNumber()).thenReturn("15");
        //#### END CONFIGURATION

        result = SNBECS.isEffective(SNBE);

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION

        //--------------- TEST : Everything is right  ---------------
        //#### BEGIN CONFIGURATION
        Whitebox.setInternalState(SNBECS,"configurationItem",configurationItem);
        when(SNBE.getStartNumber()).thenReturn("15");
        when(SNBE.getEndNumber()).thenReturn("30");
        //#### END CONFIGURATION

        result = SNBECS.isEffective(SNBE);

        //#### BEGIN VERIFICATION
        Assert.assertTrue(result);
        //#### END VERIFICATION

        //--------------- TEST : Not serial based effectivity ---------------
        result = SNBECS.isEffective(new DateBasedEffectivity());

        //#### BEGIN VERIFICATION
        Assert.assertFalse(result);
        //#### END VERIFICATION
    }

    @Test
    public void test_constructor_field_from_parent(){

        //Check if configuration item was set correctly
        SerialNumberBasedEffectivityConfigSpec snb = new SerialNumberBasedEffectivityConfigSpec("A",configurationItem);
        Assert.assertNotNull(snb.getConfigurationItem());
        snb = new SerialNumberBasedEffectivityConfigSpec("A",configuration);
        Assert.assertNotNull(snb.getConfigurationItem());

        //Test getters and setters
        snb.setNumber("BX15ZX");
        Assert.assertEquals("BX15ZX",snb.getNumber());
    }
}