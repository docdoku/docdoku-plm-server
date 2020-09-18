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
import javax.json.bind.annotation.JsonbProperty;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author morgan on 09/04/15.
 */


@ApiModel(value="QueryDTO", description="This class is a representation of a {@link com.docdoku.plm.server.core.query.Query} entity")
public class QueryDTO implements Serializable {

    @ApiModelProperty(value = "Query id")
    private int id;

    @ApiModelProperty(value = "Query name")
    private String name;

    @ApiModelProperty(value = "Query creation date")
    @JsonbDateFormat(value = DateUtils.GLOBAL_DATE_FORMAT)
    private Date creationDate;

    @ApiModelProperty(value = "Part iteration query rule")
    @JsonbProperty(nillable = true)
    private QueryRuleDTO queryRule;

    @ApiModelProperty(value = "Path data query rule")
    @JsonbProperty(nillable = true)
    private QueryRuleDTO pathDataQueryRule;

    @ApiModelProperty(value = "List of select statements")
    private List<String> selects;

    @ApiModelProperty(value = "List of order by statements")
    private List<String> orderByList;

    @ApiModelProperty(value = "List of grouped by statements")
    private List<String> groupedByList;

    @ApiModelProperty(value = "Query context list")
    private List<QueryContextDTO> contexts;

    public QueryDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public QueryRuleDTO getQueryRule() {
        return queryRule;
    }

    public void setQueryRule(QueryRuleDTO queryRule) {
        this.queryRule = queryRule;
    }

    public QueryRuleDTO getPathDataQueryRule() {
        return pathDataQueryRule;
    }

    public void setPathDataQueryRule(QueryRuleDTO pathDataQueryRule) {
        this.pathDataQueryRule = pathDataQueryRule;
    }

    public List<String> getSelects() {
        return selects;
    }

    public void setSelects(List<String> selects) {
        this.selects = selects;
    }

    public List<String> getOrderByList() {
        return orderByList;
    }

    public void setOrderByList(List<String> orderByList) {
        this.orderByList = orderByList;
    }

    public List<String> getGroupedByList() {
        return groupedByList;
    }

    public void setGroupedByList(List<String> groupedByList) {
        this.groupedByList = groupedByList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public List<QueryContextDTO> getContexts() {
        return contexts;
    }

    public void setContexts(List<QueryContextDTO> contexts) {
        this.contexts = contexts;
    }
}
