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
package com.docdoku.plm.server.rest.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;


@ApiModel(value = "SerialNumberBasedEffectivityDTO",
        description = "This class is the representation of a {@link com.docdoku.plm.server.core.common.SerialNumberBasedEffectivity} entity",
        parent = EffectivityDTO.class)
public class SerialNumberBasedEffectivityDTO extends EffectivityDTO implements Serializable {

    @ApiModelProperty(value = "Start number of the Serial Number")
    private String startNumber;

    @ApiModelProperty(value = "End number of the Serial Number")
    private String endNumber;

    public SerialNumberBasedEffectivityDTO() {
    }

    public String getStartNumber() { return startNumber; }

    public void setStartNumber(String startNumber) { this.startNumber = startNumber; }

    public String getEndNumber() { return endNumber; }

    public void setEndNumber(String endNumber) { this.endNumber = endNumber; }
}
