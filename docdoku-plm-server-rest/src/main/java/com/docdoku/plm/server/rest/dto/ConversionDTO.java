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


@ApiModel(value="ConversionDTO", description="This class is the representation of a {@link com.docdoku.plm.server.core.product.Conversion} entity")
public class ConversionDTO implements Serializable {

    @ApiModelProperty(value = "Conversion end date")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date endDate;

    @ApiModelProperty(value = "Conversion start date")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date startDate;

    @ApiModelProperty(value = "Success flag")
    private boolean succeed;

    @ApiModelProperty(value = "Pending flag")
    private boolean pending;

    public ConversionDTO() {
    }

    public ConversionDTO(Date endDate, Date startDate, boolean succeed, boolean pending) {
        this.endDate = endDate;
        this.startDate = startDate;
        this.succeed = succeed;
        this.pending = pending;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public boolean isSucceed() {
        return succeed;
    }

    public void setSucceed(boolean succeed) {
        this.succeed = succeed;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }
}
