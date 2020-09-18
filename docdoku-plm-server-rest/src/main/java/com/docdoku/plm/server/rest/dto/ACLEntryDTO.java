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
import com.docdoku.plm.server.core.security.ACLPermission;

import java.io.Serializable;


@ApiModel(value = "ACLEntryDTO", description = "This class holds permission data")
public class ACLEntryDTO implements Serializable {

    @ApiModelProperty(value = "Member id")
    private String key;

    @ApiModelProperty(value = "ACL permission value")
    private ACLPermission value;

    public ACLEntryDTO() {
    }

    public ACLEntryDTO(String key, ACLPermission value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ACLPermission getValue() {
        return value;
    }

    public void setValue(ACLPermission value) {
        this.value = value;
    }
}
