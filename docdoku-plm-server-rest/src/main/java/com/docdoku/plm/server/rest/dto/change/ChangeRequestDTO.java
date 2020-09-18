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

package com.docdoku.plm.server.rest.dto.change;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.json.bind.annotation.JsonbProperty;
import java.io.Serializable;
import java.util.List;


@ApiModel(value="ChangeRequestDTO", description="This class is the representation of a {@link com.docdoku.plm.server.core.change.ChangeRequest} entity")
public class ChangeRequestDTO extends ChangeItemDTO implements Serializable {

    @ApiModelProperty(value = "Change request addressed issues")
    private List<ChangeIssueDTO> addressedChangeIssues;

    @ApiModelProperty(value = "Change request due milestone id")
    @JsonbProperty(nillable = true)
    private int milestoneId;

    public ChangeRequestDTO() {

    }

    public List<ChangeIssueDTO> getAddressedChangeIssues() {
        return addressedChangeIssues;
    }

    public void setAddressedChangeIssues(List<ChangeIssueDTO> addressedChangeRequests) {
        this.addressedChangeIssues = addressedChangeRequests;
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(int milestoneId) {
        this.milestoneId = milestoneId;
    }
}
