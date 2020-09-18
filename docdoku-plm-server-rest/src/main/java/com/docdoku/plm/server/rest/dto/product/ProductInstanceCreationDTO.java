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

package com.docdoku.plm.server.rest.dto.product;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.util.DateUtils;
import com.docdoku.plm.server.rest.dto.ACLDTO;
import com.docdoku.plm.server.rest.dto.BinaryResourceDTO;
import com.docdoku.plm.server.rest.dto.DocumentRevisionDTO;
import com.docdoku.plm.server.rest.dto.InstanceAttributeDTO;

import javax.json.bind.annotation.JsonbDateFormat;
import java.io.Serializable;
import java.util.*;


@ApiModel(value = "ProductInstanceCreationDTO", description = "Use this class to create a new {@link com.docdoku.plm.server.core.configuration.ProductInstanceMaster} entity")
public class ProductInstanceCreationDTO implements Serializable {

    @ApiModelProperty(value = "Product instance serial number")
    private String serialNumber;

    @ApiModelProperty(value = "Configuration item in use")
    private String configurationItemId;

    @ApiModelProperty(value = "Baseline in use")
    private Integer baselineId;

    @ApiModelProperty(value = "Product instance ACL")
    private ACLDTO acl;

    @ApiModelProperty(value = "Product instance attributes")
    private List<InstanceAttributeDTO> instanceAttributes = new ArrayList<>();

    @ApiModelProperty(value = "Product instance linked documents")
    private Set<DocumentRevisionDTO> linkedDocuments = new HashSet<>();

    @ApiModelProperty(value = "Product instance attached files")
    private List<BinaryResourceDTO> attachedFiles;

    @ApiModelProperty(value = "Effectivity filter in use")
    private ProductBaselineType type;

    @ApiModelProperty(value = "Date for date effectivity filter")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date effectiveDate;

    @ApiModelProperty(value = "Serial number for serial number effectivity filter")
    private String effectiveSerialNumber;

    @ApiModelProperty(value = "Lot id for lot id effectivity filter")
    private String effectiveLotId;

    public ProductInstanceCreationDTO() {
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getConfigurationItemId() {
        return configurationItemId;
    }

    public void setConfigurationItemId(String configurationItemId) {
        this.configurationItemId = configurationItemId;
    }

    public Integer getBaselineId() {
        return baselineId;
    }

    public void setBaselineId(Integer baselineId) {
        this.baselineId = baselineId;
    }

    public ACLDTO getAcl() {
        return acl;
    }

    public void setAcl(ACLDTO acl) {
        this.acl = acl;
    }

    public List<InstanceAttributeDTO> getInstanceAttributes() {
        return instanceAttributes;
    }

    public void setInstanceAttributes(List<InstanceAttributeDTO> instanceAttributes) {
        this.instanceAttributes = instanceAttributes;
    }

    public Set<DocumentRevisionDTO> getLinkedDocuments() {
        return linkedDocuments;
    }

    public void setLinkedDocuments(Set<DocumentRevisionDTO> linkedDocuments) {
        this.linkedDocuments = linkedDocuments;
    }

    public List<BinaryResourceDTO> getAttachedFiles() {
        return attachedFiles;
    }

    public void setAttachedFiles(List<BinaryResourceDTO> attachedFiles) {
        this.attachedFiles = attachedFiles;
    }



    public ProductBaselineType getType() {
        return type;
    }

    public void setType(ProductBaselineType type) {
        this.type = type;
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
