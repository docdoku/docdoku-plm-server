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

package com.docdoku.plm.server.rest.converters;

import org.dozer.DozerConverter;
import com.docdoku.plm.server.core.meta.DefaultAttributeTemplate;
import com.docdoku.plm.server.core.meta.InstanceAttributeTemplate;
import com.docdoku.plm.server.core.meta.ListOfValuesAttributeTemplate;
import com.docdoku.plm.server.core.product.PartNumberAttributeTemplate;
import com.docdoku.plm.server.rest.dto.InstanceAttributeTemplateDTO;
import com.docdoku.plm.server.rest.dto.InstanceAttributeType;

/**
 * @author Florent Garin
 */
public class InstanceAttributeTemplateDozerConverter extends DozerConverter<InstanceAttributeTemplate, InstanceAttributeTemplateDTO> {

    public InstanceAttributeTemplateDozerConverter() {
        super(InstanceAttributeTemplate.class, InstanceAttributeTemplateDTO.class);
    }


    @Override
    public InstanceAttributeTemplateDTO convertTo(InstanceAttributeTemplate instanceAttributeTemplate, InstanceAttributeTemplateDTO dto) {
        if(dto==null)
            dto=new InstanceAttributeTemplateDTO();

        dto.setLocked(instanceAttributeTemplate.isLocked());
        dto.setName(instanceAttributeTemplate.getName());
        dto.setMandatory(instanceAttributeTemplate.isMandatory());
        if(instanceAttributeTemplate instanceof DefaultAttributeTemplate){
            DefaultAttributeTemplate defaultIA = (DefaultAttributeTemplate)instanceAttributeTemplate;
            dto.setAttributeType(InstanceAttributeType.valueOf(defaultIA.getAttributeType().name()));

        }else if(instanceAttributeTemplate instanceof ListOfValuesAttributeTemplate){
            ListOfValuesAttributeTemplate lovIA=(ListOfValuesAttributeTemplate)instanceAttributeTemplate;
            dto.setLovName(lovIA.getLovName());
            dto.setAttributeType(InstanceAttributeType.LOV);
        }else if(instanceAttributeTemplate instanceof PartNumberAttributeTemplate){
            dto.setAttributeType(InstanceAttributeType.PART_NUMBER);
        }
        return dto;
    }

    @Override
    public InstanceAttributeTemplate convertFrom(InstanceAttributeTemplateDTO dto, InstanceAttributeTemplate instanceAttributeTemplate) {

        InstanceAttributeTemplate data;
        if (InstanceAttributeType.LOV.equals(dto.getAttributeType())) {
            data = new ListOfValuesAttributeTemplate();
        }else if (InstanceAttributeType.PART_NUMBER.equals(dto.getAttributeType())){
            data = new PartNumberAttributeTemplate();
        }
        else {
            DefaultAttributeTemplate defaultIA = new DefaultAttributeTemplate();
            defaultIA.setAttributeType(DefaultAttributeTemplate.AttributeType.valueOf(dto.getAttributeType().name()));
            data = defaultIA;
        }

        data.setName(dto.getName());
        data.setMandatory(dto.isMandatory());
        data.setLocked(dto.isLocked());
        return data;


    }
}
