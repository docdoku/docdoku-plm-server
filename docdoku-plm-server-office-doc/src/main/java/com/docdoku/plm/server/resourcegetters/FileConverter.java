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

package com.docdoku.plm.server.resourcegetters;

import org.jodconverter.OfficeDocumentConverter;
import org.jodconverter.office.LocalOfficeManager;
import org.jodconverter.office.OfficeException;
import org.jodconverter.office.OfficeManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class FileConverter {

    private final OfficeConfig officeConfig;
    private static final Logger LOGGER = Logger.getLogger(FileConverter.class.getName());

    @Inject
    public FileConverter(OfficeConfig officeConfig) {
        this.officeConfig = officeConfig;
    }

    private OfficeManager officeManager;

    @PostConstruct
    private void init() {
        officeManager = LocalOfficeManager.builder()
                .officeHome(new File(officeConfig.getOfficeHome()))
                .portNumbers(officeConfig.getOfficePort())
                .build();
        try {
            officeManager.start();
        } catch (OfficeException e) {

            LOGGER.log(Level.SEVERE, "Office manager not started : "+e);
        }
    }

    @PreDestroy
    private void close() {

        try {
            officeManager.stop();
        } catch (OfficeException e) {

            LOGGER.log(Level.SEVERE,"Office manager not stopped : "+e);
        }
    }

    public synchronized InputStream convertToPDF(String sourceName, final InputStream streamToConvert) throws IOException, OfficeException {
        File tmpDir = Files.createTempDirectory("docdoku-").toFile();
        File fileToConvert = new File(tmpDir, sourceName);

        Files.copy(streamToConvert, fileToConvert.toPath());

        File pdfFile = convertToPDF(fileToConvert);

        //clean-up
        tmpDir.deleteOnExit();

        return new FileInputStream(pdfFile);
    }

    private File convertToPDF(File fileToConvert) throws OfficeException {
        File pdfFile = new File(fileToConvert.getParentFile(), "converted.pdf");
        OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
        converter.convert(fileToConvert, pdfFile);
        return pdfFile;
    }
}
