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

/**
 * @author Morgan Guimard
 */

@ApiModel(value="UserStatsDTO",
        description="This class is a representation of user stats in workspace")
public class UserStatsDTO implements Serializable {

    @ApiModelProperty(value = "Users count")
    private Integer users;

    @ApiModelProperty(value = "Active users count")
    private Integer activeusers;

    @ApiModelProperty(value = "Inactive users count")
    private Integer inactiveusers;

    @ApiModelProperty(value = "Groups count")
    private Integer groups;

    @ApiModelProperty(value = "Active groups count")
    private Integer activegroups;

    @ApiModelProperty(value = "Inactive groups count")
    private Integer inactivegroups;

    public UserStatsDTO() {
    }


    public Integer getUsers() {
        return users;
    }

    public void setUsers(Integer users) {
        this.users = users;
    }

    public Integer getActiveusers() {
        return activeusers;
    }

    public void setActiveusers(Integer activeusers) {
        this.activeusers = activeusers;
    }

    public Integer getInactiveusers() {
        return inactiveusers;
    }

    public void setInactiveusers(Integer inactiveusers) {
        this.inactiveusers = inactiveusers;
    }

    public Integer getGroups() {
        return groups;
    }

    public void setGroups(Integer groups) {
        this.groups = groups;
    }

    public Integer getActivegroups() {
        return activegroups;
    }

    public void setActivegroups(Integer activegroups) {
        this.activegroups = activegroups;
    }

    public Integer getInactivegroups() {
        return inactivegroups;
    }

    public void setInactivegroups(Integer inactivegroups) {
        this.inactivegroups = inactivegroups;
    }
}
