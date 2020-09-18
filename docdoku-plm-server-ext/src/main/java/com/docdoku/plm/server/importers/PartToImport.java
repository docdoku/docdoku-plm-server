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

import com.docdoku.plm.server.core.meta.InstanceAttribute;
import com.docdoku.plm.server.core.product.PartIteration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represent a minimalistic Part with data which are useful to update a part
 *
 * @author Laurent Le Van
 * @version 1.0.0
 * @since 03/02/16.
 */
public class PartToImport extends AttributesHolder implements Serializable {

    private String number;
    private String revisionNote;
    private String description;
    private PartIteration partIteration;
    private double amount = 1; // default value
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
     * Initialization of a new empty Part
     */
    public PartToImport() {
        this.number = null;
        this.attributes = new ArrayList<>();
    }

    /**
     * Initialize a Part with its number
     *
     * @param number number of the attribute
     */
    public PartToImport(String number) {
        this.number = number;
        this.attributes = new ArrayList<>();
    }

    /**
     * Initialize an new instance Article with a number and a ArrayList of Attribute
     *
     * @param number Part number
     * @param list   yList of part
     */
    public PartToImport(String number, List<Attribute> list) {
        this.number = number;
        this.attributes = list;
    }

    public String getRevisionNote() {
        return revisionNote;
    }

    public void setRevisionNote(String revisionNote) {
        this.revisionNote = revisionNote;
    }

    public PartIteration getPartIteration() {
        return partIteration;
    }

    public void setPartIteration(PartIteration partIteration) {
        this.partIteration = partIteration;
    }

    /**
     * Getter for part number
     *
     * @return part number
     */
    public String getNumber() {
        return this.number;
    }

    /**
     * Setter for part number
     *
     * @param number part number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
