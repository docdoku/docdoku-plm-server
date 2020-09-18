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
import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.util.DateUtils;

import javax.json.bind.annotation.JsonbDateFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


@ApiModel(value="ProductBaselineCreationDTO", description="DTO creation class for {@link com.docdoku.plm.server.core.configuration.ProductBaseline} entity")
public class ProductBaselineCreationDTO implements Serializable {

    @ApiModelProperty(value = "Baseline name")
    private String name;

    @ApiModelProperty(value = "Baseline description")
    private String description;

    @ApiModelProperty(value = "Configuration item in use")
    private String configurationItemId;

    @ApiModelProperty(value = "Baseline type")
    private ProductBaselineType type;

    @ApiModelProperty(value = "Baselined part list")
    private List<BaselinedPartDTO> baselinedParts;

    @ApiModelProperty(value = "Baseline substitute links used, as id list")
    private List<String> substituteLinks;

    @ApiModelProperty(value = "Baseline optional links retained, as id list")
    private List<String> optionalUsageLinks;

    @ApiModelProperty(value = "Effective date")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date effectiveDate;

    @ApiModelProperty(value = "Effective serial number")
    private String effectiveSerialNumber;

    @ApiModelProperty(value = "Effective lot id")
    private String effectiveLotId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfigurationItemId() {
        return configurationItemId;
    }

    public void setConfigurationItemId(String configurationItemId) {
        this.configurationItemId = configurationItemId;
    }

    public ProductBaselineType getType() {
        return type;
    }

    public void setType(ProductBaselineType type) {
        this.type = type;
    }

    public List<BaselinedPartDTO> getBaselinedParts() {
        return baselinedParts;
    }

    public void setBaselinedParts(List<BaselinedPartDTO> baselinedParts) {
        this.baselinedParts = baselinedParts;
    }

    public List<String> getSubstituteLinks() {
        return substituteLinks;
    }

    public void setSubstituteLinks(List<String> substituteLinks) {
        this.substituteLinks = substituteLinks;
    }

    public List<String> getOptionalUsageLinks() {
        return optionalUsageLinks;
    }

    public void setOptionalUsageLinks(List<String> optionalUsageLinks) {
        this.optionalUsageLinks = optionalUsageLinks;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getEffectiveSerialNumber() {
        return effectiveSerialNumber;
    }

    public void setEffectiveSerialNumber(String effectiveSerialNumber) {
        this.effectiveSerialNumber = effectiveSerialNumber;
    }

    public String getEffectiveLotId() {
        return effectiveLotId;
    }

    public void setEffectiveLotId(String effectiveLotId) {
        this.effectiveLotId = effectiveLotId;
    }
}
