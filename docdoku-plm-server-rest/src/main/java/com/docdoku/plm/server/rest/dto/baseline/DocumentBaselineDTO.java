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
import com.docdoku.plm.server.core.configuration.DocumentBaselineType;
import com.docdoku.plm.server.core.util.DateUtils;
import com.docdoku.plm.server.rest.dto.UserDTO;

import javax.json.bind.annotation.JsonbDateFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


@ApiModel(value="DocumentBaselineDTO", description="This class is the representation of {@link com.docdoku.plm.server.core.configuration.DocumentBaseline} entity")
public class DocumentBaselineDTO implements Serializable {

    @ApiModelProperty(value = "Baseline id")
    private int id;

    @ApiModelProperty(value = "Baseline name")
    private String name;

    @ApiModelProperty(value = "Baseline description")
    private String description;

    @ApiModelProperty(value = "Baseline creation date")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date creationDate;

    @ApiModelProperty(value = "Baseline type")
    private DocumentBaselineType type;

    @ApiModelProperty(value = "Baselined document list")
    private List<BaselinedDocumentDTO> baselinedDocuments;

    @ApiModelProperty(value = "Baseline author")
    private UserDTO author;

    public DocumentBaselineDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public DocumentBaselineType getType() {
        return type;
    }

    public void setType(DocumentBaselineType type) {
        this.type = type;
    }

    public List<BaselinedDocumentDTO> getBaselinedDocuments() {
        return baselinedDocuments;
    }

    public void setBaselinedDocuments(List<BaselinedDocumentDTO> baselinedDocuments) {
        this.baselinedDocuments = baselinedDocuments;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }
}
