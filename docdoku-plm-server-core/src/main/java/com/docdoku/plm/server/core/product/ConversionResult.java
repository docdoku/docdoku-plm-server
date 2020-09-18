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

package com.docdoku.plm.server.core.product;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This ConversionResult class represents the conversion status done by a
 * CADConverter plugin.
 * <p>
 * It holds the converted file and its materials.
 */
public class ConversionResult implements Closeable, Serializable {


    public static class Position implements Serializable {

        /**
         * Position of a component instance
         */
        private static final long serialVersionUID = 1L;

        private double[] translation;
        private double[][] rotationmatrix;

        /**
         * Constructor for a component position.
         *
         * @param rm
         *            the rotation matrix
         * @param o
         *            Translation vector
         */
        public Position(double[][] rm, double[] o) {
            this.translation = o;
            this.rotationmatrix = rm;
        }

        public double[] getTranslation() {
            return translation;
        }

        public double[][] getRotationMatrix() {
            return this.rotationmatrix;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ConversionResult.class.getName());

    private static final long serialVersionUID = 1L;

    /**
     * The converted file LODs for succeed conversions
     * Map: QualityAsInteger, URI
     */
    private Map<Integer,Path> convertedFileLODs;
    /**
     * Bounding box
     */
    private double[] box;
    /**
     * The list of materials files if any
     */
    private List<Path> materials = new ArrayList<>();
    /**
     * The output of conversion program
     */
    private String stdOutput;
    /**
     * The error output of conversion program
     */
    private String errorOutput;

    private Map<String, List<Position>> componentPositionMap;

    private Path tempDir;
    /**
     * Default constructor
     */
    public ConversionResult() {
    }

    /**
     * Constructor with assembly component-position map.
     *
     * @param componentPositionMap
     *            Assembly components and positions
     */
    public ConversionResult(Map<String, List<Position>> componentPositionMap) {
        this.componentPositionMap = componentPositionMap;
    }

    public List<Path> getMaterials() {
        return materials;
    }

    public void setMaterials(List<Path> materials) {
        this.materials = new ArrayList<>();
        materials.forEach(path -> this.materials.add(path));
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

    public Map<String, List<Position>> getComponentPositionMap() {
        return this.componentPositionMap;
    }

    public void setComponentPositionMap(Map<String, List<Position>> componentPositionMap) {
        this.componentPositionMap = componentPositionMap;
    }

    public double[] getBox() {
        return box;
    }

    public void setBox(double[] box) {
        this.box = box;
    }

    public Map<Integer, Path> getConvertedFileLODs() {
        return convertedFileLODs;
    }

    public void setConvertedFileLODs(Map<Integer, Path> convertedFileLODs) {
        this.convertedFileLODs = convertedFileLODs;
    }

    public Path getTempDir() {
        return tempDir;
    }

    public void setTempDir(Path tempDir) {
        this.tempDir = tempDir;
    }

    @Override
    public void close() throws IOException {
        if (convertedFileLODs != null) {
            convertedFileLODs.values().forEach(convertedFile -> {
                try {
                    Files.deleteIfExists(convertedFile);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, null, e);
                }
            });
        }
        if (materials != null) {
            for (Path m : materials) {
                try {
                    Files.deleteIfExists(m);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, null, e);
                }
            }
        }
    }
}
