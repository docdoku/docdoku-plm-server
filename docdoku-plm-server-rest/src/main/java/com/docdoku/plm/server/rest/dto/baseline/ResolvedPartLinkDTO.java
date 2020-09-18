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

package com.docdoku.plm.server.rest.dto.baseline;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.docdoku.plm.server.rest.dto.LightPartLinkDTO;
import com.docdoku.plm.server.rest.dto.PartIterationDTO;

import java.io.Serializable;


@ApiModel(value="ResolvedPartLinkDTO", description="This class is the representation of a {@link com.docdoku.plm.server.core.product.PartLink} and its resolved {@link com.docdoku.plm.server.core.product.PartIteration} in a given context (ConfigSpec)")
public class ResolvedPartLinkDTO implements Serializable {

    @ApiModelProperty(value = "Resolved part iteration")
    private PartIterationDTO partIteration;

    @ApiModelProperty(value = "Usage Link")
    private LightPartLinkDTO partLink;

    public ResolvedPartLinkDTO() {
    }

    public PartIterationDTO getPartIteration() {
        return partIteration;
    }

    public void setPartIteration(PartIterationDTO partIteration) {
        this.partIteration = partIteration;
    }

    public LightPartLinkDTO getPartLink() {
        return partLink;
    }

    public void setPartLink(LightPartLinkDTO partLink) {
        this.partLink = partLink;
    }
}
