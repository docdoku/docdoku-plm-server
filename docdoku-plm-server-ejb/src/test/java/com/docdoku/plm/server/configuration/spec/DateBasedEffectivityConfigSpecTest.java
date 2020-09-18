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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class DateBasedEffectivityConfigSpecTest {

    private DateBasedEffectivityConfigSpec dateBasedEffectivityConfigSpec_withConfigurationItem;
    private DateBasedEffectivityConfigSpec dateBasedEffectivityConfigSpec_withProductConfiguration;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private Date after,before,date;

    @Mock ConfigurationItem configurationItem,configurationItem_tmp;
    @Mock ProductConfiguration productConfiguration;
    @Mock DateBasedEffectivity tmp_DBE;

    @Before
    public void setUp() throws ParseException {

        initMocks(this);
        //when(configurationItem.getId()).thenReturn("ITEM-001");
        //when(configurationItem.getWorkspaceId()).thenReturn(ProductUtil.WORKSPACE_ID);

        //when(configurationItem_tmp.getId()).thenReturn("ITEM-002");
        //when(configurationItem_tmp.getWorkspaceId()).thenReturn(ProductUtil.WORKSPACE_ID+"_tmp");

        when(productConfiguration.getConfigurationItem()).thenReturn(configurationItem);

        date =  dateFormat.parse("20/08/2012");
        //Only in order to test if constructors are functional
        dateBasedEffectivityConfigSpec_withConfigurationItem = new DateBasedEffectivityConfigSpec(
               date, configurationItem);
        dateBasedEffectivityConfigSpec_withProductConfiguration = new DateBasedEffectivityConfigSpec(
                date,productConfiguration);

        after = dateFormat.parse("20/08/2020");
        before = dateFormat.parse("20/08/2010");
    }

    @Test
    public void isEffectiveTest() throws ParseException {

        //------------- TEST : Check ConfigurationItem attributes from parent -------------
        Assert.assertNotNull(dateBasedEffectivityConfigSpec_withConfigurationItem.getConfigurationItem());
        Assert.assertNotNull(dateBasedEffectivityConfigSpec_withProductConfiguration.getConfigurationItem());

        //------------- TEST : No similar ConfigurationItem detected -------------
        //## BEGIN CONFIGURATION
        when(tmp_DBE.getConfigurationItem()).thenReturn(configurationItem_tmp);
        //## END CONFIGURATION

        boolean result = dateBasedEffectivityConfigSpec_withConfigurationItem.isEffective(tmp_DBE);

        //## BEGIN VERIFICATION
        Assert.assertFalse(result);
        //## END VERIFICATION

        //------------- TEST : Not effective because the start date has not arrived yet -------------
        //## BEGIN CONFIGURATION
        when(tmp_DBE.getConfigurationItem()).thenReturn(configurationItem);
        when(tmp_DBE.getStartDate()).thenReturn(after);
        //## END CONFIGURATION

        result = dateBasedEffectivityConfigSpec_withConfigurationItem.isEffective(tmp_DBE);

        //## BEGIN VERIFICATION
        Assert.assertFalse(result);
        //## END VERIFICATION

        //------------- TEST : Not effective because the date has passed -------------
        //## BEGIN CONFIGURATION
        when(tmp_DBE.getStartDate()).thenReturn(date);
        when(tmp_DBE.getEndDate()).thenReturn(before);
        //## END CONFIGURATION

        result = dateBasedEffectivityConfigSpec_withConfigurationItem.isEffective(tmp_DBE);

        //## BEGIN VERIFICATION
        Assert.assertFalse(result);
        //## END VERIFICATION

        //------------- TEST : Is effective -------------
        //## BEGIN CONFIGURATION
        when(tmp_DBE.getStartDate()).thenReturn(before);
        when(tmp_DBE.getEndDate()).thenReturn(after);
        //## END CONFIGURATION

        result = dateBasedEffectivityConfigSpec_withConfigurationItem.isEffective(tmp_DBE);

        //## BEGIN VERIFICATION
        Assert.assertTrue(result);
        //## END VERIFICATION

        //------------- TEST : Not a DataBasedEffectivity -------------
        result = dateBasedEffectivityConfigSpec_withConfigurationItem.isEffective(new LotBasedEffectivity());

        //## BEGIN VERIFICATION
        Assert.assertFalse(result);
        //## END VERIFICATION

        //------------- TEST : Getters and setters -------------
        //## BEGIN CONFIGURATION
        dateBasedEffectivityConfigSpec_withConfigurationItem.setDate(dateFormat.parse("11/04/2001"));
        //## BEGIN CONFIGURATION

        Assert.assertEquals(dateFormat.parse("11/04/2001"),dateBasedEffectivityConfigSpec_withConfigurationItem.getDate());
    }
}