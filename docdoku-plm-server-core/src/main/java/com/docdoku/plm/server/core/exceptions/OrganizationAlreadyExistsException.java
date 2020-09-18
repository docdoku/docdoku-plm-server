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

package com.docdoku.plm.server.core.exceptions;

import com.docdoku.plm.server.core.common.Organization;

import java.text.MessageFormat;


/**
 *
 * @author Florent Garin
 */
public class OrganizationAlreadyExistsException extends EntityAlreadyExistsException {
    private final Organization mOrganization;


    public OrganizationAlreadyExistsException(String pMessage) {
        super(pMessage);
        mOrganization=null;
    }

    public OrganizationAlreadyExistsException(Organization pOrganization) {
        this(pOrganization, null);
    }

    public OrganizationAlreadyExistsException(Organization pOrganization, Throwable pCause) {
        super( pCause);
        mOrganization=pOrganization;
    }

    @Override
    public String getLocalizedMessage() {
        String message = getBundleDefaultMessage();
        return MessageFormat.format(message,mOrganization);
    }
}
