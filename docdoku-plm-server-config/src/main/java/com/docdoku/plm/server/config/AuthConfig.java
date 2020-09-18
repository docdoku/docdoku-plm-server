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

import org.jose4j.keys.HmacKey;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.crypto.KeyGenerator;
import javax.ejb.Singleton;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get auth config from resources
 *
 * @author Morgan Guimard
 */
@Singleton
public class AuthConfig {

    @Resource(lookup="auth.config")
    private Properties properties;

    private Key defaultKey;

    private static final Logger LOGGER = Logger.getLogger(AuthConfig.class.getName());

    @PostConstruct
    private void init() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            defaultKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Cannot generate random JWT default key", e);
        }
    }

    public Boolean isJwtEnabled() {
        return Boolean.parseBoolean(properties.getProperty("jwtEnabled"));
    }

    public Boolean isBasicHeaderEnabled() {
        return Boolean.parseBoolean(properties.getProperty("basicHeaderEnabled"));
    }

    public Boolean isSessionEnabled() {
        return Boolean.parseBoolean(properties.getProperty("sessionEnabled"));
    }

    public Key getJWTKey() {
        String secret = properties.getProperty("jwtKey");

        if (null != secret && !secret.isEmpty()) {
            try {
                return new HmacKey(secret.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                    LOGGER.log(Level.SEVERE, "Cannot create JWT key", e);
            }
        }

        return defaultKey;
    }
}
