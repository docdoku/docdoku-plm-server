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

import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IEffectivityManagerLocal;
import com.docdoku.plm.server.core.services.IProductManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.dao.ConfigurationItemDAO;
import com.docdoku.plm.server.dao.EffectivityDAO;
import com.docdoku.plm.server.dao.PartRevisionDAO;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;
import java.util.Set;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID})
@Local(IEffectivityManagerLocal.class)
@Stateless(name = "EffectivityManagerBean")
public class EffectivityManagerBean implements IEffectivityManagerLocal {

    @Inject
    private ConfigurationItemDAO configurationItemDAO;

    @Inject
    private EffectivityDAO effectivityDAO;

    @Inject
    private PartRevisionDAO partRevisionDAO;


    @Inject
    private IUserManagerLocal userManager;


    @Inject
    private IProductManagerLocal productManager;

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public SerialNumberBasedEffectivity createSerialNumberBasedEffectivity(String workspaceId, String partNumber, String version, String pName, String pDescription, String pConfigurationItemId, String pStartNumber, String pEndNumber)
            throws EffectivityAlreadyExistsException, CreationException, ConfigurationItemNotFoundException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, PartRevisionNotFoundException, UserNotActiveException {

        userManager.checkWorkspaceWriteAccess(workspaceId);

        // lower range is mandatory, upper range isn't
        if (pStartNumber == null || pStartNumber.isEmpty()) {
            throw new CreationException();
        }

        PartRevisionKey partRevisionKey = new PartRevisionKey(workspaceId, partNumber, version);
        ConfigurationItemKey configurationItemKey = new ConfigurationItemKey(workspaceId, pConfigurationItemId);

        ConfigurationItem configurationItem =
                configurationItemDAO.loadConfigurationItem(configurationItemKey);

        SerialNumberBasedEffectivity serialNumberBasedEffectivity = new SerialNumberBasedEffectivity();
        serialNumberBasedEffectivity.setName(pName);
        serialNumberBasedEffectivity.setDescription(pDescription);
        serialNumberBasedEffectivity.setConfigurationItem(configurationItem);
        serialNumberBasedEffectivity.setStartNumber(pStartNumber);
        serialNumberBasedEffectivity.setEndNumber(pEndNumber);


        effectivityDAO.createEffectivity(serialNumberBasedEffectivity);

        PartRevision partRevision = productManager.getPartRevision(partRevisionKey);
        Set<Effectivity> effectivities = partRevision.getEffectivities();
        effectivities.add(serialNumberBasedEffectivity);
        partRevision.setEffectivities(effectivities);
        partRevisionDAO.updateRevision(partRevision);

        return serialNumberBasedEffectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public DateBasedEffectivity createDateBasedEffectivity(
            String workspaceId, String partNumber, String version, String pName, String pDescription, String pConfigurationItemId, Date pStartDate, Date pEndDate)
            throws EffectivityAlreadyExistsException, CreationException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, PartRevisionNotFoundException, UserNotActiveException, ConfigurationItemNotFoundException {

        userManager.checkWorkspaceWriteAccess(workspaceId);

        // lower range is mandatory, upper range isn't
        if (pStartDate == null) {
            throw new CreationException();
        }

        // ConfigurationItem is optional for Date based effectivities
        ConfigurationItem configurationItem = null;

        if (pConfigurationItemId != null && !pConfigurationItemId.isEmpty()) {
            ConfigurationItemKey configurationItemKey = new ConfigurationItemKey(workspaceId, pConfigurationItemId);
            configurationItem = configurationItemDAO.loadConfigurationItem(configurationItemKey);
        }

        DateBasedEffectivity dateBasedEffectivity = new DateBasedEffectivity();
        dateBasedEffectivity.setName(pName);
        dateBasedEffectivity.setDescription(pDescription);
        dateBasedEffectivity.setStartDate(pStartDate);
        dateBasedEffectivity.setEndDate(pEndDate);
        dateBasedEffectivity.setConfigurationItem(configurationItem);
        effectivityDAO.createEffectivity(dateBasedEffectivity);

        PartRevisionKey partRevisionKey = new PartRevisionKey(workspaceId, partNumber, version);
        PartRevision partRevision = productManager.getPartRevision(partRevisionKey);
        Set<Effectivity> effectivities = partRevision.getEffectivities();
        effectivities.add(dateBasedEffectivity);
        partRevision.setEffectivities(effectivities);
        partRevisionDAO.updateRevision(partRevision);
        return dateBasedEffectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public LotBasedEffectivity createLotBasedEffectivity(
            String workspaceId, String partNumber, String version, String pName, String pDescription, String pConfigurationItemId, String pStartLotId, String pEndLotId) throws UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, CreationException, EffectivityAlreadyExistsException, ConfigurationItemNotFoundException, PartRevisionNotFoundException, UserNotActiveException {


        userManager.checkWorkspaceWriteAccess(workspaceId);

        // lower range is mandatory, upper range isn't
        if (pStartLotId == null || pStartLotId.isEmpty()) {
            throw new CreationException();
        }

        ConfigurationItemKey configurationItemKey = new ConfigurationItemKey(workspaceId, pConfigurationItemId);
        ConfigurationItem configurationItem = configurationItemDAO.loadConfigurationItem(configurationItemKey);

        LotBasedEffectivity lotBasedEffectivity = new LotBasedEffectivity();
        lotBasedEffectivity.setName(pName);
        lotBasedEffectivity.setDescription(pDescription);
        lotBasedEffectivity.setConfigurationItem(configurationItem);
        lotBasedEffectivity.setStartLotId(pStartLotId);
        lotBasedEffectivity.setEndLotId(pEndLotId);
        effectivityDAO.createEffectivity(lotBasedEffectivity);

        PartRevisionKey partRevisionKey = new PartRevisionKey(workspaceId, partNumber, version);
        PartRevision partRevision = productManager.getPartRevision(partRevisionKey);

        Set<Effectivity> effectivities = partRevision.getEffectivities();
        effectivities.add(lotBasedEffectivity);
        partRevision.setEffectivities(effectivities);
        partRevisionDAO.updateRevision(partRevision);

        return lotBasedEffectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public Effectivity getEffectivity(String workspaceId, int pId) throws EffectivityNotFoundException, UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        PartRevision partRevision = effectivityDAO.getPartRevisionHolder(pId);

        if (partRevision == null || !partRevision.getWorkspaceId().equals(workspaceId)) {
            throw new EffectivityNotFoundException(String.valueOf(pId));
        }

        return partRevision.getEffectivities().stream()
                .filter(e -> e.getId() == pId).findFirst().orElse(null);
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public Effectivity updateEffectivity(String workspaceId, int pId, String pName, String pDescription) throws EffectivityNotFoundException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException {

        userManager.checkWorkspaceWriteAccess(workspaceId);

        PartRevision partRevision = effectivityDAO.getPartRevisionHolder(pId);

        if (partRevision == null || !partRevision.getWorkspaceId().equals(workspaceId)) {
            throw new EffectivityNotFoundException(String.valueOf(pId));
        }

        Effectivity effectivity = partRevision.getEffectivities().stream()
                .filter(e -> e.getId() == pId)
                .findFirst()
                .orElseThrow(() -> new EffectivityNotFoundException(String.valueOf(pId)));

        effectivity.setName(pName);
        effectivity.setDescription(pDescription);
        effectivityDAO.updateEffectivity(effectivity);
        return effectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public SerialNumberBasedEffectivity updateSerialNumberBasedEffectivity(String workspaceId, int pId, String pName, String pDescription, String pStartNumber, String pEndNumber) throws EffectivityNotFoundException, UpdateException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, CreationException {

        userManager.checkWorkspaceWriteAccess(workspaceId);

        // lower range is mandatory, upper range isn't
        if (pStartNumber == null || pStartNumber.isEmpty()) {
            throw new CreationException();
        }

        PartRevision partRevision = effectivityDAO.getPartRevisionHolder(pId);

        if (partRevision == null || !partRevision.getWorkspaceId().equals(workspaceId)) {
            throw new EffectivityNotFoundException(String.valueOf(pId));
        }

        SerialNumberBasedEffectivity effectivity = (SerialNumberBasedEffectivity) partRevision.getEffectivities().stream()
                .filter(e -> e.getId() == pId).findFirst()
                .orElseThrow(() -> new EffectivityNotFoundException(String.valueOf(pId)));

        effectivity.setName(pName);
        effectivity.setDescription(pDescription);
        effectivity.setStartNumber(pStartNumber);
        effectivity.setEndNumber(pEndNumber);
        effectivityDAO.updateEffectivity(effectivity);

        return effectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public DateBasedEffectivity updateDateBasedEffectivity(String workspaceId, int pId, String pName, String pDescription, Date pStartDate, Date pEndDate) throws EffectivityNotFoundException, UpdateException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, CreationException {
        userManager.checkWorkspaceWriteAccess(workspaceId);

        // lower range is mandatory, upper range isn't
        if (pStartDate == null) {
            throw new CreationException();
        }

        PartRevision partRevision = effectivityDAO.getPartRevisionHolder(pId);

        if (partRevision == null || !partRevision.getWorkspaceId().equals(workspaceId)) {
            throw new EffectivityNotFoundException(String.valueOf(pId));
        }

        DateBasedEffectivity effectivity = (DateBasedEffectivity) partRevision.getEffectivities().stream()
                .filter(e -> e.getId() == pId).findFirst()
                .orElseThrow(() -> new EffectivityNotFoundException(String.valueOf(pId)));

        effectivity.setName(pName);
        effectivity.setDescription(pDescription);
        effectivity.setStartDate(pStartDate);
        effectivity.setEndDate(pEndDate);
        effectivityDAO.updateEffectivity(effectivity);

        return effectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public LotBasedEffectivity updateLotBasedEffectivity(String workspaceId, int pId, String pName, String pDescription, String pStartLotId, String pEndLotId) throws UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, CreationException, EffectivityNotFoundException {

        userManager.checkWorkspaceWriteAccess(workspaceId);

        // lower range is mandatory, upper range isn't
        if (pStartLotId == null || pStartLotId.isEmpty()) {
            throw new CreationException();
        }

        PartRevision partRevision = effectivityDAO.getPartRevisionHolder(pId);

        if (partRevision == null || !partRevision.getWorkspaceId().equals(workspaceId)) {
            throw new EffectivityNotFoundException(String.valueOf(pId));
        }

        LotBasedEffectivity effectivity = (LotBasedEffectivity) partRevision.getEffectivities().stream()
                .filter(e -> e.getId() == pId).findFirst()
                .orElseThrow(() -> new EffectivityNotFoundException(String.valueOf(pId)));

        effectivity.setName(pName);
        effectivity.setDescription(pDescription);
        effectivity.setStartLotId(pStartLotId);
        effectivity.setEndLotId(pEndLotId);
        effectivityDAO.updateEffectivity(effectivity);

        return effectivity;
    }

    @Override
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    public void deleteEffectivity(String workspaceId, String partNumber, String version, int pId) throws EffectivityNotFoundException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccessRightException, PartRevisionNotFoundException, UserNotActiveException {
        userManager.checkWorkspaceWriteAccess(workspaceId);

        PartRevision partRevision = effectivityDAO.getPartRevisionHolder(pId);

        Effectivity effectivity = partRevision.getEffectivities().stream()
                .filter(e -> e.getId() == pId).findFirst()
                .orElseThrow(() -> new EffectivityNotFoundException(String.valueOf(pId)));

        if (effectivity == null || !partRevision.getWorkspaceId().equals(workspaceId)) {
            throw new EffectivityNotFoundException(String.valueOf(pId));
        }

        partRevisionDAO.removePartRevisionEffectivity(partRevision, effectivity);

        effectivityDAO.removeEffectivity(effectivity);
    }
}
