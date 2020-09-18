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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import com.docdoku.plm.server.core.common.BinaryResource;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.config.AuthConfig;
import com.docdoku.plm.server.config.ServerConfig;
import com.docdoku.plm.server.converters.ConversionOrder;
import com.docdoku.plm.server.converters.serialization.JsonbSerializer;
import com.docdoku.plm.server.dao.PartRevisionDAO;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

/**
 * CAD File converter
 *
 * @author Florent.Garin
 */
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID})
@RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
@Local(IConverterManagerLocal.class)
@Stateless(name = "ConverterBean")
public class ConverterBean implements IConverterManagerLocal {

    @Inject
    private IProductManagerLocal productService;
    @Inject
    private ITokenManagerLocal tokenManager;
    @Inject
    private AuthConfig authConfig;
    @Inject
    private IContextManagerLocal contextManager;
    @Inject
    private PartRevisionDAO partRevisionDAO;
    @Inject
    private IUserManagerLocal userService;
    @Inject
    private IBinaryStorageManagerLocal storageManager;
    @Inject
    private ServerConfig serverConfig;

    private static final Logger LOGGER = Logger.getLogger(ConverterBean.class.getName());
    private static final String PRODUCER_TOPIC = "CONVERT";
    private KafkaProducer<String, ConversionOrder> producer;

    @PostConstruct
    public void init (){
        Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", "kafka:9092");
        producerProperties.put("acks", "0");
        producerProperties.put("retries", "1");
        producerProperties.put("batch.size", "20971520");
        producerProperties.put("linger.ms", "33");
        producerProperties.put("max.request.size", "2097152");
        producerProperties.put("compression.type", "gzip");
        producerProperties.put("key.serializer", StringSerializer.class.getName());
        producerProperties.put("value.serializer", JsonbSerializer.class.getName());
        producerProperties.put("kafka.topic", PRODUCER_TOPIC);

        producer = new KafkaProducer<>(producerProperties);
    }

