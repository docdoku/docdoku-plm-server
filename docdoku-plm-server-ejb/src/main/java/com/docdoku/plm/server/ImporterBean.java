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


import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.configuration.*;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.InstanceAttribute;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.i18n.PropertiesLoader;
import com.docdoku.plm.server.importers.*;

import javax.annotation.PostConstruct;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Attributes importer
 *
 * @author Elisabel Généreux
 */
@Stateless(name = "ImporterBean")
public class ImporterBean implements IImporterManagerLocal {

    private static final String I18N_CONF = "/com/docdoku/plm/server/importers/Importers";

    private static final Logger LOGGER = Logger.getLogger(ImporterBean.class.getName());

    private List<PartImporter> partImporters = new ArrayList<>();
    private List<PathDataImporter> pathDataImporters = new ArrayList<>();
    private List<BomImporter> bomImporters = new ArrayList<>();

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private IProductManagerLocal productManager;

    @Inject
    private IProductInstanceManagerLocal productInstanceManager;

    @Inject
    private ILOVManagerLocal lovManager;

    @Inject
    private BeanLocator beanLocator;

    @PostConstruct
    void init() {
        partImporters.addAll(beanLocator.search(PartImporter.class));
        pathDataImporters.addAll(beanLocator.search(PathDataImporter.class));
        bomImporters.addAll(beanLocator.search(BomImporter.class));
    }

    @Override
    @Asynchronous
    @FileImport
    public Future<ImportResult> importIntoParts(String workspaceId, File file, String originalFileName, String revisionNote, boolean autoCheckout, boolean autoCheckin, boolean permissiveUpdate) {
        Locale locale = getLocale(workspaceId);
        PartImporter selectedImporter = selectPartImporter(file);

        Properties properties = PropertiesLoader.loadLocalizedProperties(locale, I18N_CONF, ImporterBean.class);

        PartImporterResult partImporterResult;

        if (selectedImporter != null) {
            try {
                partImporterResult = selectedImporter.importFile(locale, workspaceId, file, autoCheckout, autoCheckin, permissiveUpdate);
            } catch (PartImporter.ImporterException e) {
                LOGGER.log(Level.SEVERE, null, e);
                List<String> errors = Collections.singletonList(AttributesImporterUtils.createError(properties, "ImporterException", e.getMessage()));
                partImporterResult = new PartImporterResult(file, new ArrayList<>(), errors, null, null, null);
            }
        } else {
            List<String> errors = getNoImporterAvailableError(properties);
            partImporterResult = new PartImporterResult(file, new ArrayList<>(), errors, null, null, null);
        }

        if (partImporterResult.hasErrors()) {
            return new AsyncResult<>(new ImportResult(partImporterResult.getImportedFile(), partImporterResult.getWarnings(), partImporterResult.getErrors()));
        }

        ImportResult result = doPartImport(properties, workspaceId, revisionNote, autoCheckout, autoCheckin, permissiveUpdate, partImporterResult);

        return new AsyncResult<>(result);
    }

    @Override
    public ImportPreview dryRunImportIntoParts(String workspaceId, File file, String originalFileName, boolean autoCheckout, boolean autoCheckin, boolean permissiveUpdate) throws ImportPreviewException {
        Locale locale = getLocale(workspaceId);
        PartImporter selectedImporter = selectPartImporter(file);
        Properties properties = PropertiesLoader.loadLocalizedProperties(locale, I18N_CONF, ImporterBean.class);

        PartImporterResult partImporterResult;

        if (selectedImporter != null) {
            try {
                partImporterResult = selectedImporter.importFile(locale, workspaceId, file, autoCheckout, autoCheckin, permissiveUpdate);
            } catch (PartImporter.ImporterException e) {
                LOGGER.log(Level.SEVERE, null, e);
                List<String> errors = Collections.singletonList(AttributesImporterUtils.createError(properties, "ImporterException", e.getMessage()));
                partImporterResult = new PartImporterResult(file, new ArrayList<>(), errors, null, null, null);
            }
        } else {
            List<String> errors = getNoImporterAvailableError(properties);
            partImporterResult = new PartImporterResult(file, new ArrayList<>(), errors, null, null, null);
        }

        Map<String, PartToImport> partsToImport = partImporterResult.getPartsToImport();
        List<PartRevision> toCheckout = new ArrayList<>();

        if (partsToImport != null) {
            for (PartToImport part : partsToImport.values()) {
                try {
                    PartRevision currentPartRevision = productManager.getPartMaster(new PartMasterKey(workspaceId, part.getNumber())).getLastRevision();
                    PartIteration currentPartIteration = currentPartRevision.getLastIteration();

                    if (autoCheckout && !currentPartRevision.isCheckedOut() && productManager.canWrite(currentPartRevision.getKey()) && !part.getAttributes().isEmpty()
                            && AttributesImporterUtils.checkIfUpdateOrCreateInstanceAttributes(part.getAttributes(), currentPartIteration.getInstanceAttributes())) {
                        toCheckout.add(currentPartRevision);
                    }
                } catch (UserNotFoundException | UserNotActiveException | WorkspaceNotFoundException
                        | PartMasterNotFoundException | PartRevisionNotFoundException | AccessRightException
                        | WorkspaceNotEnabledException e) {
                    LOGGER.log(Level.SEVERE, null, e);
                }
            }
        }

        return new ImportPreview(toCheckout, new ArrayList<>());
    }

