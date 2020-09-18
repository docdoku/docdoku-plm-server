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
import com.docdoku.plm.server.core.product.PartIterationKey;

import javax.json.bind.annotation.JsonbProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@ApiModel(value = "ConversionResultDTO", description = "")
public class ConversionResultDTO implements Serializable {

    public ConversionResultDTO(){

    }

    @ApiModelProperty(value = "PartIterationKey")
    @JsonbProperty(nillable = true)
    private PartIterationKey partIterationKey;

    @ApiModelProperty(value = "Temp dir")
    @JsonbProperty(nillable = true)
    private String tempDir;

    public static class PositionDTO implements Serializable {

        private double[] translation;
        private double[][] rotationmatrix;

        public PositionDTO(){}

        public PositionDTO(double[][] rm, double[] o) {
            this.translation = o;
            this.rotationmatrix = rm;
        }

        public double[] getTranslation() {
            return translation;
        }

        public void setTranslation(double[] translation) {
            this.translation = translation;
        }

        public double[][] getRotationmatrix() {
            return rotationmatrix;
        }

        public void setRotationmatrix(double[][] rotationmatrix) {
            this.rotationmatrix = rotationmatrix;
        }
    }

    @ApiModelProperty(value = "Converted file")
    @JsonbProperty(nillable = true)
    private String convertedFile;

    @ApiModelProperty(value = "Converted file LODs")
    @JsonbProperty(nillable = true)
    private Map<String,String> convertedFileLODs;

    @ApiModelProperty(value = "Materials")
    @JsonbProperty(nillable = true)
    private List<String> materials = new ArrayList<>();

    @ApiModelProperty(value = "Std output")
    @JsonbProperty(nillable = true)
    private String stdOutput;

    @ApiModelProperty(value = "Error output")
    @JsonbProperty(nillable = true)
    private String errorOutput;

    @ApiModelProperty(value = "Component position map")
    @JsonbProperty(nillable = true)
    private Map<String, List<PositionDTO>> componentPositionMap;

    @ApiModelProperty(value = "Bounding box")
    @JsonbProperty(nillable = true)
    private Double[] box;

    public PartIterationKey getPartIterationKey() {
        return partIterationKey;
    }

    public void setPartIterationKey(PartIterationKey partIterationKey) {
        this.partIterationKey = partIterationKey;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public String getConvertedFile() {
        return convertedFile;
    }

    public void setConvertedFile(String convertedFile) {
        this.convertedFile = convertedFile;
    }

    public List<String> getMaterials() {
        return materials;
    }

    public void setMaterials(List<String> materials) {
        this.materials = materials;
    }

    public String getStdOutput() {
        return stdOutput;
    }

    public void setStdOutput(String stdOutput) {
        this.stdOutput = stdOutput;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    public void setErrorOutput(String errorOutput) {
        this.errorOutput = errorOutput;
    }

    public Map<String, List<PositionDTO>> getComponentPositionMap() {
        return componentPositionMap;
    }

    public void setComponentPositionMap(Map<String, List<PositionDTO>> componentPositionMap) {
        this.componentPositionMap = componentPositionMap;
    }

    public Map<String, String> getConvertedFileLODs() {
        return convertedFileLODs;
    }

    public void setConvertedFileLODs(Map<String, String> convertedFileLODs) {
        this.convertedFileLODs = convertedFileLODs;
    }

    public Double[] getBox() {
        return box;
    }

    public void setBox(Double[] box) {
        this.box = box;
    }
}
