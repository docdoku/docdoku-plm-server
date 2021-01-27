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

package com.docdoku.plm.server.rest.locale;


import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.exceptions.AccountNotFoundException;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IAccountManagerLocal;
import com.docdoku.plm.server.core.services.IContextManagerLocal;
import com.docdoku.plm.server.i18n.PropertiesLoader;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
@RequestScoped
public class LocaleProvider {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final Locale DEFAULT = new Locale(DEFAULT_LANGUAGE);
    private static final Logger LOGGER = Logger.getLogger(LocaleProvider.class.getName());

    @Context
    private HttpServletRequest httpRequest;

    private Locale locale;

    public LocaleProvider() {
    }

    @PostConstruct
    public void init() {
        try {
            InitialContext ctx = new InitialContext();
            IContextManagerLocal contextManager = (IContextManagerLocal) ctx.lookup("java:app/docdoku-plm-server-ejb/ContextManagerBean!com.docdoku.plm.server.core.services.IContextManagerLocal");
            if (contextManager.isCallerInRole(UserGroupMapping.REGULAR_USER_ROLE_ID) || contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)) {
                IAccountManagerLocal accountManager = (IAccountManagerLocal) ctx.lookup("java:app/docdoku-plm-server-ejb/AccountManagerBean!com.docdoku.plm.server.core.services.IAccountManagerLocal");
                try {
                    Account myAccount = accountManager.getMyAccount();
                    locale = myAccount.getLocale();
                } catch (AccountNotFoundException e) {
                    LOGGER.log(Level.FINE, null, e);
                    locale = defaults();
                }
            } else {
                locale = defaults();
            }
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Cannot initialize LocaleProvider", e);
        }
    }

    @Produces
    public Locale userLocale() {
        return locale;
    }

    private Locale defaults() {
        Locale locale = httpRequest.getLocale();
        String language = locale.getLanguage();
        return PropertiesLoader.getSupportedLanguages().contains(language) ? new Locale(language) : DEFAULT;
    }

}