    @Override
    @Asynchronous
    @FileImport
    public Future<ImportResult> importIntoPathData(String workspaceId, File file, String originalFileName, String revisionNote, boolean autoFreezeAfterUpdate, boolean permissiveUpdate) {
        Locale locale = getLocale(workspaceId);
        PathDataImporter selectedImporter = selectPathDataImporter(file);
        Properties properties = PropertiesLoader.loadLocalizedProperties(locale, I18N_CONF, ImporterBean.class);

        PathDataImporterResult pathDataImporterResult;

        if (selectedImporter != null) {
            pathDataImporterResult = selectedImporter.importFile(locale, workspaceId, file, autoFreezeAfterUpdate, permissiveUpdate);
        } else {
            List<String> errors = getNoImporterAvailableError(properties);
            pathDataImporterResult = new PathDataImporterResult(file, new ArrayList<>(), errors, null, null, null);
        }

        ImportResult result = doPathDataImport(properties, workspaceId, revisionNote, autoFreezeAfterUpdate, permissiveUpdate, pathDataImporterResult);

        return new AsyncResult<>(result);
    }


    @Override
    @Asynchronous
    @FileImport
    public Future<ImportResult> importBom(String workspaceId, File file, String originalFileName, String revisionNote, boolean autoCheckout, boolean autoCheckin, boolean permissiveUpdate) {
        Locale locale = getLocale(workspaceId);
        BomImporter selectedImporter = selectBomImporter(file);
        Properties properties = PropertiesLoader.loadLocalizedProperties(locale, I18N_CONF, ImporterBean.class);

        BomImporterResult bomImporterResult;

        if (selectedImporter != null) {
            try {
                bomImporterResult = selectedImporter.importFile(locale, workspaceId, file, autoCheckout, autoCheckin, permissiveUpdate);
            } catch (BomImporter.ImporterException e) {
                LOGGER.log(Level.SEVERE, null, e);
                List<String> errors = Collections.singletonList(AttributesImporterUtils.createError(properties, "ImporterException", e.getMessage()));
                bomImporterResult = new BomImporterResult(file, new ArrayList<>(), errors, null, null, null);
            }
        } else {
            List<String> errors = getNoImporterAvailableError(properties);
            bomImporterResult = new BomImporterResult(file, new ArrayList<>(), errors, null, null, null);
        }

        if (bomImporterResult.hasErrors()) {
            return new AsyncResult<>(new ImportResult(bomImporterResult.getImportedFile(), bomImporterResult.getWarnings(), bomImporterResult.getErrors()));
        }

        ImportResult result = doBomImport(properties, workspaceId, getUser(workspaceId), revisionNote, autoCheckin, autoCheckout, permissiveUpdate, bomImporterResult);
        return new AsyncResult<>(result);
    }


