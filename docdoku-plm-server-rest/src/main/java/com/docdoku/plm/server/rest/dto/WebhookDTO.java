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
import java.util.List;


@ApiModel(value = "WebhookDTO", description = "This class is a representation of a {@link com.docdoku.plm.server.core.hooks.Webhook} entity")
public class WebhookDTO implements Serializable {

    @ApiModelProperty(value = "Webhook id")
    private int id;

    @ApiModelProperty(value = "Webhook name")
    private String name;

    @ApiModelProperty(value = "Webhook active flag")
    private boolean active;

    @ApiModelProperty(value = "Webhook parameters list")
    private List<WebhookAppParameterDTO> parameters;

    @ApiModelProperty(value = "Webhook app name")
    private String appName;

    public WebhookDTO() {
    }

    public WebhookDTO(int id, String name, boolean active, List<WebhookAppParameterDTO> parameters, String appName) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.parameters = parameters;
        this.appName = appName;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<WebhookAppParameterDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<WebhookAppParameterDTO> parameters) {
        this.parameters = parameters;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}

