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


@ApiModel(value="BinaryResourceDTO", description="This class is the representation of a {@link com.docdoku.plm.server.core.common.BinaryResource} entity")
public class BinaryResourceDTO implements Serializable {

    @ApiModelProperty(value = "Resource full path")
    private String fullName;

    @ApiModelProperty(value = "Resource name")
    private String name;

    @ApiModelProperty(value = "Resource size")
    private long contentLength;

    @ApiModelProperty(value = "Resource last modification date")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date lastModified;

    public BinaryResourceDTO() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