    @Override
    public ImportPreview dryRunImportBom(String workspaceId, File file, String originalFileName, boolean autoCheckout, boolean autoCheckin, boolean permissiveUpdate) throws ImportPreviewException {
        Locale locale = getLocale(workspaceId);
        BomImporter selectedImporter = selectBomImporter(file);
        Properties properties = PropertiesLoader.loadLocalizedProperties(locale, I18N_CONF, ImporterBean.class);

        BomImporterResult bomImporterResult;

        if (selectedImporter != null) {
            try {
                bomImporterResult = selectedImporter.importFile(locale, workspaceId, file, autoCheckout, autoCheckin, permissiveUpdate);
            } catch (BomImporter.ImporterException e) {
                LOGGER.log(Level.SEVERE, null, e);
                List<String> errors = Collections.singletonList(AttributesImporterUtils.createError(properties, "ImporterException", e.getMessage()));
                bomImporterResult = new BomImporterResult(file, new ArrayList<>(), errors, null, null, null);
            }
        } else {
            List<String> errors = getNoImporterAvailableError(properties);
            bomImporterResult = new BomImporterResult(file, new ArrayList<>(), errors, null, null, null);
        }

        Map<String, PartMaster> bomAsMap = new HashMap<>();
        List<PartMaster> toCreate = new ArrayList<>();
        List<PartRevision> toCheckout = new ArrayList<>();
        Set<PartToImport> rows = new HashSet<>();

        if (bomImporterResult.getPartsMap() != null) {

            for (Map.Entry<PartToImport, List<PartToImport>> entry : bomImporterResult.getPartsMap().entrySet()) {
                rows.add(entry.getKey());
                rows.addAll(entry.getValue());
            }

            for (PartToImport bomRow : rows) {
                String partNumber = bomRow.getNumber();
                PartMaster partMaster = new PartMaster(new Workspace(workspaceId), partNumber);
                bomAsMap.put(partNumber, partMaster);
            }

            for (Map.Entry<String, PartMaster> entry : bomAsMap.entrySet()) {
                PartMaster value = entry.getValue();
                PartMasterKey partMasterKey = value.getKey();
                try {
                    if (!productManager.partMasterExists(partMasterKey)) {
                        toCreate.add(value);
                    } else {
                        PartMaster partMaster = productManager.getPartMaster(partMasterKey);
                        PartRevision lastRevision = partMaster.getLastRevision();

                        if (!lastRevision.isCheckedOut() && autoCheckout) {
                            toCheckout.add(lastRevision);
                        }
                    }
                } catch (ApplicationException e) {
                    LOGGER.log(Level.SEVERE, "Cannot get part master info: " + value, e);
                }
            }
        }

        return new ImportPreview(toCheckout, toCreate);

    }

    private User getUser(String workspaceId) {
        User user = null;
        try {
            user = userManager.whoAmI(workspaceId);
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Cannot fetch account info", e);
        }
        return user;
    }

    private Locale getLocale(String workspaceId) {
        User user = getUser(workspaceId);
        return user != null ? user.getLocale() : Locale.getDefault();
    }

    private PartImporter selectPartImporter(File file) {
        PartImporter selectedImporter = null;
        for (PartImporter importer : partImporters) {
            if (importer.canImportFile(file.getName())) {
                selectedImporter = importer;
                break;
            }
        }
        return selectedImporter;
    }

    private PathDataImporter selectPathDataImporter(File file) {
        PathDataImporter selectedImporter = null;
        for (PathDataImporter importer : pathDataImporters) {
            if (importer.canImportFile(file.getName())) {
                selectedImporter = importer;
                break;
            }
        }
        return selectedImporter;
    }

    private BomImporter selectBomImporter(File file) {
        BomImporter selectedImporter = null;
        for (BomImporter importer : bomImporters) {
            if (importer.canImportFile(file.getName())) {
                selectedImporter = importer;
                break;
            }
        }
        return selectedImporter;
    }

    private List<String> getNoImporterAvailableError(Properties properties) {
        return Collections.singletonList(properties.getProperty("NoImporterAvailable"));
    }


