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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get config from resources
 *
 * @author Morgan Guimard
 */
@Singleton
public class SecurityConfig {

    @Resource(lookup="security.config")
    private Properties properties;

    private static final Logger LOGGER = Logger.getLogger(SecurityConfig.class.getName());

    private Key key;

    @PostConstruct
    void init(){
        try{
            KeyStore ks = KeyStore.getInstance(getKeyType());
            try (InputStream fis = new BufferedInputStream(new FileInputStream(getKeystoreLocation()))) {
                ks.load(fis, getKeystorePass().toCharArray());
            }
            key = ks.getKey(getKeyAlias(), getKeyPass().toCharArray());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Keystore loading failed", ex);
        }
    }

    private String getKeystoreLocation(){
        return properties.getProperty("keystoreLocation");
    }

    private String getKeystorePass(){
        return properties.getProperty("keystorePass");
    }

    private String getKeyAlias(){
        return properties.getProperty("keyAlias");
    }

    private String getKeyType(){
        return  Optional.ofNullable(properties.getProperty("keystoreType")).orElse("JCEKS");
    }

    private String getKeyPass(){
        return  Optional.ofNullable(properties.getProperty("keyPass")).orElse(getKeystorePass());
    }

    public Key getKey(){
        return key;
    }

}
