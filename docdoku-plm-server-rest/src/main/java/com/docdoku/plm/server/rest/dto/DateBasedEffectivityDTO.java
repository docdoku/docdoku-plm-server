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
import com.docdoku.plm.server.core.util.DateUtils;

import javax.json.bind.annotation.JsonbDateFormat;
import java.io.Serializable;
import java.util.Date;

@ApiModel(value = "DateBasedEffectivityDTO",
        description = "This class is the representation of a {@link com.docdoku.plm.server.core.common.DateBasedEffectivity} entity",
        parent = EffectivityDTO.class)
public class DateBasedEffectivityDTO extends EffectivityDTO implements Serializable {

    @ApiModelProperty(value = "Start date of the Effectivity")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date startDate;

    @ApiModelProperty(value = "End date of the Effectivity")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date endDate;

    public DateBasedEffectivityDTO() {
    }

    public Date getStartDate() { return startDate; }

    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }

    public void setEndDate(Date endDate) { this.endDate = endDate; }
}
