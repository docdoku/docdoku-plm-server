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


import com.docdoku.plm.server.core.common.Version;
import com.docdoku.plm.server.core.product.PartRevisionKey;

import java.text.MessageFormat;


/**
 *
 * @author Florent Garin
 */
public class PartRevisionNotFoundException extends EntityNotFoundException {
    private final String mPartMNumber;
    private final String mPartRStringVersion;

    public PartRevisionNotFoundException(String pMessage) {
        super(pMessage);
        mPartMNumber=null;
        mPartRStringVersion=null;
    }

    public PartRevisionNotFoundException(PartRevisionKey pPartRPK) {
        this(pPartRPK, null);
    }

    public PartRevisionNotFoundException(PartRevisionKey pPartRPK, Throwable pCause) {
        this(pPartRPK.getPartMaster().getNumber(), pPartRPK.getVersion(), pCause);
    }

    public PartRevisionNotFoundException(String pPartMNumber, Version pPartRVersion) {
        this(pPartMNumber, pPartRVersion.toString(), null);
    }

    public PartRevisionNotFoundException(String pPartMNumber, String pPartRStringVersion) {
        this(pPartMNumber, pPartRStringVersion, null);
    }

    public PartRevisionNotFoundException(String pPartMNumber, String pPartRStringVersion, Throwable pCause) {
        super( pCause);
        mPartMNumber=pPartMNumber;
        mPartRStringVersion=pPartRStringVersion;
    }

    @Override
    public String getLocalizedMessage() {
        String message = getBundleDefaultMessage();
        return MessageFormat.format(message,mPartMNumber,mPartRStringVersion);     
    }
}
