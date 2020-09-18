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
import com.docdoku.plm.server.core.product.PartIterationKey;

import java.text.MessageFormat;


/**
 *
 * @author Florent Garin
 */
public class PartIterationNotFoundException extends EntityNotFoundException {
    private final String mPartMNumber;
    private final String mPartRStringVersion;
    private final int mPartIIteration;

    public PartIterationNotFoundException(String pMessage) {
        super(pMessage);
        mPartMNumber=null;
        mPartRStringVersion=null;
        mPartIIteration=-1;
    }

    public PartIterationNotFoundException(PartIterationKey pPartIPK) {
        this(pPartIPK, null);
    }

    public PartIterationNotFoundException(PartIterationKey pPartIPK, Throwable pCause) {
        this(pPartIPK.getPartMasterNumber(), pPartIPK.getPartRevision().getVersion(), pPartIPK.getIteration(), pCause);
    }

    public PartIterationNotFoundException(String pPartMNumber, Version pPartRVersion, int pPartIIteration) {
        this(pPartMNumber, pPartRVersion.toString(), pPartIIteration, null);
    }

    public PartIterationNotFoundException(String pPartMNumber, String pPartRStringVersion, int pPartIIteration) {
        this(pPartMNumber, pPartRStringVersion, pPartIIteration, null);
    }

    public PartIterationNotFoundException(String pPartMNumber, String pPartRStringVersion, int pPartIIteration, Throwable pCause) {
        super( pCause);
        mPartMNumber=pPartMNumber;
        mPartRStringVersion=pPartRStringVersion;
        mPartIIteration=pPartIIteration;
    }

    @Override
    public String getLocalizedMessage() {
        String message = getBundleDefaultMessage();
        return MessageFormat.format(message,mPartMNumber,mPartRStringVersion, mPartIIteration);
    }
}
