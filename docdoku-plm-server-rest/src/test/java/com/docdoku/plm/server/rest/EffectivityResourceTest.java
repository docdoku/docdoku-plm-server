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
import com.docdoku.plm.server.core.product.DateBasedEffectivity;
import com.docdoku.plm.server.core.product.LotBasedEffectivity;
import com.docdoku.plm.server.core.product.SerialNumberBasedEffectivity;
import com.docdoku.plm.server.core.product.TypeEffectivity;
import com.docdoku.plm.server.core.services.IEffectivityManagerLocal;
import com.docdoku.plm.server.core.services.IProductManagerLocal;
import com.docdoku.plm.server.rest.dto.EffectivityDTO;

import javax.ws.rs.core.Response;
import java.util.Date;

import static org.mockito.MockitoAnnotations.initMocks;


/**
 * @author Morgan Guimard
 */
public class EffectivityResourceTest {

    @InjectMocks
    private EffectivityResource effectivityResource = new EffectivityResource();

    @Mock
    private IEffectivityManagerLocal effectivityManager;

    @Mock
    private IProductManagerLocal productManager;

    @Before
    public void init(){
        initMocks(this);
        effectivityResource.init();
    }

    @Test
    public void getEffectivityTest() throws ApplicationException {
        int effectivityId = 42;
        String workspaceId = "wks";

        Mockito.when(effectivityManager.getEffectivity(workspaceId, effectivityId))
                .thenReturn(new SerialNumberBasedEffectivity());
        EffectivityDTO effectivityDTO = effectivityResource.getEffectivity("wks", effectivityId);
        Assert.assertNotNull(effectivityDTO);
        Assert.assertEquals(TypeEffectivity.SERIALNUMBERBASEDEFFECTIVITY, effectivityDTO.getTypeEffectivity());


        Mockito.when(effectivityManager.getEffectivity(workspaceId, effectivityId))
                .thenReturn(new DateBasedEffectivity());
        effectivityDTO = effectivityResource.getEffectivity("wks", effectivityId);
        Assert.assertNotNull(effectivityDTO);
        Assert.assertEquals(TypeEffectivity.DATEBASEDEFFECTIVITY, effectivityDTO.getTypeEffectivity());


        Mockito.when(effectivityManager.getEffectivity(workspaceId, effectivityId))
                .thenReturn(new LotBasedEffectivity());
        effectivityDTO = effectivityResource.getEffectivity("wks", effectivityId);
        Assert.assertNotNull(effectivityDTO);
        Assert.assertEquals(TypeEffectivity.LOTBASEDEFFECTIVITY, effectivityDTO.getTypeEffectivity());

    }

    @Test
    public void updateEffectivityTest() throws ApplicationException {
        String workspaceId = "wks";
        int effectivityId = 42;

        // serial number

        EffectivityDTO effectivityDTO = new EffectivityDTO();
        effectivityDTO.setId(effectivityId);
        effectivityDTO.setDescription("Ola ola");
        effectivityDTO.setTypeEffectivity(TypeEffectivity.SERIALNUMBERBASEDEFFECTIVITY);
        effectivityDTO.setStartNumber("ABCDEF");
        effectivityDTO.setEndNumber("GHIJKL");

        Mockito.when(effectivityManager.updateSerialNumberBasedEffectivity(workspaceId, effectivityId,
                effectivityDTO.getName(), effectivityDTO.getDescription(),
                effectivityDTO.getStartNumber(), effectivityDTO.getEndNumber()))
                .thenReturn(new SerialNumberBasedEffectivity());

        Response res = effectivityResource.updateEffectivity(workspaceId, effectivityId, effectivityDTO);
        Object entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(EffectivityDTO.class));
        EffectivityDTO result = (EffectivityDTO) entity;
        Assert.assertEquals(TypeEffectivity.SERIALNUMBERBASEDEFFECTIVITY, result.getTypeEffectivity());

        // date

        effectivityDTO.setTypeEffectivity(TypeEffectivity.DATEBASEDEFFECTIVITY);
        effectivityDTO.setStartDate(new Date());
        effectivityDTO.setEndDate(new Date());

        Mockito.when( effectivityManager.updateDateBasedEffectivity(workspaceId, effectivityId,
                effectivityDTO.getName(), effectivityDTO.getDescription(),
                effectivityDTO.getStartDate(), effectivityDTO.getEndDate()))
                .thenReturn(new DateBasedEffectivity());

        res = effectivityResource.updateEffectivity(workspaceId, effectivityId, effectivityDTO);
        entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(EffectivityDTO.class));
        result = (EffectivityDTO) entity;
        Assert.assertEquals(TypeEffectivity.DATEBASEDEFFECTIVITY, result.getTypeEffectivity());

        // lot

        effectivityDTO.setTypeEffectivity(TypeEffectivity.LOTBASEDEFFECTIVITY);
        effectivityDTO.setStartLotId("ABC");
        effectivityDTO.setEndLotId("DEF");

        Mockito.when(effectivityManager.updateLotBasedEffectivity(workspaceId, effectivityId,
                effectivityDTO.getName(), effectivityDTO.getDescription(),
                effectivityDTO.getStartLotId(), effectivityDTO.getEndLotId()))
                .thenReturn(new LotBasedEffectivity());

        res = effectivityResource.updateEffectivity(workspaceId, effectivityId, effectivityDTO);
        entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(EffectivityDTO.class));
        result = (EffectivityDTO) entity;
        Assert.assertEquals(TypeEffectivity.LOTBASEDEFFECTIVITY, result.getTypeEffectivity());

        // unset type

        effectivityDTO.setTypeEffectivity(null);
        Mockito.when( effectivityManager.updateEffectivity(workspaceId, effectivityId,
                effectivityDTO.getName(), effectivityDTO.getDescription()))
                .thenReturn(new LotBasedEffectivity());
        res = effectivityResource.updateEffectivity(workspaceId, effectivityId, effectivityDTO);
        entity = res.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(EffectivityDTO.class));
        result = (EffectivityDTO) entity;
        Assert.assertEquals(null, result.getTypeEffectivity());


    }
}
