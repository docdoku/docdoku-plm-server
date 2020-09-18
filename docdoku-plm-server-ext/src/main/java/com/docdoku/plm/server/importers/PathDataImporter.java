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

package com.docdoku.plm.server.importers;

import javax.ejb.Remote;
import java.io.File;
import java.util.Locale;

/**
 * PathDataImporter plugin interface
 */
@Remote
public interface PathDataImporter {

    /**
     * Determine if plugin is able to import the given file format
     *
     * @param importFileName the file name
     * @return true if plugin can handle the import, false otherwise
     */
    boolean canImportFile(String importFileName);

    /**
     * Import the file and make requested changes
     *
     * @param workspaceId           the workspace concerned by the import
     * @param file                  the file to import
     * @param autoFreezeAfterUpdate todo
     * @param permissiveUpdate      todo
     * @return an import result object
     */
    PathDataImporterResult importFile(Locale locale, String workspaceId, File file, boolean autoFreezeAfterUpdate, boolean permissiveUpdate);
}
