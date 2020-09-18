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

import com.docdoku.plm.server.core.document.DocumentIterationKey;

import java.text.MessageFormat;

/**
 *
 * @author Morgan Guimard
 */
public class DocumentIterationNotFoundException extends EntityNotFoundException {
    private final String mDocMId;
    private final String mDocRStringVersion;
    private final Integer mDocIStringIteration;

    public DocumentIterationNotFoundException(String pMessage) {
        super(pMessage);
        mDocMId=null;
        mDocRStringVersion=null;
        mDocIStringIteration=null;
    }

    public DocumentIterationNotFoundException(DocumentIterationKey pKey) {
        this(pKey, null);
    }

    public DocumentIterationNotFoundException(DocumentIterationKey pKey, Throwable pCause) {
        this(pKey.getDocumentMasterId(), pKey.getDocumentRevisionVersion(), pKey.getIteration(),  pCause);
    }

    public DocumentIterationNotFoundException(String pDocMId,
                                              String pDocRStringVersion,
                                              int pDocIStringIteration,
                                              Throwable pCause) {
        super(pCause);
        mDocMId=pDocMId;
        mDocRStringVersion=pDocRStringVersion;
        mDocIStringIteration=pDocIStringIteration;
    }

    @Override
    public String getLocalizedMessage() {
        String message = getBundleDefaultMessage();
        return MessageFormat.format(message,mDocMId,mDocRStringVersion,mDocIStringIteration);
    }
}
