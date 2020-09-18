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


import com.docdoku.plm.server.core.common.BinaryResource;
import com.docdoku.plm.server.core.product.PartIterationKey;

import java.io.Serializable;

public class ConversionOrder implements Serializable {

    private PartIterationKey partIterationKey;

    private BinaryResource binaryResource;

    private String userToken;

    public ConversionOrder(PartIterationKey partIterationKey, BinaryResource binaryResource, String userToken) {
        this.partIterationKey = partIterationKey;
        this.binaryResource = binaryResource;
        this.userToken = userToken;
    }

    public ConversionOrder() {
    }

    public PartIterationKey getPartIterationKey() {

        return partIterationKey;
    }

    public void setPartIterationKey(PartIterationKey partIterationKey) {
        this.partIterationKey = partIterationKey;
    }

    public BinaryResource getBinaryResource() {
        return binaryResource;
    }

    public void setBinaryResource(BinaryResource binaryResource) {
        this.binaryResource = binaryResource;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }
}
