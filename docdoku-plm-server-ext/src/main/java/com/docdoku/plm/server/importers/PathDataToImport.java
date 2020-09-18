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

import com.docdoku.plm.server.core.configuration.PathDataIteration;
import com.docdoku.plm.server.core.meta.InstanceAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represent a minimalistic Product Instance with data which are useful to update a product Instance
 *
 * @author Laurent Le Van
 * @version 1.0.0
 * @since 29/02/16.
 */
public class PathDataToImport extends AttributesHolder implements Serializable {

    private String productId;
    private String serialNumber;
    private String partNumber;
    private String path;
    private String revisionNote;
    private PathDataIteration pathDataIteration;

    /**
     * Attributes in a simple format
     */
    private List<Attribute> attributes;

    /**
     * Attributes in a more detailed format
     */
    private List<InstanceAttribute> instanceAttributes;

    /**
     * Getter for an instance attribute
     *
     * @return an InstanceAttribute Object
     */
    public List<InstanceAttribute> getInstanceAttributes() {
        return instanceAttributes;
    }

    /**
     * Setter for an instance Attribute
     *
     * @param instanceAttributes z list of instance attribute
     */
    public void setInstanceAttributes(List<InstanceAttribute> instanceAttributes) {
        this.instanceAttributes = instanceAttributes;
    }

    /**
     * Add an attribute to the list
     *
     * @param attribute an attribute to add
     */
    public void addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
    }

    /**
     * Get the list of attributes
     *
     * @return a list a attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }

    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    /**
     * Initialization of a new Product Instance
     */
    public PathDataToImport() {
        this.productId = null;
        this.serialNumber = null;
        this.partNumber = null;
        this.path = null;
        this.attributes = new ArrayList<>();
        this.revisionNote = null;
        this.pathDataIteration = null;
    }

    /**
     * Initialization of a new Product Instance
     */
    public PathDataToImport(String id, String serialNumber, String partNumber, String path) {
        this.productId = id;
        this.serialNumber = serialNumber;
        this.partNumber = partNumber;
        this.path = path;
        this.attributes = new ArrayList<>();
    }

    public String getPath() {
        return this.path;
    }

    /**
     * Setter for the Path
     *
     * @param PathData as a String
     */
    public void setPath(String PathData) {
        this.path = PathData;
    }

    /**
     * Getter for the serial number
     *
     * @return the serial number
     */
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Set the serial number
     *
     * @param sn the serial number as a String
     */
    public void setSerialNumber(String sn) {
        this.serialNumber = sn;
    }

    /**
     * Get the part number
     *
     * @return the part number as a String
     */
    public String getPartNumber() {
        return this.partNumber;
    }

    /**
     * Set the part number
     *
     * @param pn the part number as a String
     */
    public void setPartNumber(String pn) {
        this.partNumber = pn;
    }

    /**
     * @return the product Id as a String
     */
    public String getProductId() {
        return this.productId;
    }

    /**
     * Getter for revision note
     *
     * @return the revision note as a String
     */
    public String getRevisionNote() {
        return revisionNote;
    }

    /**
     * Setter for the revision note
     *
     * @param revisionNote as a String
     */
    public void setRevisionNote(String revisionNote) {
        this.revisionNote = revisionNote;
    }

    /**
     * Getter for the path data iteration
     *
     * @return Path Data Iteration
     */
    public PathDataIteration getPathDataIteration() {
        return pathDataIteration;
    }

    /**
     * Setter for the path data iteration
     *
     * @param iteration a Path Data Iteration Object
     */
    public void setPathDataIteration(PathDataIteration iteration) {
        this.pathDataIteration = iteration;
    }

}
