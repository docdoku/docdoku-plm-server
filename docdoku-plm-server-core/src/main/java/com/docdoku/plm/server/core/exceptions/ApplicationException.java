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

import com.docdoku.plm.server.i18n.PropertiesLoader;

import java.util.Locale;
import java.util.Properties;

/**
 * Parent class for all application (non system) exceptions.
 *
 * @author Florent Garin
 */
public abstract class ApplicationException extends Exception {

    private static final String BUNDLE_BASE_NAME = "/com/docdoku/plm/server/core/i18n/LocalStrings";

    private Properties properties;

    public ApplicationException() {}

    public ApplicationException(String pMessage) {
        super(pMessage);
    }

    public ApplicationException(Throwable pCause) {
        super(pCause);
    }

    public ApplicationException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    protected String getBundleDefaultMessage() {
        return getBundleMessage(getClass().getSimpleName());
    }

    protected String getBundleMessage(String pKey) {
        if (null == properties) {
            setLocale(new Locale("en"));
        }
        return properties.getProperty(pKey);
    }

    public String getMessage(Locale locale){
        properties = PropertiesLoader.loadLocalizedProperties(locale, BUNDLE_BASE_NAME, getClass());
        return getMessage();
    }

    @Override
    public String getMessage() {
        String detailMessage = super.getMessage();
        return detailMessage == null ? getLocalizedMessage() : detailMessage;
    }

    @Override
    public abstract String getLocalizedMessage();

    @Override
    public String toString() {
        return getMessage();
    }

    private void setLocale(Locale locale) {
        properties = PropertiesLoader.loadLocalizedProperties(locale, BUNDLE_BASE_NAME, getClass());
    }
}
