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

import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.*;
import com.docdoku.plm.server.core.services.ILOVManagerLocal;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AttributesImporterUtils {

    private static final Logger LOGGER = Logger.getLogger(AttributesImporterUtils.class.getName());

    private AttributesImporterUtils() {
    }

    public static void updateAndCreateInstanceAttributes(ILOVManagerLocal lovManager, Properties properties, List<Attribute> attributes, List<InstanceAttribute> currentInstanceAttributes, String objectId, List<String> errors, String workspaceId, List<InstanceAttribute> updatedInstanceAttributes) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {

        for (Attribute attribute : attributes) {
            // Let's browse attributes we want to update or create
            InstanceAttribute currentAttribute = findAttribute(currentInstanceAttributes, attribute);
            if (attribute.getId() != null) {
                if (currentAttribute == null) {
                    String error = createError(properties, "AttributeNotFound", attribute.getModel().getName(), objectId);
                    LOGGER.log(Level.WARNING, error);
                    errors.add(error);

                } else {
                    updateAttribute(properties, attribute, currentAttribute, errors);
                    removeAttribute(currentInstanceAttributes, currentAttribute);//we delete it from this list to avoid double update on the same object
                }
            } else {
                if (currentAttribute == null) {
                    InstanceAttribute instanceAttribute = createAttribute(lovManager, properties, attribute, workspaceId, errors);
                    updatedInstanceAttributes.add(instanceAttribute);
                } else {
                    LOGGER.log(Level.WARNING, "Attribute" + attribute.getModel().getName() + " already exist");
                    String errorMsg = createError(properties, "DuplicateEntry", attribute.getModel().getName());
                    if (!errors.contains(errorMsg)) errors.add(errorMsg);
                }
            }
        }

    }
    public static InstanceAttribute createInstanceAttribute(Properties properties, InstanceAttribute instanceAttribute, List<String> errors) {
        InstanceAttribute attribute;
        if (instanceAttribute instanceof InstanceTextAttribute) {
            attribute = new InstanceTextAttribute();
        } else if (instanceAttribute instanceof InstanceBooleanAttribute) {
            attribute = new InstanceBooleanAttribute();
        } else if (instanceAttribute instanceof InstanceURLAttribute) {
            attribute = new InstanceURLAttribute();
        } else if (instanceAttribute instanceof InstanceDateAttribute) {
            attribute = new InstanceDateAttribute();
        } else if (instanceAttribute instanceof InstanceNumberAttribute) {
            attribute = new InstanceNumberAttribute();
        } else if (instanceAttribute instanceof InstanceListOfValuesAttribute) {
            attribute = new InstanceListOfValuesAttribute();
            ((InstanceListOfValuesAttribute) attribute).setItems(createLovItem((InstanceListOfValuesAttribute) instanceAttribute));
        } else if (instanceAttribute instanceof InstanceLongTextAttribute) {
            attribute = new InstanceLongTextAttribute();
        } else {
            errors.add(createError(properties, "InvalidAttributeType"));
            return null;
        }

        attribute.setName(instanceAttribute.getName());
        attribute.setLocked(instanceAttribute.isLocked());
        attribute.setMandatory(instanceAttribute.isMandatory());
        attribute.setValue(instanceAttribute.getValue());
        return attribute;
    }


    public static List<NameValuePair> createLovItem(InstanceListOfValuesAttribute instanceAttribute) {
        List<NameValuePair> nameValues = new ArrayList<>();
        for (NameValuePair nameValuePair : instanceAttribute.getItems()) {
            NameValuePair nameValue = new NameValuePair();
            nameValue.setName(nameValuePair.getName());
            nameValue.setValue(nameValuePair.getValue());
            nameValues.add(nameValue);
        }
        return nameValues;
    }

    public static String createError(Properties properties, String errorKey, Object... params) {
        return MessageFormat.format(properties.getProperty(errorKey), params);
    }

    public static List<InstanceAttribute> getInstanceAttributes(Properties properties, List<InstanceAttribute> instanceAttributes, List<String> errors) {
        return instanceAttributes
                .stream()
                .map(instanceAttribute -> createInstanceAttribute(properties, instanceAttribute, errors))
                .collect(Collectors.toList());
    }



    public static void updateAttribute(Properties properties, Attribute attributeToImport, InstanceAttribute attribute, List<String> errors) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {

        final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        switch (attributeToImport.getModel().getType()) {
            case "TEXT": case "LONG_TEXT":
                attribute.setValue(attributeToImport.getValue());
                break;
            case "NUMBER":
                Float nbValue = attributeToImport.getValue() == null ? null : Float.parseFloat(attributeToImport.getValue());
                attribute.setValue(nbValue);
                break;
            case "DATE":
                Date dateValue = null;
                if (attributeToImport.getValue() != null) {
                    try {
                        dateValue = SDF.parse(attributeToImport.getValue());
                    } catch (ParseException e) {
                        LOGGER.log(Level.SEVERE, "Cannot parse date : " + attributeToImport.getValue(), e);
                        errors.add(createError(properties, "UnparseableDateValue", attributeToImport.getValue()));
                        return;
                    }
                }
                attribute.setValue(dateValue);
                break;
            case "BOOLEAN":
                attribute.setValue(Boolean.parseBoolean(attributeToImport.getValue()));
                break;
            case "URL":
                attribute.setValue(attributeToImport.getValue());
                break;
            case "LOV":
                InstanceListOfValuesAttribute lov = (InstanceListOfValuesAttribute) attribute;
                int index = indexOfValue(lov.getItems(), attributeToImport.getValue());
                if (index == -1) {
                    errors.add(createError(properties, "LovValueNotFound", attributeToImport.getValue()));
                    return;
                }
                attribute.setValue(index);
                break;
        }
    }

    public static int indexOfValue(List<NameValuePair> items, String name) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }


    public static  InstanceAttribute createAttribute(ILOVManagerLocal lovManager, Properties properties, Attribute attribute, String workspace,List<String> errors) {

        final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        InstanceAttribute newAttribute;

        switch (attribute.getModel().getType()) {
            case "TEXT":
                newAttribute = new InstanceTextAttribute(attribute.getModel().getName(), attribute.getValue(), false);
                break;
            case "NUMBER":
                newAttribute = new InstanceNumberAttribute(attribute.getModel().getName(), Float.parseFloat(attribute.getValue()), false);
                break;
            case "DATE":
                try {
                    Date d = SDF.parse(attribute.getValue());
                    newAttribute = new InstanceDateAttribute(attribute.getModel().getName(), d, false);
                } catch (ParseException e) {
                    LOGGER.log(Level.SEVERE, "Cannot parse date : " + attribute.getValue(), e);
                    //should throw an exception and rollback or notify the user.
                    newAttribute = null;
                }
                break;
            case "BOOLEAN":
                newAttribute = new InstanceBooleanAttribute(attribute.getModel().getName(), Boolean.parseBoolean(attribute.getValue()), false);
                break;
            case "URL":
                newAttribute = new InstanceURLAttribute(attribute.getModel().getName(), attribute.getValue(), false);
                break;
            case "LOV":
                try {
                    ListOfValues lov = lovManager.findLov(new ListOfValuesKey(workspace, attribute.getModel().getLovName()));
                    int index = indexOfValue(lov.getValues(), attribute.getValue());
                    if (index == -1) {
                        errors.add(createError(properties, "LovValueNotFound",attribute.getValue()));
                    }
                    InstanceListOfValuesAttribute instanceLov = new InstanceListOfValuesAttribute(attribute.getModel().getName(), index, false);
                    instanceLov.setItems(lov.getValues());
                    //Have to set the index value again, since the items were previously empty and the function setIndexValue forbid the index to be superior than the size of items
                    instanceLov.setIndexValue(index);
                    newAttribute = instanceLov;
                } catch (ListOfValuesNotFoundException | UserNotFoundException | UserNotActiveException | WorkspaceNotFoundException | WorkspaceNotEnabledException e) {
                    LOGGER.log(Level.SEVERE, "Cannot find lov : " + attribute.getValue(), e);
                    if (!errors.contains(e.getLocalizedMessage())) errors.add(e.getLocalizedMessage());
                    newAttribute = null;
                }
                break;
            case "LONG_TEXT":
                newAttribute = new InstanceLongTextAttribute(attribute.getModel().getName(), attribute.getValue(), false);
                break;
            default:
                // this state should never be reached
                //should throw an exception and rollback or notify the user.
                newAttribute = null;
                break;
        }

        return newAttribute;
    }



    public static  InstanceAttribute findAttribute(List<InstanceAttribute> instanceAttributes, Attribute attributeToFind) {
        int index = getIndexOfAttribute(instanceAttributes, attributeToFind.getModel().getName(), attributeToFind.getModel().getType());
        return index == -1 ? null : instanceAttributes.get(index);
    }

    public static  String getTypeOfInstanceAttribute(InstanceAttribute instanceAttribute){

        if(instanceAttribute instanceof InstanceDateAttribute){
            return "DATE";
        }
        else if(instanceAttribute instanceof InstanceNumberAttribute){
            return "NUMBER";
        }
        else if(instanceAttribute instanceof InstanceTextAttribute){
            return "TEXT";
        }
        else if(instanceAttribute instanceof InstanceURLAttribute){
            return "URL";
        }
        else if(instanceAttribute instanceof InstanceBooleanAttribute){
            return "BOOLEAN";
        }
        else if(instanceAttribute instanceof InstanceListOfValuesAttribute){
            return "LOV";
        }
        else if(instanceAttribute instanceof InstanceLongTextAttribute){
            return "LONG_TEXT";
        }
        else return null;
    }

    public static  int getIndexOfAttribute(List<InstanceAttribute> instanceAttributes, String name, String type) {
        int index = 0;

        for (InstanceAttribute instanceAttribute : instanceAttributes) {

            if (instanceAttribute.getName().equals(name) && (Objects.equals(getTypeOfInstanceAttribute(instanceAttribute),type))) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static  boolean removeAttribute(List<InstanceAttribute> instanceAttributes, InstanceAttribute attribute) {
        int index = getIndexOfAttribute(instanceAttributes, attribute.getName(), getTypeOfInstanceAttribute(attribute));
        return index != -1 && instanceAttributes.remove(index) != null;
    }


    public static  boolean checkIfUpdateOrCreateInstanceAttributes(List<Attribute> attributes, List<InstanceAttribute> currentInstanceAttributes) {

        for (Attribute attribute : attributes) {

            InstanceAttribute currentAttribute = findAttribute(currentInstanceAttributes, attribute);

            if (attribute.getId()!=null) {
                if (currentAttribute != null) {
                    // Update case
                    return true;
                }
            } else {
                if(currentAttribute == null){
                    // Creation case
                    return true;
                }
            }

        }

        return false;
    }

}
