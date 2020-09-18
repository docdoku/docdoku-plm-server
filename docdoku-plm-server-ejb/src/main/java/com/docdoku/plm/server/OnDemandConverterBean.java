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
package com.docdoku.plm.server;

import com.docdoku.plm.server.core.common.BinaryResource;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.services.IOnDemandConverterManagerLocal;
import com.docdoku.plm.server.converters.OnDemandConverter;
import com.docdoku.plm.server.dao.BinaryResourceDAO;

import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.Locale;


/**
 * Resource Getter
 */

@Stateless(name = "OnDemandConverterBean")
public class OnDemandConverterBean implements IOnDemandConverterManagerLocal {

    @Inject
    private BinaryResourceDAO binaryResourceDAO;

    @Inject
    @Any
    private Instance<OnDemandConverter> documentResourceGetters;

    @Override
    public InputStream getDocumentConvertedResource(String outputFormat, BinaryResource binaryResource, Locale locale)
            throws WorkspaceNotFoundException, UserNotActiveException, UserNotFoundException, ConvertedResourceException, WorkspaceNotEnabledException {

        DocumentIteration docI = binaryResourceDAO.getDocumentHolder(binaryResource);
        OnDemandConverter selectedOnDemandConverter = selectOnDemandConverter(outputFormat, binaryResource);
        if (selectedOnDemandConverter != null) {
            return selectedOnDemandConverter.getConvertedResource(outputFormat, binaryResource, docI, locale);
        }

        return null;
    }

    @Override
    public InputStream getPartConvertedResource(String outputFormat, BinaryResource binaryResource, Locale locale)
            throws WorkspaceNotFoundException, UserNotActiveException, UserNotFoundException, ConvertedResourceException, WorkspaceNotEnabledException {

        PartIteration partIteration = binaryResourceDAO.getPartHolder(binaryResource);
        OnDemandConverter selectedOnDemandConverter = selectOnDemandConverter(outputFormat, binaryResource);

        if (selectedOnDemandConverter != null) {
            return selectedOnDemandConverter.getConvertedResource(outputFormat, binaryResource, partIteration, locale);
        }

        return null;
    }

    private OnDemandConverter selectOnDemandConverter(String outputFormat, BinaryResource binaryResource) {
        OnDemandConverter selectedOnDemandConverter = null;
        for (OnDemandConverter onDemandConverter : documentResourceGetters) {
            if (onDemandConverter.canConvert(outputFormat, binaryResource)) {
                selectedOnDemandConverter = onDemandConverter;
                break;
            }
        }
        return selectedOnDemandConverter;
    }
}