    private boolean canChangePart(String workspaceId, PartRevision lastRevision, boolean autoCheckout)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        return (autoCheckout && !lastRevision.isCheckedOut()) || (lastRevision.isCheckedOut() && lastRevision.getCheckOutUser().equals(user));
    }

    private ImportResult doPartImport(Properties properties, String workspaceId, String revisionNote, boolean autoCheckout, boolean autoCheckin,
                                      boolean permissiveUpdate, PartImporterResult partImporterResult) {

        List<String> errors = partImporterResult.getErrors();
        List<String> warnings = partImporterResult.getWarnings();
        Map<String, PartToImport> partsToImport = partImporterResult.getPartsToImport();
        List<PartToImport> listParts = new ArrayList<>();

        for (PartToImport part : partsToImport.values()) {

            try {
                PartMaster currentPartMaster = productManager.getPartMaster(new PartMasterKey(workspaceId, part.getNumber()));

                PartIteration partIteration = currentPartMaster.getLastRevision().getLastIteration();

                boolean hasAccess = productManager.canWrite(currentPartMaster.getLastRevision().getKey());

                if (part.hasAttributes() && (hasAccess && canChangePart(workspaceId, partIteration.getPartRevision(), autoCheckout))) {

                    //info : we create 2 instanceAttribute Lists to ensure separation between current list and updated list
                    List<InstanceAttribute> updatedInstanceAttributes = AttributesImporterUtils.getInstanceAttributes(properties, partIteration.getInstanceAttributes(), errors);//we will update data here
                    List<InstanceAttribute> currentInstanceAttributes = new ArrayList<>(updatedInstanceAttributes);//we will delete updated attributes from here

                    List<Attribute> attributes = part.getAttributes();
                    part.getNumber();
                    AttributesImporterUtils.updateAndCreateInstanceAttributes(lovManager, properties, attributes, currentInstanceAttributes, part.getNumber(), errors, workspaceId, updatedInstanceAttributes);
                    part.setInstanceAttributes(updatedInstanceAttributes);
                    if (revisionNote != null && !revisionNote.isEmpty()) {
                        part.setRevisionNote(revisionNote);
                    }
                    part.setPartIteration(partIteration);
                    listParts.add(part);

                } else if (permissiveUpdate && !hasAccess) {
                    warnings.add(AttributesImporterUtils.createError(properties, "NotAccess", part.getNumber()));
                    LOGGER.log(Level.WARNING, "No right on [" + part.getNumber() + "]");

                } else if (!canChangePart(workspaceId, partIteration.getPartRevision(), autoCheckout)) {
                    User user = userManager.checkWorkspaceReadAccess(workspaceId);

                    if (partIteration.getPartRevision().isCheckedOut() && !partIteration.getPartRevision().getCheckOutUser().equals(user)) {
                        String errorMessage = AttributesImporterUtils.createError(properties, "AlreadyCheckedOut", part.getNumber(), partIteration.getPartRevision().getCheckOutUser().getName());
                        if (permissiveUpdate) {
                            warnings.add(errorMessage);
                        } else {
                            errors.add(errorMessage);
                        }

                    } else if (!partIteration.getPartRevision().isCheckedOut()) {
                        String errorMessage = AttributesImporterUtils.createError(properties, "NotCheckedOut", part.getNumber());
                        if (permissiveUpdate) {
                            warnings.add(errorMessage);
                        } else {
                            errors.add(errorMessage);
                        }
                    }
                }

            } catch
                    (AccessRightException | UserNotFoundException | UserNotActiveException | WorkspaceNotFoundException
                            | PartMasterNotFoundException | PartRevisionNotFoundException | WorkspaceNotEnabledException e) {
                LOGGER.log(Level.WARNING, "Could not get PartMaster[" + part.getNumber() + "]", e);
                errors.add(e.getLocalizedMessage());
            }
        }

        if (!errors.isEmpty()) {
            return new ImportResult(partImporterResult.getImportedFile(), warnings, errors);
        }

        try {
            bulkPartUpdate(listParts, workspaceId, autoCheckout, autoCheckin, permissiveUpdate, errors, warnings);
        } catch (ApplicationException e) {
            LOGGER.log(Level.WARNING, null, e);
            errors.add("Unhandled exception");
        }
        return new ImportResult(partImporterResult.getImportedFile(), warnings, errors);
    }

    public void bulkPartUpdate(List<PartToImport> parts, String workspaceId, boolean autoCheckout, boolean autoCheckin, boolean permissive, List<String> errors, List<String> warnings) throws ApplicationException {

        LOGGER.log(Level.INFO, "Bulk parts update");
        User user = userManager.checkWorkspaceReadAccess(workspaceId);

        boolean errorOccurred = false;
        ApplicationException exception = null;

        for (PartToImport part : parts) {
            PartIteration partIteration = part.getPartIteration();

            try {
                PartMaster currentPartMaster = productManager.getPartMaster(new PartMasterKey(workspaceId, part.getNumber()));

                boolean isAutoCheckedOutByImport = false; //to check if checkout for the update

                if (autoCheckout && !currentPartMaster.getLastRevision().isCheckedOut() && productManager.canWrite(currentPartMaster.getLastRevision().getKey())) {
                    PartRevision currentPartRevision = productManager.checkOutPart(new PartRevisionKey(workspaceId, part.getNumber(), currentPartMaster.getLastRevision().getVersion()));
                    isAutoCheckedOutByImport = true;
                    partIteration = currentPartRevision.getLastIteration();
                }

                //Check if not permissive or permissive and checked out
                if (partIteration.getPartRevision().isCheckedOut() && partIteration.getPartRevision().getCheckOutUser().equals(user)) {
                    //Do not lose previous saved revision note if no note specified during import
                    if (part.getRevisionNote() == null) {
                        part.setRevisionNote(partIteration.getIterationNote());
                    }
                    productManager.updatePartIteration(partIteration.getKey(), part.getRevisionNote(), partIteration.getSource(), null, part.getInstanceAttributes(), null, null, null, null);
                } else {
                    throw new NotAllowedException("NotAllowedException25", partIteration.getPartRevision().toString());
                }

                //CheckIn if checkout before
                if (autoCheckin && isAutoCheckedOutByImport && partIteration.getPartRevision().getCheckOutUser().equals(user)) {
                    try {
                        productManager.checkInPart(new PartRevisionKey(currentPartMaster.getKey(), currentPartMaster.getLastRevision().getVersion()));
                    } catch (NotAllowedException e) {
                        LOGGER.log(Level.WARNING, null, e);
                        warnings.add(e.getLocalizedMessage());
                    }
                }
            } catch (CreationException | PartMasterNotFoundException | EntityConstraintException | UserNotFoundException | WorkspaceNotFoundException | UserNotActiveException | PartUsageLinkNotFoundException | PartRevisionNotFoundException | AccessRightException | FileAlreadyExistsException e) {
                LOGGER.log(Level.WARNING, null, e);
                errors.add(e.getLocalizedMessage() + ": " + partIteration.getNumber());
                errorOccurred = true;
                exception = e;

            } catch (NotAllowedException e) {
                LOGGER.log(Level.WARNING, null, e);
                if (permissive) {
                    warnings.add(e.getLocalizedMessage());
                } else {
                    errors.add(e.getLocalizedMessage());
                    errorOccurred = true;
                    exception = e;
                }
            }
        }

        LOGGER.log(Level.INFO, "Bulk parts update finished");

        if (errorOccurred) {
            throw exception;
        }
    }

    private ImportResult doPathDataImport(Properties properties, String workspaceId, String revisionNote, boolean autoFreezeAfterUpdate, boolean permissiveUpdate, PathDataImporterResult pathDataImporterResult) {

        Map<String, Map<String, Boolean>> instancesAccess = new HashMap<>();
        Map<String, Map<String, ProductInstanceIteration>> productInstancesCache = new HashMap<>();
        Map<String, PathDataToImport> result = pathDataImporterResult.getPartsToImport();
        List<PathDataToImport> listPathData = new ArrayList<>();
        List<String> errors = pathDataImporterResult.getErrors();
        List<String> warnings = pathDataImporterResult.getWarnings();
        File file = pathDataImporterResult.getImportedFile();

        for (PathDataToImport pathData : result.values()) {
            createOrUpdatePathData(properties, workspaceId, pathData, permissiveUpdate, revisionNote, listPathData, errors, instancesAccess, productInstancesCache);
        }

        LOGGER.log(Level.INFO, "Iterate  pathData finished");


        if (errors.size() > 0) {
            return new ImportResult(file, warnings, errors);
        }

        try {
            bulkPathDataUpdate(listPathData, workspaceId, autoFreezeAfterUpdate, permissiveUpdate, errors, warnings);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
            errors.add("Unhandled exception");
        }

        return new ImportResult(file, warnings, errors);
    }

    private void createOrUpdatePathData(Properties properties, String workspaceId, PathDataToImport pathData, boolean permissiveUpdate, String revisionNote, List<PathDataToImport> listPathData, List<String> errors, Map<String, Map<String, Boolean>> instancesAccess, Map<String, Map<String, ProductInstanceIteration>> productInstancesCache) {

        try {

            // Cache hack start //
            String productId = pathData.getProductId();
            String serialNUmber = pathData.getSerialNumber();
            Map<String, Boolean> productAccess = instancesAccess.computeIfAbsent(productId, k -> new HashMap<>());

            Boolean hasInstanceAccess = productAccess.get(serialNUmber);
            if (hasInstanceAccess == null) {
                hasInstanceAccess = productInstanceManager.canWrite(workspaceId, pathData.getProductId(), pathData.getSerialNumber());
                productAccess.put(serialNUmber, hasInstanceAccess);
            }

            ProductInstanceIteration productInstanceIteration = null;
            if (hasInstanceAccess) {
                Map<String, ProductInstanceIteration> cache = productInstancesCache.computeIfAbsent(productId, k -> new HashMap<>());

                productInstanceIteration = cache.get(serialNUmber);

                if (productInstanceIteration == null) {
                    ProductInstanceMaster productInstanceMaster = productInstanceManager.getProductInstanceMaster(new ProductInstanceMasterKey(serialNUmber, workspaceId, productId));
                    productInstanceIteration = productInstanceMaster.getLastIteration();
                    cache.put(serialNUmber, productInstanceIteration);

                }
            }

            // Cache hack end //
            // hasInstanceAccess is always true when productInstanceIteration is not null
            if (null != productInstanceIteration && !pathData.getAttributes().isEmpty()) {

                // 2 Possibilities : PathDataMasterId null => create new Path Data, not null => update PathData
                PathDataMaster currentPathDataMaster = findPathDataMaster(productInstanceIteration, pathData.getPath());

                if (currentPathDataMaster != null) {
                    PathDataIteration pathDataIteration = currentPathDataMaster.getLastIteration();

                    //info : we create 2 instanceAttribute Lists to ensure separation between current list and updated list
                    List<InstanceAttribute> updatedInstanceAttributes = AttributesImporterUtils.getInstanceAttributes(properties, pathDataIteration.getInstanceAttributes(), errors);//we will update data here
                    List<InstanceAttribute> currentInstanceAttributes = new ArrayList<>(updatedInstanceAttributes);//we will delete updated attributes from here

                    List<Attribute> attributes = pathData.getAttributes();
                    AttributesImporterUtils.updateAndCreateInstanceAttributes(lovManager, properties, attributes, currentInstanceAttributes, pathData.getPath(), errors, workspaceId, updatedInstanceAttributes);
                    pathData.setInstanceAttributes(updatedInstanceAttributes);
                    pathData.setRevisionNote(revisionNote);
                    pathData.setPathDataIteration(pathDataIteration);
                    listPathData.add(pathData);
                } else {
                    List<Attribute> attributes = pathData.getAttributes();

                    List<InstanceAttribute> newInstanceAttributes = new ArrayList<>();

                    for (Attribute attribute : attributes) {
                        InstanceAttribute instanceAttribute = AttributesImporterUtils.createAttribute(lovManager, properties, attribute, workspaceId, errors);
                        newInstanceAttributes.add(instanceAttribute);
                    }
                    pathData.setPath(pathData.getPath());
                    pathData.setInstanceAttributes(newInstanceAttributes);
                    pathData.setRevisionNote(revisionNote);
                    listPathData.add(pathData);
                }

            }
        } catch (UserNotFoundException | UserNotActiveException | WorkspaceNotFoundException | WorkspaceNotEnabledException e) {
            LOGGER.log(Level.WARNING, "Could not get PathData Master [" + pathData.getPath() + "]", e);
            errors.add(e.getLocalizedMessage());
        } catch (ProductInstanceMasterNotFoundException e) {
            LOGGER.log(Level.WARNING, "Could not get Product Instance Master [" + pathData.getPath() + "]", e);
            errors.add(e.getLocalizedMessage());
        }
    }

    private PathDataMaster findPathDataMaster(ProductInstanceIteration productInstanceIteration, String path) {
        for (PathDataMaster pathDataMaster : productInstanceIteration.getPathDataMasterList()) {
            if (pathDataMaster.getPath().equals(path)) {
                return pathDataMaster;
            }
        }
        return null;
    }

    public void bulkPathDataUpdate(List<PathDataToImport> pathDataList, String workspaceId, boolean autoFreeze, boolean permissive, List<String> errors, List<String> warnings) throws Exception {

        LOGGER.log(Level.INFO, "Bulk path data update");

        boolean errorOccured = false;
        Exception exception = null;


        for (PathDataToImport pathData : pathDataList) {
            try {

                PathDataMaster currentPathDataMaster = productInstanceManager.getPathDataByPath(workspaceId, pathData.getProductId(), pathData.getSerialNumber(), pathData.getPath());

                PathDataMaster pathDataMaster;

                if (currentPathDataMaster != null) {
                    pathDataMaster = productInstanceManager.addNewPathDataIteration(workspaceId, pathData.getProductId(), pathData.getSerialNumber(), currentPathDataMaster.getId(), cloneAttributes(pathData.getInstanceAttributes()), pathData.getRevisionNote(), null, null);
                } else {
                    pathDataMaster = productInstanceManager.createPathDataMaster(workspaceId, pathData.getProductId(), pathData.getSerialNumber(), pathData.getPath(), cloneAttributes(pathData.getInstanceAttributes()), pathData.getRevisionNote());
                }
                if (autoFreeze) {
                    productInstanceManager.addNewPathDataIteration(workspaceId, pathData.getProductId(), pathData.getSerialNumber(), pathDataMaster.getId(), cloneAttributes(pathDataMaster.getLastIteration().getInstanceAttributes()), null, null, null);
                }

            } catch (UserNotFoundException | WorkspaceNotFoundException | UserNotActiveException | AccessRightException | ProductInstanceMasterNotFoundException | PathDataAlreadyExistsException e) {
                LOGGER.log(Level.SEVERE, null, e);
                errors.add(e.getLocalizedMessage());
                errorOccured = true;
                exception = e;

            } catch (NotAllowedException e) {
                if (permissive) {
                    warnings.add(e.getLocalizedMessage());
                } else {
                    errors.add(e.getLocalizedMessage());
                    errorOccured = true;
                    exception = e;
                }
            }

        }

        LOGGER.log(Level.INFO, "Bulk path data update finished");

        if (errorOccured) {
            throw exception;
        }
    }

    private List<InstanceAttribute> cloneAttributes(List<InstanceAttribute> pAttributes) {
        List<InstanceAttribute> attributes = new ArrayList<>();
        for (InstanceAttribute instanceAttribute : pAttributes) {
            attributes.add(instanceAttribute.clone());
        }
        return attributes;
    }


    private ImportResult doBomImport(Properties properties, String workspaceId, User user, String revisionNote, boolean autoCheckin, boolean autoCheckout, boolean permissiveUpdate, BomImporterResult bomImporterResult) {

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<PartRevisionKey> toCheckin = new HashSet<>();
        File file = bomImporterResult.getImportedFile();

        for (Map.Entry<PartToImport, List<PartToImport>> entry : bomImporterResult.getPartsMap().entrySet()) {

            // Process children parts
            List<PartToImport> childrenRows = entry.getValue();
            List<PartUsageLink> usageLinks = new ArrayList<>();

            for (PartToImport childRow : childrenRows) {
                String childNumber = childRow.getNumber();

                try {
                    PartMaster childPartMaster = getOrCreatePartMaster(properties, errors, workspaceId, toCheckin, childNumber, childRow, revisionNote, autoCheckin);
                    usageLinks.add(createUsageLink(childPartMaster, childRow));
                } catch (ApplicationException e) {
                    LOGGER.log(Level.SEVERE, "Cannot initialize part master " + workspaceId + "-" + childNumber, e);
                    errors.add(e.getMessage());
                }
            }

            // Process parent part
            PartToImport parentRow = entry.getKey();

            String parentPartNumber = parentRow.getNumber();
            PartMaster parentPart;

            try {
                parentPart = getOrCreatePartMaster(properties, errors, workspaceId, toCheckin, parentPartNumber, parentRow, revisionNote, autoCheckin);
                PartRevision parentPartLastRevision = parentPart.getLastRevision();
                PartIteration parentPartLastIteration = parentPartLastRevision.getLastIteration();

                if (isLocked(parentPartLastRevision, user)) {
                    if (permissiveUpdate) {
                        // Ignore lock by someone else, next bucket
                        continue;
                    } else {
                        errors.add(new NotAllowedException("NotAllowedException37").getMessage());
                        return new ImportResult(file, warnings, errors);
                    }

                } else if (!parentPartLastRevision.isCheckedOut()) {

                    if (autoCheckout) {
                        parentPartLastRevision = productManager.checkOutPart(parentPartLastRevision.getKey());
                        parentPartLastIteration = parentPartLastRevision.getLastIteration();
                    } else if (!permissiveUpdate) {
                        errors.add(new NotAllowedException("NotAllowedException25", parentPartNumber).getMessage());
                        return new ImportResult(file, warnings, errors);
                    } else { // permissive update
                        continue;
                    }

                } // else part is already checked out by user

                productManager.updatePartIteration(parentPartLastIteration.getKey(), revisionNote, null,
                        usageLinks, null, null, null, null, null);

                if (autoCheckin) {
                    toCheckin.add(parentPartLastRevision.getKey());
                }

            } catch (ApplicationException e) {
                LOGGER.log(Level.SEVERE, "Cannot initialize part master " + workspaceId + "-" + parentPartNumber, e);
                errors.add(e.getMessage());
            }

        }

        for (PartRevisionKey revisionKey : toCheckin) {
            try {
                productManager.checkInPart(revisionKey);
            } catch (ApplicationException e) {
                LOGGER.log(Level.SEVERE, "Cannot checkin part revision " + revisionKey, e);
                errors.add(e.getMessage());
            }
        }


        return null;
    }

    private PartMaster getOrCreatePartMaster(Properties properties, List<String> errors, String workspaceId, Set<PartRevisionKey> toCheckin, String partNumber, PartToImport bomRow, String revisionNote, boolean autoCheckin)
            throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, WorkspaceNotEnabledException, PartMasterNotFoundException, PartMasterAlreadyExistsException, PartMasterTemplateNotFoundException, FileAlreadyExistsException, NotAllowedException, UserGroupNotFoundException, RoleNotFoundException, WorkflowModelNotFoundException, AccessRightException, CreationException, PartRevisionNotFoundException, ListOfValuesNotFoundException, DocumentRevisionNotFoundException, PartUsageLinkNotFoundException, EntityConstraintException {

        PartMaster partMaster;
        PartMasterKey partMasterKey = new PartMasterKey(workspaceId, partNumber);

        if (productManager.partMasterExists(partMasterKey)) {
            partMaster = productManager.getPartMaster(partMasterKey);
        } else {
            partMaster = productManager.createPartMaster(workspaceId, partNumber, "", false, null,
                    bomRow.getDescription(), null, null, null, null, null);

            PartRevision lastRevision = partMaster.getLastRevision();
            PartIteration lastIteration = lastRevision.getLastIteration();
            List<InstanceAttribute> instanceAttributes = bomRow.getAttributes()
                    .stream()
                    .map(attribute -> AttributesImporterUtils.createAttribute(lovManager, properties, attribute, workspaceId, errors))
                    .collect(Collectors.toList());

            productManager.updatePartIteration(lastIteration.getKey(), revisionNote, null,
                    null, instanceAttributes, null, null, null, null);

            if (autoCheckin) {
                toCheckin.add(lastRevision.getKey());
            }
        }
        return partMaster;
    }


    private PartUsageLink createUsageLink(PartMaster childPartMaster, PartToImport childRow) {
        PartUsageLink partUsageLink = new PartUsageLink();
        List<CADInstance> cadInstances = new ArrayList<>();

        Double quantity = childRow.getAmount();

        for (double i = 0; i < quantity; i++) {
            cadInstances.add(new CADInstance(0, 0, 0, 0, 0, 0));
        }

        partUsageLink.setComponent(childPartMaster);
        partUsageLink.setAmount(quantity);
        partUsageLink.setCadInstances(cadInstances);

        return partUsageLink;
    }

    private boolean isLocked(PartRevision partRevision, User user) {
        return partRevision.isCheckedOut() && !user.equals(partRevision.getCheckOutUser());
    }

}
