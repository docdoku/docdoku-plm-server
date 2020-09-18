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

import org.jodconverter.office.OfficeException;
import org.jodconverter.office.OfficeManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.exceptions.FileNotFoundException;
import com.docdoku.plm.server.core.exceptions.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

@RunWith(MockitoJUnitRunner.class)
public class FileConverterTest {

    @InjectMocks
    private FileConverter fileConverter;

    @Mock
    private OfficeConfig officeConfig;

    @Mock
    private OfficeManager officeManager;

    @Before
    public void setup() throws OfficeException {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void convertToPDFTest() throws StorageException, IOException, OfficeException, FileNotFoundException, URISyntaxException {

        try(InputStream resourceAsStream = getClass().getResourceAsStream("/com/docdoku/plm/server/resourcegetters/sample.txt")){

            Assert.assertNotNull(resourceAsStream);
            try{

                fileConverter.convertToPDF("sample.txt", resourceAsStream);
                Assert.fail("Should have thrown an IOException");

            }catch (IOException e ){

                Mockito.verify(officeManager, Mockito.times(1)).execute(ArgumentMatchers.any());
            }

        }catch (IOException e ){

            Assert.fail("Resource not found: " + e.getMessage());
        }
    }
}