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

package com.docdoku.plm.server.core.services;

import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.OAuthProvider;
import com.docdoku.plm.server.core.common.ProvidedAccount;
import com.docdoku.plm.server.core.exceptions.AccountNotFoundException;
import com.docdoku.plm.server.core.exceptions.OAuthProviderNotFoundException;
import com.docdoku.plm.server.core.exceptions.ProvidedAccountNotFoundException;

import java.util.List;

/**
 * @author Morgan Guimard
 */
public interface IOAuthManagerLocal {

    List<OAuthProvider> getProviders();

    OAuthProvider getProvider(int id) throws OAuthProviderNotFoundException;

    OAuthProvider createProvider(String name, boolean enabled, String authority, String issuer,
                                 String clientID, String jwsAlgorithm, String jwkSetURL, String redirectUri,
                                 String secret, String scope, String responseType, String authorizationEndpoint) throws AccountNotFoundException;

    OAuthProvider updateProvider(int id, String name, boolean enabled, String authority, String issuer,
                                 String clientID, String jwsAlgorithm, String jwkSetURL, String redirectUri,
                                 String secret, String scope, String responseType, String authorizationEndpoint) throws AccountNotFoundException, OAuthProviderNotFoundException;

    void deleteProvider(int id) throws AccountNotFoundException, OAuthProviderNotFoundException;

    ProvidedAccount getProvidedAccount(int providerId, String sub) throws ProvidedAccountNotFoundException;


    void createProvidedAccount(Account account, String sub, OAuthProvider provider);

    boolean isProvidedAccount(Account account);

    Integer getProviderId(Account account);

    String findAvailableLogin(String sub);

    void loadProvidersFromProperties();
}
