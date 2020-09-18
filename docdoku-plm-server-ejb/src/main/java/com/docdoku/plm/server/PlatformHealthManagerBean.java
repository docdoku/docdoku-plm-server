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

package com.docdoku.plm.server;

import com.docdoku.plm.server.core.exceptions.PlatformHealthException;
import com.docdoku.plm.server.core.services.IBinaryStorageManagerLocal;
import com.docdoku.plm.server.core.services.IIndexerManagerLocal;
import com.docdoku.plm.server.core.services.IPlatformHealthManagerLocal;
import com.docdoku.plm.server.config.ServerConfig;
import com.docdoku.plm.server.resourcegetters.OfficeConfig;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

@Local(IPlatformHealthManagerLocal.class)
@Stateless(name = "PlatformHealthManagerBean")
public class PlatformHealthManagerBean implements IPlatformHealthManagerLocal {

    private static final Logger LOGGER = Logger.getLogger(PlatformHealthManagerBean.class.getName());

    @Inject
    private EntityManager em;

    @Inject
    private IIndexerManagerLocal indexerManager;

    @Inject
    private IBinaryStorageManagerLocal storageManager;

    @Inject
    private OfficeConfig officeConfig;

    @Inject
    private ServerConfig serverConfig;

    @Override
    public void runHealthCheck() throws PlatformHealthException {

        boolean check = true;

        // Database check
        try {
            Integer two = (Integer) em.createNativeQuery("select 1+1").getSingleResult();
            if (two != 2) {
                LOGGER.log(Level.SEVERE, "Database doesn't seem to be reachable");
                check = false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database doesn't seem to be reachable", e);
            check = false;
        }

        // Indexer check
        boolean ping = indexerManager.ping();
        if (!ping) {
            LOGGER.log(Level.SEVERE, "Indexer doesn't seem to be reachable");
            check = false;
        }

        // LibreOffice check
        String officeHome = null;
        try {
            officeHome = officeConfig.getOfficeHome();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Office properties object not accessible", e);
            check = false;
        }

        if (officeHome == null || officeHome.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Office is not configured");
            check = false;
        } else {
            Path path = Paths.get(officeHome);

            if (!path.toFile().exists()) {
                LOGGER.log(Level.SEVERE, "Office is not available for given path: " + officeHome);
                check = false;
            }

            if (!path.toFile().canExecute()) {
                LOGGER.log(Level.SEVERE, "Office is not executable");
                check = false;
            }
        }

        try {
            Integer officePort = officeConfig.getOfficePort();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Office port incorrectly set", e);
            check = false;
        }

        // Check for mandatory config
        String vaultPath = serverConfig.getVaultPath();

        if (vaultPath == null || vaultPath.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Vaultpath is not set, you won't be able to upload/download files");
            check = false;
        } else {
            Path path = Paths.get(vaultPath);

            if (!path.toFile().exists()) {
                LOGGER.log(Level.SEVERE, "Vaultpath is not available for given path: " + vaultPath);
                check = false;
            }

            if (!path.toFile().canWrite()) {
                LOGGER.log(Level.SEVERE, "Vaultpath is not writeable, please set appropriate rights on " + vaultPath);
                check = false;
            }

        }

        // Check for optional config
        String codebase = serverConfig.getCodebase();
        if (codebase == null || codebase.isEmpty()) {
            LOGGER.log(Level.WARNING, "Codebase is not set, if you are using docdoku-web-front with this server you should configure it");
        }

        if (!check) {
            LOGGER.log(Level.SEVERE, "Health check didn't pass");
            throw new PlatformHealthException();
        }

    }
}
