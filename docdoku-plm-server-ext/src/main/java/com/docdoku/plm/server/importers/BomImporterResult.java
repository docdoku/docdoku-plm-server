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

package com.docdoku.plm.server.importers;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BomImporterResult implements Serializable {

    private File importedFile;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String stdOutput;
    private String errorOutput;
    private Map<PartToImport, List<PartToImport>> partsMap;

    public BomImporterResult(File importedFile, List<String> warnings, List<String> errors, String stdOutput, String errorOutput,Map<PartToImport, List<PartToImport>> partsMap) {
        this.importedFile = importedFile;
        this.warnings = warnings;
        this.errors = errors;
        this.stdOutput = stdOutput;
        this.errorOutput = errorOutput;
        this.partsMap = partsMap;
    }

    public File getImportedFile() {
        return importedFile;
    }

    public void setImportedFile(File importedFile) {
        this.importedFile = importedFile;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
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


    public Map<PartToImport, List<PartToImport>> getPartsMap() {
        return partsMap;
    }

    public void setPartsMap(Map<PartToImport, List<PartToImport>> partsMap) {
        this.partsMap = partsMap;
    }
}
