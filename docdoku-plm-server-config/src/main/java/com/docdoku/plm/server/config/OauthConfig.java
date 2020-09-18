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

package com.docdoku.plm.server.config;

import com.docdoku.plm.server.core.common.OAuthProvider;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Get oauth config from resources
 * <p>
 * Insert oAuth provider config with ./asadmin commands, before the server starts
 *
 * @author Morgan Guimard
 */
@Singleton
public class OauthConfig {

    private static final Logger LOGGER = Logger.getLogger(OauthConfig.class.getName());

    @Resource(lookup = "oauth.config")
    private Properties properties;

    public List<OAuthProvider> getProviders() {

        if (properties == null || properties.isEmpty()) {
            LOGGER.log(Level.INFO, "No providers configured, skipping task.");
            return new ArrayList<>();
        }

        List<String> keys = properties.keySet().stream().map(Object::toString).collect(Collectors.toList());
        Map<Integer, OAuthProvider> providers = new HashMap<>();


        for (String k : keys) {

            String[] splitKey = k.split("/");

            if (splitKey.length < 2) {
                LOGGER.log(Level.SEVERE, "Cannot parse provider id: " + k);
                break;
            }

            Integer id;

            try {
                id = Integer.valueOf(splitKey[0]);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "Cannot parse config key: " + k);
                break;
            }

            OAuthProvider oAuthProvider;

            if (!providers.containsKey(id)) {
                oAuthProvider = new OAuthProvider();
                oAuthProvider.setId(id);
                providers.put(id, oAuthProvider);
            } else {
                oAuthProvider = providers.get(id);
            }

            String property = k.substring(2);
            String value = properties.getProperty(k);

            switch (property) {
                case "name":
                    oAuthProvider.setName(value);
                    break;
                case "enabled":
                    oAuthProvider.setEnabled(Boolean.parseBoolean(value));
                    break;
                case "authority":
                    oAuthProvider.setAuthority(value);
                    break;
                case "issuer":
                    oAuthProvider.setIssuer(value);
                    break;
                case "clientID":
                    oAuthProvider.setClientID(value);
                    break;
                case "jwsAlgorithm":
                    oAuthProvider.setJwsAlgorithm(value);
                    break;
                case "jwkSetURL":
                    oAuthProvider.setJwkSetURL(value);
                    break;
                case "redirectUri":
                    oAuthProvider.setRedirectUri(value);
                    break;
                case "secret":
                    oAuthProvider.setSecret(value);
                    break;
                case "scope":
                    oAuthProvider.setScope(value);
                    break;
                case "responseType":
                    oAuthProvider.setResponseType(value);
                    break;
                case "authorizationEndpoint":
                    oAuthProvider.setAuthorizationEndpoint(value);
                    break;
                default:
                    break;
            }

        }

        List<OAuthProvider> discoversProviders = providers.values().stream().collect(Collectors.toList());
        LOGGER.log(Level.INFO, discoversProviders.size() + " providers discovered");
        return discoversProviders;
    }
}
