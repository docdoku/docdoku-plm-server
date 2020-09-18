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
import com.docdoku.plm.server.core.document.DocumentRevisionKey;

import java.text.MessageFormat;


/**
 *
 * @author Florent Garin
 */
public class DocumentRevisionNotFoundException extends EntityNotFoundException {
    private final String mDocMId;
    private final String mDocRStringVersion;

    public DocumentRevisionNotFoundException(String pMessage) {
        super(pMessage);
        mDocMId=null;
        mDocRStringVersion=null;
    }

    public DocumentRevisionNotFoundException(DocumentRevisionKey pDocRPK) {
        this(pDocRPK, null);
    }

    public DocumentRevisionNotFoundException(DocumentRevisionKey pDocRPK, Throwable pCause) {
        this(pDocRPK.getDocumentMaster().getId(), pDocRPK.getVersion(), pCause);
    }

    public DocumentRevisionNotFoundException(String pDocMID, Version pDocRVersion) {
        this(pDocMID, pDocRVersion.toString(), null);
    }

    public DocumentRevisionNotFoundException(String pDocMId, String pDocRStringVersion) {
        this(pDocMId, pDocRStringVersion, null);
    }

    public DocumentRevisionNotFoundException(String pDocMId, String pDocRStringVersion, Throwable pCause) {
        super( pCause);
        mDocMId=pDocMId;
        mDocRStringVersion=pDocRStringVersion;
    }

    @Override
    public String getLocalizedMessage() {
        String message = getBundleDefaultMessage();
        return MessageFormat.format(message,mDocMId,mDocRStringVersion);
    }
}
