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

import java.io.Serializable;

/**
 * Minimalistic Attribute class with only  id, value and Attribute Model
 *
 * @author Laurent Le Van
 * @version 1.0.0
 * @since 03/02/16.
 */
public class Attribute implements Serializable {


    private String id;
    private String value;
    private AttributeModel model;

    /**
     * Constructor for an empty attribute
     */
    public Attribute(){
        this.id=null;
        this.model = new AttributeModel();
        this.value=null;
    }

    /**
     * Constructor that create an attribute with name,id,type and value
     * @param name name of the attribute
     * @param id id that identify that attribute
     * @param type which can be Text,Date, Boolean, Number, or ListOfValue
     * @param value value of that attribute
     */
    public Attribute(String name, String id, String type, String value){
        this.id=id;
        this.value=value;
        this.model = new AttributeModel(name,type);
    }

    /**
     * Constructor that takes 3 parameters : an id, an Attribute model and a String value
     * @param id id of an attribute
     * @param model model of an attribute
     * @param value value of this attribute
     */
    public Attribute(String id, AttributeModel model, String value){
        this.id=id;
        this.model =model;
        this.value = value;
    }

    public void setValue(String value){
        this.value = value;
    }

    public AttributeModel getModel(){
        return this.model;
    }

    public void setModel(AttributeModel model){
        this.model = model;
    }

    public String getValue(){
        return value;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }

    public String toString(){
        return "\nid : "+this.id+"\n"+model.toString()+"value : "+this.value+"\n\n";
    }
}

