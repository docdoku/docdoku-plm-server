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
import com.docdoku.plm.server.core.product.ConversionResult;
import com.docdoku.plm.server.rest.dto.ConversionResultDTO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Morgan Guimard
 */
public class ConversionResultDozerConverter extends DozerConverter<ConversionResult, ConversionResultDTO> {

    public ConversionResultDozerConverter() {
        super(ConversionResult.class, ConversionResultDTO.class);
    }

    @Override
    public ConversionResultDTO convertTo(ConversionResult conversionResult, ConversionResultDTO pConversionResultDTO) {
        return new ConversionResultDTO();
    }

    @Override
    public ConversionResult convertFrom(ConversionResultDTO conversionResultDTO, ConversionResult pConversionResult) {
        ConversionResult conversionResult = new ConversionResult();

        if( null != conversionResultDTO.getConvertedFileLODs()) {
            Map<Integer, Path> lods = new HashMap<>();
            conversionResultDTO.getConvertedFileLODs()
                    .forEach((key, value) -> lods.put(Integer.valueOf(key), Paths.get(value)));
            conversionResult.setConvertedFileLODs(lods);
         }

        conversionResult.setErrorOutput(conversionResultDTO.getErrorOutput());
        conversionResult.setStdOutput(conversionResultDTO.getStdOutput());

        if(null != conversionResultDTO.getMaterials()) {
            List<Path> materials = conversionResultDTO.getMaterials().stream().map(s -> Paths.get(s)).collect(Collectors.toList());
            conversionResult.setMaterials(materials);
        }

        if( null != conversionResultDTO.getComponentPositionMap()){
            Map<String, List<ConversionResultDTO.PositionDTO>> componentPositionMap = conversionResultDTO.getComponentPositionMap();
            Map<String, List<ConversionResult.Position>> positionMap = new HashMap<>();

            for(Map.Entry<String, List<ConversionResultDTO.PositionDTO>> entry : componentPositionMap.entrySet()){
                String key = entry.getKey();
                List<ConversionResultDTO.PositionDTO> value = entry.getValue();
                List<ConversionResult.Position> positions = value.stream().map(v -> new ConversionResult.Position(v.getRotationmatrix(), v.getTranslation())).collect(Collectors.toList());
                positionMap.put(key,positions);
            }
            conversionResult.setComponentPositionMap(positionMap);
        }

        if(conversionResultDTO.getTempDir() != null) {
            conversionResult.setTempDir(Paths.get(conversionResultDTO.getTempDir()));
        }
        return conversionResult;
    }
}
