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
import java.util.ArrayList;
import java.util.List;


@ApiModel(value="ActivityModelDTO", description="This class is the representation of an {@link com.docdoku.plm.server.core.workflow.ActivityModel} entity")
public class ActivityModelDTO implements Serializable {

    @ApiModelProperty(value = "Activity model step")
    private int step;

    @ApiModelProperty(value = "Activity model relaunch step")
    private Integer relaunchStep;

    @ApiModelProperty(value = "List of task models")
    private List<TaskModelDTO> taskModels;

    @ApiModelProperty(value = "Final lifecycle state")
    private String lifeCycleState;

    @ApiModelProperty(value = "Type of the workflow model")
    private ActivityType type;

    @ApiModelProperty(value = "Amount of tasks to complete")
    private Integer tasksToComplete;

    public ActivityModelDTO() {
        this.taskModels = new ArrayList<>();
    }

    public ActivityModelDTO(int step, List<TaskModelDTO> taskModels, String lifeCycleState, ActivityType type, Integer tasksToComplete, Integer relaunchStep) {
        this.step = step;
        this.relaunchStep = relaunchStep;
        this.taskModels = taskModels;
        this.lifeCycleState = lifeCycleState;
        this.type = type;
        this.tasksToComplete = tasksToComplete;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void addTaskModel(TaskModelDTO m) {
        this.taskModels.add(m);
    }

    public void removeTaskModel(TaskModelDTO m) {
        this.taskModels.remove(m);
    }

    public List<TaskModelDTO> getTaskModels() {
        return this.taskModels;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public Integer getTasksToComplete() {
        return tasksToComplete;
    }

    public void setTasksToComplete(Integer tasksToComplete) {
        this.tasksToComplete = tasksToComplete;
    }

    public String getLifeCycleState() {
        return lifeCycleState;
    }

    public void setLifeCycleState(String lifeCycleState) {
        this.lifeCycleState = lifeCycleState;
    }

    public Integer getRelaunchStep() {
        return relaunchStep;
    }

    public void setRelaunchStep(Integer relaunchStep) {
        this.relaunchStep = relaunchStep;
    }

    public void setTaskModels(List<TaskModelDTO> taskModels) {
        this.taskModels = taskModels;
    }
}