    @Override
    @Asynchronous
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void convertCADFileToOBJ(PartIterationKey partIterationKey, BinaryResource cadBinaryResource) {

        try {

            Conversion existingConversion = productService.getConversion(partIterationKey);

            // Don't try to convert if any conversion pending
            if (existingConversion != null && existingConversion.isPending()) {
                LOGGER.log(Level.SEVERE, "Conversion already running for part iteration {0}", partIterationKey);
                return;
            }

            // Clean old non pending conversions
            if (existingConversion != null) {
                LOGGER.log(Level.FINE, "Cleaning previous ended conversion");
                productService.removeConversion(partIterationKey);
            }

        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return;
        }

        // Creates the new one
        try {
            LOGGER.log(Level.FINE, "Creating a new conversion");
            productService.createConversion(partIterationKey);

        } catch (ApplicationException e) {
            // Abort if any error (this should not happen though)
            LOGGER.log(Level.SEVERE, null, e);
            return;
        }

        // Send message in kafka queue
        String token = generateUserToken();
        ConversionOrder conversionOrder = new ConversionOrder(partIterationKey, cadBinaryResource, token);
        producer.send(new ProducerRecord<>(PRODUCER_TOPIC, partIterationKey.toString(), conversionOrder));

    }

    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public void handleConversionResultCallback(PartRevisionKey partRevisionKey, ConversionResult conversionResult) throws UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, EntityConstraintException, UserNotActiveException, PartRevisionNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, ListOfValuesNotFoundException, PartUsageLinkNotFoundException, PartMasterNotFoundException, PartIterationNotFoundException {

        userService.checkWorkspaceWriteAccess(partRevisionKey.getWorkspaceId());

        PartRevision partRevision = partRevisionDAO.loadPartR(partRevisionKey);
        Path tempDir = Paths.get(serverConfig.getConversionsPath() +  "/" + conversionResult.getTempDir());

        if(null == partRevision) {
            LOGGER.severe("Cannot find part revision");
            return;
        }

        PartIteration partIteration = partRevision.getLastIteration();
        PartIterationKey partIterationKey = partIteration.getKey();

        String errorOutput = conversionResult.getErrorOutput();

        if(null != errorOutput && !errorOutput.isEmpty()){
            LOGGER.severe("Conversion ended with errors: \n"+errorOutput);
            productService.endConversion(partIterationKey, false);
            return;
        }

        if(!partRevision.isCheckedOut()) {
            LOGGER.severe("Cannot proceed as the part is not checked out");
            productService.endConversion(partIterationKey, false);
            return;
        }

        Map<String, List<ConversionResult.Position>> componentPositionMap = conversionResult.getComponentPositionMap();
        Map<Integer, Path> convertedFileLODs = conversionResult.getConvertedFileLODs();

        // No CAD file and no position map
        if((convertedFileLODs == null || convertedFileLODs.isEmpty()) && componentPositionMap == null) {
            LOGGER.severe("Converted file and component position map are null, conversion failed \nError output: " + errorOutput);
            productService.endConversion(partIterationKey, false);
            return;
        }

        // Handle component map
        if (componentPositionMap != null && !syncAssembly(componentPositionMap, partIteration)) {
            LOGGER.severe("Failed to sync assembly, conversion failed");
            productService.endConversion(partIterationKey, false);
            return;
        }

        List<Path> materials = conversionResult.getMaterials();
        // Save materials files as attached files
        if(materials != null && !materials.isEmpty()){
            LOGGER.info("Saving materials: " + materials.size());
            for (Path material : materials) {
                Path absolutePath = Paths.get(tempDir.toAbsolutePath() + "/" + material.getFileName());
                saveAttachedFile(partIterationKey, absolutePath);
            }
        }

        double[] box = conversionResult.getBox();

        if(convertedFileLODs != null && !convertedFileLODs.isEmpty()) {
            // Handle converted file LODs
            LOGGER.info("LODS discovered: " + convertedFileLODs.size());
            Set<Map.Entry<Integer, Path>> entries = convertedFileLODs.entrySet();
            for (Map.Entry<Integer, Path> entry : entries) {
                Integer quality = entry.getKey();
                Path convertedFile = entry.getValue();
                Path convertedFileAbsolute = Paths.get(tempDir.toAbsolutePath() + "/" + convertedFile.getFileName());
                saveGeometryFile(partIterationKey, quality, convertedFileAbsolute, box);
            }
        }

        try {
            LOGGER.log(Level.FINE, "Conversion ended with success");
            productService.endConversion(partIterationKey, true);
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    private String generateUserToken() {
        String login = contextManager.getCallerPrincipalLogin();
        Key key = authConfig.getJWTKey();
        UserGroupMapping mapping = new UserGroupMapping(login, UserGroupMapping.REGULAR_USER_ROLE_ID);
        return tokenManager.createAuthToken(key, mapping);
    }

    private boolean syncAssembly(Map<String, List<ConversionResult.Position>> componentPositionMap, PartIteration partToConvert)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException,
            WorkspaceNotFoundException, PartRevisionNotFoundException, PartMasterNotFoundException,
            ListOfValuesNotFoundException, PartUsageLinkNotFoundException, DocumentRevisionNotFoundException,
            AccessRightException, NotAllowedException, EntityConstraintException {

        boolean succeed = true;

        List<PartUsageLink> partUsageLinks = new ArrayList<>();
        for (Map.Entry<String, List<ConversionResult.Position>> entry : componentPositionMap.entrySet()) {
            // Component name
            String cadFileName = entry.getKey();
            // Linked component positioning
            List<ConversionResult.Position> positions = entry.getValue();
            // Retrieve this part master ID
            PartMaster partMaster = productService.findPartMasterByCADFileName(partToConvert.getWorkspaceId(),
                    cadFileName);

            if (partMaster != null) {
                PartUsageLink partUsageLink = new PartUsageLink();
                partUsageLink.setAmount(positions.size());
                partUsageLink.setComponent(partMaster);
                partUsageLink.setCadInstances(toCADInstances(positions));
                partUsageLinks.add(partUsageLink);
            } else {
                LOGGER.log(Level.WARNING, "No Part found for {0}", cadFileName);
                succeed = false;
            }
        }
        // Replace usage links (erase old structure)
        partToConvert.setComponents(partUsageLinks);
        productService.updatePartIteration(partToConvert.getKey(), partToConvert.getIterationNote(),
                partToConvert.getSource(), partToConvert.getComponents(), partToConvert.getInstanceAttributes(),
                partToConvert.getInstanceAttributeTemplates(), null, null, null);
        if (succeed) {
            LOGGER.log(Level.INFO, "Assembly synchronized");
        }
        return succeed;
    }

    List<CADInstance> toCADInstances(List<ConversionResult.Position> positions) {
        List<CADInstance> instances = new ArrayList<>();
        for (ConversionResult.Position p : positions) {
            double[] rm = DoubleStream
                    .concat(Arrays.stream(p.getRotationMatrix()[0]), DoubleStream
                            .concat(Arrays.stream(p.getRotationMatrix()[1]), Arrays.stream(p.getRotationMatrix()[2])))
                    .toArray();
            instances
                    .add(new CADInstance(new RotationMatrix(rm), p.getTranslation()[0], p.getTranslation()[1], p.getTranslation()[2]));
        }
        return instances;
    }

    private void saveGeometryFile(PartIterationKey partIPK, int quality, Path file, double[] box) {
        try {
            Geometry lod = (Geometry) productService.saveGeometryInPartIteration(partIPK, file.getFileName().toString(),
                    quality, Files.size(file), box);
            try (OutputStream os = storageManager.getBinaryResourceOutputStream(lod)) {
                Files.copy(file, os);
                LOGGER.log(Level.INFO, "geometry saved : " + file.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to get geometry file's size", e);
        } catch (UserNotFoundException | WorkspaceNotFoundException | WorkspaceNotEnabledException | CreationException
                | FileAlreadyExistsException | PartRevisionNotFoundException | NotAllowedException
                | UserNotActiveException | StorageException e) {
            LOGGER.log(Level.SEVERE, "Cannot save geometry to part iteration", e);
        }
    }

    private void saveAttachedFile(PartIterationKey partIPK, Path file) {
        try {
            BinaryResource binaryResource = productService.saveFileInPartIteration(partIPK,
                    file.getFileName().toString(), PartIteration.ATTACHED_FILES_SUBTYPE, Files.size(file));
            try (OutputStream os = storageManager.getBinaryResourceOutputStream(binaryResource)) {
                Files.copy(file, os);
                LOGGER.log(Level.INFO, "Attached file copied");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to save attached file", e);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to get attached file's size", e);
        } catch (UserNotFoundException | WorkspaceNotFoundException | WorkspaceNotEnabledException | CreationException
                | FileAlreadyExistsException | PartRevisionNotFoundException | NotAllowedException
                | UserNotActiveException | StorageException e) {
            LOGGER.log(Level.SEVERE, "Cannot save attached file to part iteration", e);
        }
    }

}
