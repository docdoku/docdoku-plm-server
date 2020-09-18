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

import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.document.DocumentMasterTemplate;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.ListOfValues;
import com.docdoku.plm.server.core.meta.ListOfValuesKey;
import com.docdoku.plm.server.core.meta.NameValuePair;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartMasterTemplate;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.ILOVManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.dao.*;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Lebeau Julien on 03/03/15.
 */
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(ILOVManagerLocal.class)
@Stateless(name = "LOVManagerBean")
public class LOVManagerBean implements ILOVManagerLocal {

    @Inject
    private LOVDAO lovDAO;

    @Inject
    private DocumentMasterTemplateDAO documentMasterTemplateDAO;

    @Inject
    private PartIterationDAO partIterationDAO;

    @Inject
    private PartMasterTemplateDAO partMasterTemplateDAO;

    @Inject
    private WorkspaceDAO workspaceDAO;

    @Inject
    private IUserManagerLocal userManager;

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ListOfValues> findLOVFromWorkspace(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return lovDAO.loadLOVList(workspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ListOfValues findLov(ListOfValuesKey lovKey) throws ListOfValuesNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(lovKey.getWorkspaceId());
        return lovDAO.loadLOV(lovKey);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void createLov(String workspaceId, String name, List<NameValuePair> nameValuePairList) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, ListOfValuesAlreadyExistsException, CreationException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceWriteAccess(workspaceId);

        if (name == null || name.trim().isEmpty()) {
            throw new CreationException("LOVNameEmptyException");
        }

        if (nameValuePairList == null || nameValuePairList.isEmpty()) {
            throw new CreationException("LOVPossibleValueException");
        }

        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);

        ListOfValues lov = new ListOfValues(workspace, name);
        lov.setValues(nameValuePairList);

        lovDAO.createLOV(lov);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteLov(ListOfValuesKey lovKey) throws ListOfValuesNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, EntityConstraintException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(lovKey.getWorkspaceId());
        userManager.checkWorkspaceWriteAccess(lovKey.getWorkspaceId());

        if (isLovUsedInDocumentMasterTemplate(lovKey)) {
            throw new EntityConstraintException("EntityConstraintException14");
        }

        if (isLovUsedInPartMasterTemplate(lovKey)) {
            throw new EntityConstraintException("EntityConstraintException15");
        }

        ListOfValues lov = lovDAO.loadLOV(lovKey);
        lovDAO.deleteLOV(lov);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ListOfValues updateLov(ListOfValuesKey lovKey, String name, String workspaceId, List<NameValuePair> nameValuePairList) throws ListOfValuesAlreadyExistsException, CreationException, ListOfValuesNotFoundException, UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, AccessRightException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(lovKey.getWorkspaceId());
        userManager.checkWorkspaceWriteAccess(lovKey.getWorkspaceId());
        ListOfValues lovToUpdate = findLov(lovKey);
        lovToUpdate.setValues(nameValuePairList);
        return lovDAO.updateLOV(lovToUpdate);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean isLOVDeletable(ListOfValuesKey lovKey) {
        return !isLovUsedInDocumentMasterTemplate(lovKey) && !isLovUsedInPartMasterTemplate(lovKey) && !isLovUsedInPartIterationInstanceAttributeTemplates(lovKey);
    }

    private boolean isLovUsedInDocumentMasterTemplate(ListOfValuesKey lovKey) {
        List<DocumentMasterTemplate> documentsUsingLOV = documentMasterTemplateDAO.findAllDocMTemplatesFromLOV(lovKey);
        return documentsUsingLOV != null && !documentsUsingLOV.isEmpty();
    }

    private boolean isLovUsedInPartMasterTemplate(ListOfValuesKey lovKey) {
        List<PartMasterTemplate> partsUsingLOV = partMasterTemplateDAO.findAllPartMTemplatesFromLOV(lovKey);
        return partsUsingLOV != null && !partsUsingLOV.isEmpty();
    }

    private boolean isLovUsedInPartIterationInstanceAttributeTemplates(ListOfValuesKey lovKey) {
        List<PartIteration> partsUsingLOV = partIterationDAO.findAllPartIterationFromLOV(lovKey);
        return partsUsingLOV != null && !partsUsingLOV.isEmpty();
    }

}
