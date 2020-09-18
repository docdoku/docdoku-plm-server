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

package com.docdoku.plm.server.converters;

import com.docdoku.plm.server.core.product.ConversionResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ConversionResultProxy extends ConversionResult {

    private Path convertedFile;

    public ConversionResultProxy() {
        super();
    }

    public ConversionResultProxy(Path convertedFile) {
        super();
        this.convertedFile  = convertedFile;
    }

    public ConversionResultProxy(Path convertedFile, List<Path> materials) {
        super();
        this.setConvertedFile(convertedFile);
        this.setMaterials(materials);
    }

    public ConversionResultProxy(Map<String, List<Position>> componentPositionMap) {
        super();
        this.setComponentPositionMap(componentPositionMap);
    }

    public Path getConvertedFile() {
        return convertedFile;
    }

    public void setConvertedFile(Path convertedFile) {
        this.convertedFile = convertedFile;
    }
}
