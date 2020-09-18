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

import com.docdoku.plm.server.core.change.ModificationNotification;
import com.docdoku.plm.server.core.common.*;
import com.docdoku.plm.server.core.configuration.*;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentLink;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.*;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.product.PartIteration.Source;
import com.docdoku.plm.server.core.query.*;
import com.docdoku.plm.server.core.security.ACL;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.core.sharing.SharedEntityKey;
import com.docdoku.plm.server.core.sharing.SharedPart;
import com.docdoku.plm.server.core.util.NamingConvention;
import com.docdoku.plm.server.core.util.Tools;
import com.docdoku.plm.server.core.workflow.*;
import com.docdoku.plm.server.configuration.PSFilterVisitor;
import com.docdoku.plm.server.configuration.PSFilterVisitorCallbacks;
import com.docdoku.plm.server.configuration.filter.LatestCheckedInPSFilter;
import com.docdoku.plm.server.configuration.filter.UpdatePartIterationPSFilter;
import com.docdoku.plm.server.configuration.filter.WIPPSFilter;
import com.docdoku.plm.server.dao.*;
import com.docdoku.plm.server.events.*;
import com.docdoku.plm.server.factory.ACLFactory;
import com.docdoku.plm.server.validation.AttributesConsistencyUtils;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(IProductManagerLocal.class)
@Stateless(name = "ProductManagerBean")
public class ProductManagerBean implements IProductManagerLocal {

    @Inject
    private EntityManager em;

    @Inject
    private AccountDAO accountDAO;

    @Inject
    private ACLDAO aclDAO;

    @Inject
    private ACLFactory aclFactory;

    @Inject
    private BinaryResourceDAO binaryResourceDAO;

    @Inject
    private ChangeItemDAO changeItemDAO;

    @Inject
    private ConfigurationItemDAO configurationItemDAO;

    @Inject
    private ConversionDAO conversionDAO;

    @Inject
    private DocumentLinkDAO documentLinkDAO;

    @Inject
    private DocumentRevisionDAO documentRevisionDAO;

    @Inject
    private ImportDAO importDAO;

    @Inject
    private InstanceAttributeDAO instanceAttributeDAO;

    @Inject
    private LayerDAO layerDAO;

    @Inject
    private LOVDAO lovDAO;

    @Inject
    private MarkerDAO markerDAO;

    @Inject
    private ModificationNotificationDAO modificationNotificationDAO;

    @Inject
    private PartIterationDAO partIterationDAO;

    @Inject
    private PartMasterDAO partMasterDAO;

    @Inject
    private PartMasterTemplateDAO partMasterTemplateDAO;

    @Inject
    private PartRevisionDAO partRevisionDAO;

    @Inject
    private PartRevisionQueryDAO partRevisionQueryDAO;

    @Inject
    private PartUsageLinkDAO partUsageLinkDAO;

    @Inject
    private PathDataIterationDAO pathDataIterationDAO;

    @Inject
    private PathDataMasterDAO pathDataMasterDAO;

    @Inject
    private PathDataQueryDAO pathDataQueryDAO;

    @Inject
    private PathToPathLinkDAO pathToPathLinkDAO;

    @Inject
    private ProductConfigurationDAO productConfigurationDAO;

    @Inject
    private ProductBaselineDAO productBaselineDAO;

    @Inject
    private ProductInstanceMasterDAO productInstanceMasterDAO;

    @Inject
    private QueryDAO queryDAO;

    @Inject
    private RoleDAO roleDAO;

    @Inject
    private SharedEntityDAO sharedEntityDAO;

    @Inject
    private TagDAO tagDAO;

    @Inject
    private UserDAO userDAO;

    @Inject
    private UserGroupDAO userGroupDAO;

    @Inject
    private WorkflowModelDAO workflowModelDAO;

    @Inject
    private WorkspaceDAO workspaceDAO;

    @Inject
    private INotifierLocal mailer;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private IContextManagerLocal contextManager;

    @Inject
    private IBinaryStorageManagerLocal storageManager;

    @Inject
    private IIndexerManagerLocal indexerManager;

    @Inject
    private IPSFilterManagerLocal psFilterManager;

    @Inject
    private Event<TagEvent> tagEvent;

    @Inject
    private Event<PartIterationEvent> partIterationEvent;

    @Inject
    private Event<PartRevisionEvent> partRevisionEvent;

    @Inject
    private PSFilterVisitor psFilterVisitor;

    private static final Logger LOGGER = Logger.getLogger(ProductManagerBean.class.getName());

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartLink[]> findPartUsages(ConfigurationItemKey pKey, ProductStructureFilter filter, String search) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, NotAllowedException, EntityConstraintException, PartMasterNotFoundException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {

        String workspaceId = pKey.getWorkspace();
        userManager.checkWorkspaceReadAccess(workspaceId);

        List<PartLink[]> usagePaths = new ArrayList<>();

        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(pKey);

        psFilterVisitor.visit(workspaceId, filter, ci.getDesignItem(), -1, new PSFilterVisitorCallbacks() {
            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                PartMaster pm = parts.get(parts.size() - 1);

                if (pm.getNumber().matches(search) || (pm.getName() != null && pm.getName().matches(search)) || Tools.getPathAsString(path).equals(search)) {
                    PartLink[] partLinks = path.toArray(new PartLink[path.size()]);
                    usagePaths.add(partLinks);
                }
                return true;
            }
        });

        return usagePaths;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartMaster> findPartMasters(String pWorkspaceId, String pPartNumber, String pPartName, int pMaxResults) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        return partMasterDAO.findPartMasters(pWorkspaceId, pPartNumber, pPartName, pMaxResults);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ConfigurationItem createConfigurationItem(String pWorkspaceId, String pId, String pDescription, String pDesignItemNumber) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, ConfigurationItemAlreadyExistsException, CreationException, PartMasterNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        checkNameValidity(pId);

        ConfigurationItem ci = new ConfigurationItem(user, user.getWorkspace(), pId, pDescription);

        try {
            PartMaster designedPartMaster = partMasterDAO.loadPartM(new PartMasterKey(pWorkspaceId, pDesignItemNumber));
            ci.setDesignItem(designedPartMaster);
            configurationItemDAO.createConfigurationItem(ci);
            return ci;
        } catch (PartMasterNotFoundException e) {
            LOGGER.log(Level.FINEST, null, e);
            throw new PartMasterNotFoundException(pDesignItemNumber);
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMaster createPartMaster(String pWorkspaceId, String pNumber, String pName, boolean pStandardPart, String pWorkflowModelId, String pPartRevisionDescription, String templateId, Map<String, String> pACLUserEntries, Map<String, String> pACLUserGroupEntries, Map<String, Collection<String>> userRoleMapping, Map<String, Collection<String>> groupRoleMapping) throws NotAllowedException, UserNotFoundException, WorkspaceNotFoundException, AccessRightException, WorkflowModelNotFoundException, PartMasterAlreadyExistsException, CreationException, PartMasterTemplateNotFoundException, FileAlreadyExistsException, RoleNotFoundException, UserGroupNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);

        checkNumberValidity(pNumber);

        PartMaster pm = new PartMaster(user.getWorkspace(), pNumber, user);
        pm.setName(pName);
        pm.setStandardPart(pStandardPart);
        Date now = new Date();
        pm.setCreationDate(now);
        PartRevision newRevision = pm.createNextRevision(user);

        Collection<Task> runningTasks = null;
        if (pWorkflowModelId != null) {

            Map<Role, Collection<User>> roleUserMap = new HashMap<>();
            for (Map.Entry<String, Collection<String>> pair : userRoleMapping.entrySet()) {
                String roleName = pair.getKey();
                Collection<String> userLogins = pair.getValue();
                Role role = roleDAO.loadRole(new RoleKey(pWorkspaceId, roleName));
                Set<User> users = new HashSet<>();
                roleUserMap.put(role, users);
                for (String login : userLogins) {
                    User u = userDAO.loadUser(new UserKey(pWorkspaceId, login));
                    users.add(u);
                }
            }

            Map<Role, Collection<UserGroup>> roleGroupMap = new HashMap<>();
            for (Map.Entry<String, Collection<String>> pair : groupRoleMapping.entrySet()) {
                String roleName = pair.getKey();
                Collection<String> groupIds = pair.getValue();
                Role role = roleDAO.loadRole(new RoleKey(pWorkspaceId, roleName));
                Set<UserGroup> groups = new HashSet<>();
                roleGroupMap.put(role, groups);
                for (String groupId : groupIds) {
                    UserGroup g = userGroupDAO.loadUserGroup(new UserGroupKey(pWorkspaceId, groupId));
                    groups.add(g);
                }
            }

            WorkflowModel workflowModel = workflowModelDAO.loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
            Workflow workflow = workflowModel.createWorkflow(roleUserMap, roleGroupMap);
            newRevision.setWorkflow(workflow);

            for (Task task : workflow.getTasks()) {
                if (!task.hasPotentialWorker()) {
                    throw new NotAllowedException("NotAllowedException56");
                }
            }

            runningTasks = workflow.getRunningTasks();
            runningTasks.forEach(Task::start);
        }
        newRevision.setCheckOutUser(user);
        newRevision.setCheckOutDate(now);
        newRevision.setCreationDate(now);
        newRevision.setDescription(pPartRevisionDescription);
        PartIteration ite = newRevision.createNextIteration(user);
        ite.setCreationDate(now);

        if (templateId != null) {

            PartMasterTemplate partMasterTemplate = partMasterTemplateDAO.loadPartMTemplate(new PartMasterTemplateKey(pWorkspaceId, templateId));

            if (!Tools.validateMask(partMasterTemplate.getMask(), pNumber)) {
                throw new NotAllowedException("NotAllowedException42");
            }

            pm.setType(partMasterTemplate.getPartType());
            pm.setAttributesLocked(partMasterTemplate.isAttributesLocked());

            List<InstanceAttribute> attrs = new ArrayList<>();
            for (InstanceAttributeTemplate attrTemplate : partMasterTemplate.getAttributeTemplates()) {
                InstanceAttribute attr = attrTemplate.createInstanceAttribute();
                attrs.add(attr);
            }
            ite.setInstanceAttributes(attrs);

            BinaryResource sourceFile = partMasterTemplate.getAttachedFile();

            if (sourceFile != null) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                Date lastModified = sourceFile.getLastModified();
                String fullName = pWorkspaceId + "/parts/" + pm.getNumber() + "/A/1/nativecad/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                ite.setNativeCADFile(targetFile);
                try {
                    storageManager.copyData(sourceFile, targetFile);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }

        }

        if (pACLUserEntries != null && !pACLUserEntries.isEmpty() || pACLUserGroupEntries != null && !pACLUserGroupEntries.isEmpty()) {
            ACL acl = aclFactory.createACL(user.getWorkspace().getId(), pACLUserEntries, pACLUserGroupEntries);
            newRevision.setACL(acl);
        }

        partMasterDAO.createPartM(pm);

        if (runningTasks != null) {
            mailer.sendApproval(newRevision.getWorkspaceId(), runningTasks, newRevision);
        }

        return pm;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision undoCheckOutPart(PartRevisionKey pPartRPK) throws NotAllowedException, PartRevisionNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartRPK.getPartMaster().getWorkspace());

        PartRevision partR = partRevisionDAO.loadPartR(pPartRPK);
        if (partR.getACL() == null) {
            userManager.checkWorkspaceWriteAccess(pPartRPK.getWorkspaceId());
        }

        //Check access rights on partR
        if (!hasPartRevisionWriteAccess(user, partR)) {
            throw new AccessRightException(user);
        }

        if (isCheckoutByUser(user, partR)) {
            if (partR.getLastIteration().getIteration() <= 1) {
                throw new NotAllowedException("NotAllowedException41");
            }
            PartIteration partIte = partR.removeLastIteration();
            partIterationEvent.select(new AnnotationLiteral<Removed>() {
            }).fire(new PartIterationEvent(partIte));

            partIterationDAO.removeIteration(partIte);
            partR.setCheckOutDate(null);
            partR.setCheckOutUser(null);

            // Remove path to path links impacted by this change
            removeObsoletePathToPathLinks(pPartRPK.getWorkspaceId());

            for (Geometry file : partIte.getGeometries()) {
                try {
                    storageManager.deleteData(file);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }

            for (BinaryResource file : partIte.getAttachedFiles()) {
                try {
                    storageManager.deleteData(file);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }

            BinaryResource nativeCAD = partIte.getNativeCADFile();
            if (nativeCAD != null) {
                try {
                    storageManager.deleteData(nativeCAD);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }

            return partR;
        } else {
            throw new NotAllowedException("NotAllowedException19");
        }
    }

    private void removeObsoletePathToPathLinks(String workspaceId) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        List<ConfigurationItem> configurationItems = configurationItemDAO.findAllConfigurationItems(workspaceId);

        for (ConfigurationItem configurationItem : configurationItems) {
            List<PathToPathLink> pathToPathLinks = new ArrayList<>(configurationItem.getPathToPathLinks());
            for (PathToPathLink pathToPathLink : pathToPathLinks) {
                try {
                    decodePath(configurationItem.getKey(), pathToPathLink.getSourcePath());
                    decodePath(configurationItem.getKey(), pathToPathLink.getTargetPath());
                } catch (PartUsageLinkNotFoundException e) {
                    configurationItem.removePathToPathLink(pathToPathLink);
                } catch (ConfigurationItemNotFoundException e) {
                    // Should not be thrown
                    LOGGER.log(Level.SEVERE, null, e);
                }
            }
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision checkOutPart(PartRevisionKey pPartRPK) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, PartRevisionNotFoundException, NotAllowedException, FileAlreadyExistsException, CreationException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartRPK.getPartMaster().getWorkspace());

        PartRevision partR = partRevisionDAO.loadPartR(pPartRPK);
        if (partR.getACL() == null) {
            userManager.checkWorkspaceWriteAccess(pPartRPK.getWorkspaceId());
        }
        //Check access rights on partR
        if (!hasPartRevisionWriteAccess(user, partR)) {
            throw new AccessRightException(user);
        }

        if (!partR.isLastRevision()) {
            throw new NotAllowedException("NotAllowedException72");
        }

        if (partR.isCheckedOut()) {
            throw new NotAllowedException("NotAllowedException37");
        }

        if (partR.isReleased() || partR.isObsolete()) {
            throw new NotAllowedException("NotAllowedException47");
        }

        PartIteration beforeLastPartIteration = partR.getLastIteration();

        PartIteration newPartIteration = partR.createNextIteration(user);
        //We persist the doc as a workaround for a bug which was introduced
        //since glassfish 3 that set the DTYPE to null in the instance attribute table
        em.persist(newPartIteration);
        Date now = new Date();
        newPartIteration.setCreationDate(now);
        partR.setCheckOutUser(user);
        partR.setCheckOutDate(now);

        if (beforeLastPartIteration != null) {
            String partNumber = partR.getPartNumber();
            for (BinaryResource sourceFile : beforeLastPartIteration.getAttachedFiles()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                Date lastModified = sourceFile.getLastModified();
                String fullName = partR.getWorkspaceId() + "/parts/" + partNumber + "/" + partR.getVersion() + "/" + newPartIteration.getIteration() + "/attachedfiles/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                newPartIteration.addAttachedFile(targetFile);
            }

            newPartIteration.setComponents(new ArrayList<>(beforeLastPartIteration.getComponents()));

            for (Geometry sourceFile : beforeLastPartIteration.getGeometries()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                int quality = sourceFile.getQuality();
                Date lastModified = sourceFile.getLastModified();
                String fullName = partR.getWorkspaceId() + "/parts/" + partNumber + "/" + partR.getVersion() + "/" + newPartIteration.getIteration() + "/" + fileName;
                Geometry targetFile = new Geometry(quality, fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                newPartIteration.addGeometry(targetFile);
            }

            BinaryResource nativeCADFile = beforeLastPartIteration.getNativeCADFile();
            if (nativeCADFile != null) {
                String fileName = nativeCADFile.getName();
                long length = nativeCADFile.getContentLength();
                Date lastModified = nativeCADFile.getLastModified();
                String fullName = partR.getWorkspaceId() + "/parts/" + partNumber + "/" + partR.getVersion() + "/" + newPartIteration.getIteration() + "/nativecad/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                newPartIteration.setNativeCADFile(targetFile);
            }

            Set<DocumentLink> links = new HashSet<>();
            for (DocumentLink link : beforeLastPartIteration.getLinkedDocuments()) {
                DocumentLink newLink = link.clone();
                links.add(newLink);
            }
            newPartIteration.setLinkedDocuments(links);

            List<InstanceAttribute> attrs = new ArrayList<>();
            for (InstanceAttribute attr : beforeLastPartIteration.getInstanceAttributes()) {
                InstanceAttribute newAttr = attr.clone();
                //Workaround for the NULL DTYPE bug
                instanceAttributeDAO.createAttribute(newAttr);
                attrs.add(newAttr);
            }
            newPartIteration.setInstanceAttributes(attrs);

            List<InstanceAttributeTemplate> attrsTemplate = new ArrayList<>();
            for (InstanceAttributeTemplate attr : beforeLastPartIteration.getInstanceAttributeTemplates()) {
                InstanceAttributeTemplate newAttr = attr.clone();
                attrsTemplate.add(newAttr);
            }
            newPartIteration.setInstanceAttributeTemplates(attrsTemplate);

        }

        return partR;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision checkInPart(PartRevisionKey pPartRPK) throws PartRevisionNotFoundException, UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, EntityConstraintException, UserNotActiveException, PartMasterNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartRPK.getPartMaster().getWorkspace());

        PartRevision partR = partRevisionDAO.loadPartR(pPartRPK);

        if (partR.getACL() == null) {
            userManager.checkWorkspaceWriteAccess(pPartRPK.getWorkspaceId());
        }

        //Check access rights on partR
        if (!hasPartRevisionWriteAccess(user, partR)) {
            throw new AccessRightException(user);
        }

        if (isCheckoutByUser(user, partR)) {

            checkCyclicAssemblyForPartIteration(partR.getLastIteration());

            partR.setCheckOutDate(null);
            partR.setCheckOutUser(null);

            PartIteration lastIteration = partR.getLastIteration();
            lastIteration.setCheckInDate(new Date());

            indexerManager.indexPartIteration(lastIteration);

            partIterationEvent.select(new AnnotationLiteral<CheckedIn>() {
            }).fire(new PartIterationEvent(lastIteration));
            return partR;
        } else {
            throw new NotAllowedException("NotAllowedException20");
        }

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public BinaryResource getBinaryResource(String pFullName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, FileNotFoundException, NotAllowedException, AccessRightException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
        BinaryResource binaryResource = binaryResourceDAO.loadBinaryResource(pFullName);

        PartIteration partIte = binaryResourceDAO.getPartHolder(binaryResource);
        if (partIte != null) {
            PartRevision partR = partIte.getPartRevision();

            if (isACLGrantReadAccess(user, partR)) {
                if (isCheckoutByAnotherUser(user, partR) && partR.getLastIteration().equals(partIte)) {
                    throw new NotAllowedException("NotAllowedException34");
                } else {
                    return binaryResource;
                }
            } else {
                throw new AccessRightException(user);
            }
        } else {
            throw new FileNotFoundException(pFullName);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public BinaryResource getTemplateBinaryResource(String pFullName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, FileNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
        return binaryResourceDAO.loadBinaryResource(pFullName);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource saveNativeCADInPartIteration(PartIterationKey pPartIPK, String pName, long pSize) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartIPK.getWorkspaceId());
        checkNameFileValidity(pName);

        PartRevision partR = partRevisionDAO.loadPartR(pPartIPK.getPartRevision());
        PartIteration partI = partR.getIteration(pPartIPK.getIteration());

        if (isCheckoutByUser(user, partR) && partR.getLastIteration().equals(partI)) {
            String fullName = partR.getWorkspaceId() + "/parts/" + partR.getPartNumber() + "/" + partR.getVersion() + "/" + partI.getIteration() + "/nativecad/" + pName;
            BinaryResource nativeCADBinaryResource = partI.getNativeCADFile();

            if (nativeCADBinaryResource == null) {
                nativeCADBinaryResource = new BinaryResource(fullName, pSize, new Date());
                binaryResourceDAO.createBinaryResource(nativeCADBinaryResource);
                partI.setNativeCADFile(nativeCADBinaryResource);

            } else if (nativeCADBinaryResource.getFullName().equals(fullName)) {
                nativeCADBinaryResource.setContentLength(pSize);
                nativeCADBinaryResource.setLastModified(new Date());

            } else {
                partI.setNativeCADFile(null);
                binaryResourceDAO.removeBinaryResource(nativeCADBinaryResource);

                try {
                    storageManager.deleteData(nativeCADBinaryResource);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }

                nativeCADBinaryResource = new BinaryResource(fullName, pSize, new Date());
                binaryResourceDAO.createBinaryResource(nativeCADBinaryResource);
                partI.setNativeCADFile(nativeCADBinaryResource);
            }

            //Delete converted files if any
            List<Geometry> geometries = new ArrayList<>(partI.getGeometries());
            for (Geometry geometry : geometries) {
                partI.removeGeometry(geometry);
                binaryResourceDAO.removeBinaryResource(geometry);
                try {
                    storageManager.deleteData(geometry);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }
            return nativeCADBinaryResource;
        } else {
            throw new NotAllowedException("NotAllowedException4");
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource saveGeometryInPartIteration(PartIterationKey pPartIPK, String pName, int quality, long pSize, double[] box) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartIPK.getWorkspaceId());
        checkNameFileValidity(pName);

        PartRevision partR = partRevisionDAO.loadPartR(pPartIPK.getPartRevision());
        PartIteration partI = partR.getIteration(pPartIPK.getIteration());
        if (isCheckoutByUser(user, partR) && partR.getLastIteration().equals(partI)) {
            Geometry geometryBinaryResource = null;
            String fullName = partR.getWorkspaceId() + "/parts/" + partR.getPartNumber() + "/" + partR.getVersion() + "/" + partI.getIteration() + "/" + pName;

            for (Geometry geo : partI.getGeometries()) {
                if (geo.getFullName().equals(fullName)) {
                    geometryBinaryResource = geo;
                    break;
                }
            }
            if (geometryBinaryResource == null) {
                geometryBinaryResource = new Geometry(quality, fullName, pSize, new Date());
                binaryResourceDAO.createBinaryResource(geometryBinaryResource);
                partI.addGeometry(geometryBinaryResource);
            } else {
                geometryBinaryResource.setContentLength(pSize);
                geometryBinaryResource.setQuality(quality);
                geometryBinaryResource.setLastModified(new Date());
            }

            if (box != null) {
                geometryBinaryResource.setBox(box[0], box[1], box[2], box[3], box[4], box[5]);
            }

            return geometryBinaryResource;
        } else {
            throw new NotAllowedException("NotAllowedException4");
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource saveFileInPartIteration(PartIterationKey pPartIPK, String pName, String subType, long pSize) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartIPK.getWorkspaceId());
        checkNameFileValidity(pName);

        PartRevision partR = partRevisionDAO.loadPartR(pPartIPK.getPartRevision());
        PartIteration partI = partR.getIteration(pPartIPK.getIteration());
        if (isCheckoutByUser(user, partR) && partR.getLastIteration().equals(partI)) {
            BinaryResource binaryResource = null;
            String fullName = partR.getWorkspaceId() + "/parts/" + partR.getPartNumber() + "/" + partR.getVersion() + "/" + partI.getIteration() + "/" + (subType != null ? subType + "/" : "") + pName;

            for (BinaryResource bin : partI.getAttachedFiles()) {
                if (bin.getFullName().equals(fullName)) {
                    binaryResource = bin;
                    break;
                }
            }
            if (binaryResource == null) {
                binaryResource = new BinaryResource(fullName, pSize, new Date());
                binaryResourceDAO.createBinaryResource(binaryResource);
                partI.addAttachedFile(binaryResource);
            } else {
                binaryResource.setContentLength(pSize);
                binaryResource.setLastModified(new Date());
            }
            return binaryResource;
        } else {
            throw new NotAllowedException("NotAllowedException4");
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public List<ConfigurationItem> getConfigurationItems(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        if (!contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID))
            userManager.checkWorkspaceReadAccess(pWorkspaceId);

        return configurationItemDAO.findAllConfigurationItems(pWorkspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public List<ConfigurationItem> searchConfigurationItems(String pWorkspaceId, String q) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        List<ConfigurationItem> configurationItems = getConfigurationItems(pWorkspaceId);

        if (q == null || q.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String qLower = q.toLowerCase();

        return configurationItems.stream()
                .filter(configurationItem -> configurationItem.getId().toLowerCase().contains(qLower))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public ConfigurationItem getConfigurationItem(ConfigurationItemKey ciKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(ciKey.getWorkspace());
        ConfigurationItem configurationItem = em.find(ConfigurationItem.class, ciKey);
        if (configurationItem == null) {
            throw new ConfigurationItemNotFoundException(ciKey.getId());
        }
        return configurationItem;
    }

    /*
    * give pAttributes null for no modification, give an empty list for removing them
    * */
    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision updatePartIteration(PartIterationKey pKey, String pIterationNote, Source source, List<PartUsageLink> pUsageLinks, List<InstanceAttribute> pAttributes, List<InstanceAttributeTemplate> pAttributeTemplates, DocumentRevisionKey[] pLinkKeys, String[] documentLinkComments, String[] lovNames)
            throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, PartRevisionNotFoundException, PartMasterNotFoundException, EntityConstraintException, UserNotActiveException, ListOfValuesNotFoundException, PartUsageLinkNotFoundException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(pKey.getWorkspaceId());

        PartRevision partRev = partRevisionDAO.loadPartR(pKey.getPartRevision());
        if (partRev.getACL() == null) {
            userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());
        }
        //check access rights on partRevision
        if (!hasPartRevisionWriteAccess(user, partRev)) {
            throw new AccessRightException(user);
        }

        PartIteration partIte = partRev.getLastIteration();

        if (isCheckoutByUser(user, partRev) && partIte.getKey().equals(pKey)) {

            // Update linked documents

            if (pLinkKeys != null) {
                Set<DocumentLink> currentLinks = new HashSet<>(partIte.getLinkedDocuments());

                for (DocumentLink link : currentLinks) {
                    partIte.getLinkedDocuments().remove(link);
                }

                int counter = 0;
                for (DocumentRevisionKey link : pLinkKeys) {
                    DocumentLink newLink = new DocumentLink(documentRevisionDAO.loadDocR(link));
                    newLink.setComment(documentLinkComments[counter]);
                    documentLinkDAO.createLink(newLink);
                    partIte.getLinkedDocuments().add(newLink);
                    counter++;
                }

            }

            // Update attributes


            //should move that
            if (pAttributes != null) {
                List<InstanceAttribute> currentAttrs = partRev.getLastIteration().getInstanceAttributes();
                boolean valid = AttributesConsistencyUtils.hasValidChange(pAttributes, partRev.isAttributesLocked(), currentAttrs);
                if (!valid) {
                    throw new NotAllowedException("NotAllowedException59");
                }
                partRev.getLastIteration().setInstanceAttributes(pAttributes);
            }


            // Update attribute templates

            if (pAttributeTemplates != null) {

                List<InstanceAttributeTemplate> templateAttrs = new ArrayList<>();
                for (int i = 0; i < pAttributeTemplates.size(); i++) {
                    templateAttrs.add(pAttributeTemplates.get(i));
                    if (pAttributeTemplates.get(i) instanceof ListOfValuesAttributeTemplate) {
                        ListOfValuesAttributeTemplate lovAttr = (ListOfValuesAttributeTemplate) pAttributeTemplates.get(i);
                        ListOfValuesKey lovKey = new ListOfValuesKey(user.getWorkspaceId(), lovNames[i]);
                        lovAttr.setLov(lovDAO.loadLOV(lovKey));
                    }
                }
                if (!AttributesConsistencyUtils.isTemplateAttributesValid(templateAttrs, false)) {
                    throw new NotAllowedException("NotAllowedException59");
                }
                partIte.setInstanceAttributeTemplates(pAttributeTemplates);
            }

            // Update structure
            if (pUsageLinks != null) {

                List<PartUsageLink> links = new ArrayList<>();
                for (PartUsageLink usageLink : pUsageLinks) {
                    PartUsageLink partUsageLink = findOrCreatePartLink(usageLink);
                    links.add(partUsageLink);
                }
                partIte.setComponents(links);

                partUsageLinkDAO.removeOrphanPartLinks();
                removeObsoletePathToPathLinks(pKey.getWorkspaceId());
                checkCyclicAssemblyForPartIteration(partIte);

            }

            // Set note and date

            partIte.setIterationNote(pIterationNote);
            partIte.setModificationDate(new Date());
            partIte.setSource(source);

        } else {
            throw new NotAllowedException("NotAllowedException25", partIte.getPartNumber());
        }

        return partRev;

    }

    private PartUsageLink findOrCreatePartLink(PartUsageLink partUsageLink) throws PartUsageLinkNotFoundException {
        int id = partUsageLink.getId();
        PartUsageLink partLink;
        if (id != 0) {
            partLink = partUsageLinkDAO.loadPartUsageLink(id);
        } else {
            partLink = createNewPartLink(partUsageLink);
        }
        return partLink;
    }

    private PartUsageLink createNewPartLink(PartUsageLink partUsageLink) throws PartUsageLinkNotFoundException {

        PartUsageLink newLink = new PartUsageLink();

        newLink.setAmount(partUsageLink.getAmount());
        newLink.setOptional(partUsageLink.isOptional());
        newLink.setCadInstances(partUsageLink.getCadInstances());
        newLink.setComment(partUsageLink.getComment());
        newLink.setReferenceDescription(partUsageLink.getReferenceDescription());
        String linkUnit = partUsageLink.getUnit();
        newLink.setUnit(linkUnit != null && linkUnit.isEmpty() ? null : linkUnit);
        newLink.setComponent(partUsageLink.getComponent());

        List<PartSubstituteLink> substitutes = new ArrayList<>();

        for (PartSubstituteLink partSubstituteLink : partUsageLink.getSubstitutes()) {
            PartSubstituteLink newSubstituteLink = new PartSubstituteLink();
            newSubstituteLink.setAmount(partSubstituteLink.getAmount());
            newSubstituteLink.setCadInstances(partSubstituteLink.getCadInstances());
            newSubstituteLink.setComment(partSubstituteLink.getComment());
            newSubstituteLink.setReferenceDescription(partSubstituteLink.getReferenceDescription());
            String substituteUnit = partSubstituteLink.getUnit();
            newSubstituteLink.setUnit(substituteUnit != null && substituteUnit.isEmpty() ? null : substituteUnit);
            newSubstituteLink.setSubstitute(partSubstituteLink.getSubstitute());
            substitutes.add(newSubstituteLink);
        }

        newLink.setSubstitutes(substitutes);

        em.persist(newLink);

        return newLink;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public PartRevision getPartRevision(PartRevisionKey pPartRPK) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {

        User user = checkPartRevisionReadAccess(pPartRPK);

        PartRevision partR = partRevisionDAO.loadPartR(pPartRPK);

        if (isCheckoutByAnotherUser(user, partR)) {
            em.detach(partR);
            partR.removeLastIteration();
        }
        return partR;
    }


    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<ModificationNotification> getModificationNotifications(PartIterationKey pPartIPK) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        PartRevisionKey partRevisionKey = pPartIPK.getPartRevision();
        checkPartRevisionReadAccess(partRevisionKey);
        return modificationNotificationDAO.getModificationNotifications(pPartIPK);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void removeModificationNotificationsOnIteration(PartIterationKey pPartIPK) {
        //TODO insure access rights
        modificationNotificationDAO.removeModificationNotifications(pPartIPK);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void removeModificationNotificationsOnRevision(PartRevisionKey pPartRPK) {
        //TODO insure access rights
        modificationNotificationDAO.removeModificationNotifications(pPartRPK);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void createModificationNotifications(PartIteration modifiedPartIteration) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        //TODO insure access rights
        Set<PartIteration> impactedParts = new HashSet<>();
        impactedParts.addAll(getUsedByAsComponent(modifiedPartIteration.getPartRevisionKey()));
        impactedParts.addAll(getUsedByAsSubstitute(modifiedPartIteration.getPartRevisionKey()));

        for (PartIteration impactedPart : impactedParts) {
            if (impactedPart.isLastIteration()) {
                ModificationNotification notification = new ModificationNotification();
                notification.setImpactedPart(impactedPart);
                notification.setModifiedPart(modifiedPartIteration);
                modificationNotificationDAO.createModificationNotification(notification);
            }
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void updateModificationNotification(String pWorkspaceId, int pModificationNotificationId, String pAcknowledgementComment) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {

        ModificationNotification modificationNotification = modificationNotificationDAO.getModificationNotification(pModificationNotificationId);
        PartIterationKey partIKey = modificationNotification.getImpactedPart().getKey();
        PartRevisionKey partRKey = partIKey.getPartRevision();

        User user = userManager.checkWorkspaceWriteAccess(partRKey.getPartMaster().getWorkspace());

        PartRevision partR = partRevisionDAO.loadPartR(partRKey);

        //Check access rights on partR
        if (!hasPartRevisionWriteAccess(user, partR)) {
            throw new AccessRightException(user);
        }
        Date now = new Date();
        modificationNotification.setAcknowledgementComment(pAcknowledgementComment);
        modificationNotification.setAcknowledged(true);
        modificationNotification.setAcknowledgementDate(now);
        modificationNotification.setAcknowledgementAuthor(user);

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<PartIteration> getUsedByAsComponent(PartRevisionKey partRevisionKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        checkPartRevisionReadAccess(partRevisionKey);
        return partIterationDAO.findUsedByAsComponent(partRevisionKey.getPartMaster());
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<PartIteration> getUsedByAsSubstitute(PartRevisionKey partRevisionKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        checkPartRevisionReadAccess(partRevisionKey);
        return partIterationDAO.findUsedByAsSubstitute(partRevisionKey.getPartMaster());
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartIteration getPartIteration(PartIterationKey pPartIPK) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, PartIterationNotFoundException, NotAllowedException, WorkspaceNotEnabledException {

        PartRevisionKey partRevisionKey = pPartIPK.getPartRevision();
        User user = checkPartRevisionReadAccess(partRevisionKey);

        PartIteration partI = partIterationDAO.loadPartI(pPartIPK);
        PartRevision partR = partI.getPartRevision();
        partR.getIteration(pPartIPK.getIteration());
        PartIteration lastIteration = partR.getLastIteration();

        if (isCheckoutByAnotherUser(user, partR) && lastIteration.getKey().equals(pPartIPK)) {
            throw new NotAllowedException("NotAllowedException25", partR.getPartMaster().getNumber());
        }

        return partI;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void updatePartRevisionACL(String workspaceId, PartRevisionKey revisionKey, Map<String, String> pACLUserEntries, Map<String, String> pACLUserGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(workspaceId);

        PartRevision partRevision = partRevisionDAO.loadPartR(revisionKey);

        if (isAuthor(user, partRevision) || user.isAdministrator()) {

            if (partRevision.getACL() == null) {
                ACL acl = aclFactory.createACL(workspaceId, pACLUserEntries, pACLUserGroupEntries);
                partRevision.setACL(acl);
            } else {
                aclFactory.updateACL(workspaceId, partRevision.getACL(), pACLUserEntries, pACLUserGroupEntries);
            }
        } else {
            throw new AccessRightException(user);
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeACLFromPartRevision(PartRevisionKey revisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(revisionKey.getPartMaster().getWorkspace());

        PartRevision partRevision = partRevisionDAO.loadPartR(revisionKey);

        if (isAuthor(user, partRevision) || user.isAdministrator()) {
            ACL acl = partRevision.getACL();
            if (acl != null) {
                aclDAO.removeACLEntries(acl);
                partRevision.setACL(null);
            }
        } else {
            throw new AccessRightException(user);
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartRevision> searchPartRevisions(PartSearchQuery pQuery, int from, int size)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccountNotFoundException, NotAllowedException, IndexerRequestException, IndexerNotAvailableException {
        User user = userManager.checkWorkspaceReadAccess(pQuery.getWorkspaceId());
        List<PartRevision> fetchedPartRs = indexerManager.searchPartRevisions(pQuery, from, size);
        // Get Search Results

        ListIterator<PartRevision> ite = fetchedPartRs.listIterator();
        while (ite.hasNext()) {
            PartRevision partR = ite.next();

            if (isCheckoutByAnotherUser(user, partR)) {
                // Remove CheckedOut PartRevision From Results
                em.detach(partR);
                partR.removeLastIteration();
            }

            if (!hasPartRevisionReadAccess(user, partR)) {
                ite.remove();
            }
        }
        return new ArrayList<>(fetchedPartRs);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMaster findPartMasterByCADFileName(String workspaceId, String cadFileName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);

        BinaryResource br = binaryResourceDAO.findNativeCadBinaryResourceInWorkspace(workspaceId, cadFileName);
        if (br == null) {
            return null;
        }
        String partNumber = br.getHolderId();
        PartMasterKey partMasterKey = new PartMasterKey(workspaceId, partNumber);
        try {
            return partMasterDAO.loadPartM(partMasterKey);
        } catch (PartMasterNotFoundException e) {
            return null;
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Conversion getConversion(PartIterationKey partIterationKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, PartIterationNotFoundException, WorkspaceNotEnabledException {
        checkPartRevisionReadAccess(partIterationKey.getPartRevision());
        PartIteration partIteration = partIterationDAO.loadPartI(partIterationKey);
        return conversionDAO.findConversion(partIteration);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public Conversion createConversion(PartIterationKey partIterationKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, PartIterationNotFoundException, CreationException, WorkspaceNotEnabledException {
        checkPartRevisionWriteAccess(partIterationKey.getPartRevision());
        PartIteration partIteration = partIterationDAO.loadPartI(partIterationKey);
        Conversion conversion = new Conversion(partIteration);
        conversionDAO.createConversion(conversion);
        return conversion;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void removeConversion(PartIterationKey partIterationKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, PartIterationNotFoundException, WorkspaceNotEnabledException {
        checkPartRevisionWriteAccess(partIterationKey.getPartRevision());
        PartIteration partIteration = partIterationDAO.loadPartI(partIterationKey);
        Conversion conversion = conversionDAO.findConversion(partIteration);
        conversionDAO.deleteConversion(conversion);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void endConversion(PartIterationKey partIterationKey, boolean succeed) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, PartIterationNotFoundException, WorkspaceNotEnabledException {
        checkPartRevisionWriteAccess(partIterationKey.getPartRevision());
        PartIteration partIteration = partIterationDAO.loadPartI(partIterationKey);
        Conversion conversion = conversionDAO.findConversion(partIteration);
        conversion.setPending(false);
        conversion.setSucceed(succeed);
        conversion.setEndDate(new Date());
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public Import createImport(String workspaceId, String fileName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, CreationException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Import importToCreate = new Import(user, fileName);
        importDAO.createImport(importToCreate);
        return importToCreate;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<Import> getImports(String workspaceId, String filename)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        return importDAO.findImports(user, filename);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Import getImport(String workspaceId, String id) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Import anImport = importDAO.findImport(user, id);
        if (anImport.getUser().equals(user)) {
            return anImport;
        } else {
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void endImport(String workspaceId, String id, ImportResult importResult) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Import anImport = importDAO.findImport(user, id);
        anImport.setPending(false);
        anImport.setEndDate(new Date());
        if (importResult != null) {
            anImport.setErrors(importResult.getErrors());
            anImport.setWarnings(importResult.getWarnings());
            anImport.setSucceed(importResult.isSucceed());
        } else {
            anImport.setSucceed(false);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeImport(String workspaceId, String id) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Import anImport = importDAO.findImport(user, id);
        if (anImport.getUser().equals(user)) {
            importDAO.deleteImport(anImport);
        } else {
            throw new AccessRightException(user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void updateACLForPartMasterTemplate(String pWorkspaceId, String templateId, Map<String, String> userEntries, Map<String, String> groupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, PartMasterTemplateNotFoundException, WorkspaceNotEnabledException {

        // Check the read access to the workspace
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        // Load the part template
        PartMasterTemplateKey pKey = new PartMasterTemplateKey(pWorkspaceId, templateId);
        PartMasterTemplate partMasterTemplate = partMasterTemplateDAO.loadPartMTemplate(pKey);

        // Check the access to the part template
        checkPartTemplateWriteAccess(partMasterTemplate, user);

        if (partMasterTemplate.getAcl() == null) {
            ACL acl = aclFactory.createACL(pWorkspaceId, userEntries, groupEntries);
            partMasterTemplate.setAcl(acl);
        } else {
            aclFactory.updateACL(pWorkspaceId, partMasterTemplate.getAcl(), userEntries, groupEntries);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeACLFromPartMasterTemplate(String workspaceId, String partTemplateId) throws PartMasterNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartMasterTemplateNotFoundException, AccessRightException, WorkspaceNotEnabledException {

        // Check the read access to the workspace
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        // Load the part template
        PartMasterTemplateKey pKey = new PartMasterTemplateKey(workspaceId, partTemplateId);
        PartMasterTemplate partMaster = partMasterTemplateDAO.loadPartMTemplate(pKey);

        // Check the access to the part template
        checkPartTemplateWriteAccess(partMaster, user);

        ACL acl = partMaster.getAcl();
        if (acl != null) {
            aclDAO.removeACLEntries(acl);
            partMaster.setAcl(null);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision saveTags(PartRevisionKey revisionKey, String[] pTags) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {

        User user = checkPartRevisionWriteAccess(revisionKey);

        PartRevision partRevision = partRevisionDAO.loadPartR(revisionKey);

        Set<Tag> tags = new HashSet<>();
        if (pTags != null) {
            for (String label : pTags) {
                tags.add(new Tag(user.getWorkspace(), label));
            }

            List<Tag> existingTags = Arrays.asList(tagDAO.findAllTags(user.getWorkspaceId()));

            Set<Tag> tagsToCreate = new HashSet<>(tags);
            tagsToCreate.removeAll(existingTags);

            for (Tag t : tagsToCreate) {
                try {
                    tagDAO.createTag(t);
                } catch (CreationException | TagAlreadyExistsException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }

            Set<Tag> removedTags = new HashSet<>(partRevision.getTags());
            removedTags.removeAll(tags);

            Set<Tag> addedTags = partRevision.setTags(tags);

            for (Tag tag : removedTags) {
                tagEvent.select(new AnnotationLiteral<Untagged>() {
                }).fire(new TagEvent(tag, partRevision));
            }
            for (Tag tag : addedTags) {
                tagEvent.select(new AnnotationLiteral<Tagged>() {
                }).fire(new TagEvent(tag, partRevision));
            }

            if (isCheckoutByAnotherUser(user, partRevision)) {
                em.detach(partRevision);
                partRevision.removeLastIteration();
            }

            partRevision.getPartIterations().forEach(indexerManager::indexPartIteration);

        } else {
            throw new IllegalArgumentException("pTags argument must not be null");
        }

        return partRevision;

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision removeTag(PartRevisionKey partRevisionKey, String tagName) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = checkPartRevisionWriteAccess(partRevisionKey);

        PartRevision partRevision = getPartRevision(partRevisionKey);
        Tag tagToRemove = new Tag(user.getWorkspace(), tagName);
        partRevision.getTags().remove(tagToRemove);

        tagEvent.select(new AnnotationLiteral<Untagged>() {
        }).fire(new TagEvent(tagToRemove, partRevision));

        if (isCheckoutByAnotherUser(user, partRevision)) {
            em.detach(partRevision);
            partRevision.removeLastIteration();
        }

        partRevision.getPartIterations().forEach(indexerManager::indexPartIteration);

        return partRevision;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision[] findPartRevisionsByTag(String workspaceId, String tagId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(workspaceId);

        List<PartRevision> partsRevision = partRevisionDAO.findPartByTag(new Tag(user.getWorkspace(), tagId));
        ListIterator<PartRevision> iterator = partsRevision.listIterator();
        while (iterator.hasNext()) {
            PartRevision partRevision = iterator.next();
            if (!hasPartRevisionReadAccess(user, partRevision)) {
                iterator.remove();
            } else if (isCheckoutByAnotherUser(user, partRevision)) {
                em.detach(partRevision);
                partRevision.removeLastIteration();
            }
        }
        return partsRevision.toArray(new PartRevision[partsRevision.size()]);
    }


    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision[] getPartRevisionsWithReferenceOrName(String pWorkspaceId, String reference, int maxResults) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<PartRevision> partRs = partRevisionDAO.findPartsRevisionsWithReferenceOrNameLike(pWorkspaceId, reference, maxResults);
        return partRs.toArray(new PartRevision[partRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision releasePartRevision(PartRevisionKey pRevisionKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, NotAllowedException, WorkspaceNotEnabledException {
        User user = checkPartRevisionWriteAccess(pRevisionKey);                                                         // Check if the user can write the part

        PartRevision partRevision = partRevisionDAO.loadPartR(pRevisionKey);

        if (partRevision.isCheckedOut()) {
            throw new NotAllowedException("NotAllowedException46");
        }

        if (partRevision.getNumberOfIterations() == 0) {
            throw new NotAllowedException("NotAllowedException41");
        }

        if (partRevision.isObsolete()) {
            throw new NotAllowedException("NotAllowedException38");
        }

        partRevision.release(user);
        return partRevision;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision markPartRevisionAsObsolete(PartRevisionKey pRevisionKey) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, NotAllowedException, WorkspaceNotEnabledException {
        User user = checkPartRevisionWriteAccess(pRevisionKey);                                                         // Check if the user can write the part

        PartRevision partRevision = partRevisionDAO.loadPartR(pRevisionKey);

        if (!partRevision.isReleased()) {
            throw new NotAllowedException("NotAllowedException36");
        }

        partRevision.markAsObsolete(user);
        return partRevision;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision getLastReleasePartRevision(ConfigurationItemKey ciKey)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, AccessRightException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(ciKey.getWorkspace());
        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(ciKey);
        PartMaster partMaster = ci.getDesignItem();
        PartRevision lastReleasedRevision = partMaster.getLastReleasedRevision();
        if (lastReleasedRevision == null) {
            throw new PartRevisionNotFoundException(partMaster.getNumber(), "Released");
        }
        if (!canUserAccess(user, lastReleasedRevision.getKey())) {
            throw new AccessRightException(user);
        }
        return lastReleasedRevision;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ProductBaseline> findBaselinesWherePartRevisionHasIterations(PartRevisionKey partRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(partRevisionKey.getPartMaster().getWorkspace());
        PartRevision partRevision = partRevisionDAO.loadPartR(partRevisionKey);
        return productBaselineDAO.findBaselineWherePartRevisionHasIterations(partRevision);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartUsageLink> getComponents(PartIterationKey pPartIPK) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartIterationNotFoundException, NotAllowedException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartIPK.getWorkspaceId());
        PartIteration partI = partIterationDAO.loadPartI(pPartIPK);
        PartRevision partR = partI.getPartRevision();

        if (isCheckoutByAnotherUser(user, partR) && partR.getLastIteration().equals(partI)) {
            throw new NotAllowedException("NotAllowedException34");
        }
        return partI.getComponents();
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean partMasterExists(PartMasterKey partMasterKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(partMasterKey.getWorkspace());
        try {
            partMasterDAO.loadPartM(partMasterKey);
            return true;
        } catch (PartMasterNotFoundException e) {
            LOGGER.log(Level.FINEST, null, e);
            return false;
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteConfigurationItem(ConfigurationItemKey configurationItemKey) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, UserNotActiveException, ConfigurationItemNotFoundException, LayerNotFoundException, EntityConstraintException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceWriteAccess(configurationItemKey.getWorkspace());

        List<ProductConfiguration> productConfigurations = productConfigurationDAO.getAllProductConfigurationsByConfigurationItem(configurationItemKey);

        if (!productConfigurations.isEmpty()) {
            throw new EntityConstraintException("EntityConstraintException23");
        }

        List<ProductBaseline> productBaselines = productBaselineDAO.findBaselines(configurationItemKey.getId(), configurationItemKey.getWorkspace());

        if (!productBaselines.isEmpty()) {
            throw new EntityConstraintException("EntityConstraintException4");
        }

        List<ProductInstanceMaster> productInstanceMasters = productInstanceMasterDAO.findProductInstanceMasters(configurationItemKey.getId(), configurationItemKey.getWorkspace());

        if (!productInstanceMasters.isEmpty()) {
            throw new EntityConstraintException("EntityConstraintException13");
        }

        configurationItemDAO.removeConfigurationItem(configurationItemKey);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteLayer(String workspaceId, int layerId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, LayerNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        Layer layer = layerDAO.loadLayer(layerId);
        userManager.checkWorkspaceWriteAccess(layer.getConfigurationItem().getWorkspaceId());
        layerDAO.deleteLayer(layer);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource renameFileInPartIteration(String pSubType, String pFullName, String pNewName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, FileNotFoundException, FileAlreadyExistsException, CreationException, StorageException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));

        checkNameFileValidity(pNewName);

        BinaryResource file = binaryResourceDAO.loadBinaryResource(pFullName);
        PartIteration partIteration = binaryResourceDAO.getPartHolder(file);

        PartRevision partR = partIteration.getPartRevision();

        if (isCheckoutByUser(user, partR) && partR.getLastIteration().equals(partIteration)) {

            storageManager.renameFile(file, pNewName);

            if (PartIteration.NATIVE_CAD_SUBTYPE.equals(pSubType)) {
                partIteration.setNativeCADFile(null);
            } else {
                partIteration.removeAttachedFile(file);
            }
            binaryResourceDAO.removeBinaryResource(file);

            BinaryResource newFile = new BinaryResource(file.getNewFullName(pNewName), file.getContentLength(), file.getLastModified());

            binaryResourceDAO.createBinaryResource(newFile);

            if (PartIteration.NATIVE_CAD_SUBTYPE.equals(pSubType)) {
                partIteration.setNativeCADFile(newFile);
            } else {
                partIteration.addAttachedFile(newFile);
            }

            return newFile;
        } else {
            throw new NotAllowedException("NotAllowedException35");
        }

    }

    private void removeCADFile(PartIteration partIteration)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartIterationNotFoundException {

        // Delete native cad file
        BinaryResource br = partIteration.getNativeCADFile();
        if (br != null) {
            try {
                storageManager.deleteData(br);
            } catch (StorageException e) {
                LOGGER.log(Level.INFO, null, e);
            }
            partIteration.setNativeCADFile(null);
        }

        // Delete generated 3D files
        List<Geometry> geometries = new ArrayList<>(partIteration.getGeometries());
        for (Geometry geometry : geometries) {
            try {
                storageManager.deleteData(geometry);
            } catch (StorageException e) {
                LOGGER.log(Level.INFO, null, e);
            }
            partIteration.removeGeometry(geometry);
        }
    }

    private void removeAttachedFiles(PartIteration partIteration)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartIterationNotFoundException {

        // Delete attached files
        for (BinaryResource file : partIteration.getAttachedFiles()) {
            try {
                storageManager.deleteData(file);
            } catch (StorageException e) {
                LOGGER.log(Level.INFO, null, e);
            }
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeFileInPartIteration(PartIterationKey pPartIPK, String pSubType, String pName)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartIterationNotFoundException, FileNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(pPartIPK.getWorkspaceId());

        PartIteration partIteration = partIterationDAO.loadPartI(pPartIPK);
        PartRevision partR = partIteration.getPartRevision();

        if (isCheckoutByUser(user, partR) && partR.getLastIteration().equals(partIteration)) {
            BinaryResource file = binaryResourceDAO.loadBinaryResource(pName);

            if (PartIteration.NATIVE_CAD_SUBTYPE.equals(pSubType)) {
                partIteration.setNativeCADFile(null);
            } else {
                partIteration.removeAttachedFile(file);
            }

            try {
                storageManager.deleteData(file);
            } catch (StorageException e) {
                LOGGER.log(Level.INFO, null, e);
            }

            binaryResourceDAO.removeBinaryResource(file);
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void setPublicSharedPart(PartRevisionKey pPartRPK, boolean isPublicShared) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        PartRevision partRevision = getPartRevision(pPartRPK);
        partRevision.setPublicShared(isPublicShared);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMaster getPartMaster(PartMasterKey pPartMPK) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartMasterNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartMPK.getWorkspace());
        PartMaster partM = partMasterDAO.loadPartM(pPartMPK);

        partM.getPartRevisions()
                .stream()
                .filter(partR -> isCheckoutByAnotherUser(user, partR))
                .forEach(partR -> {
                    em.detach(partR);
                    partR.removeLastIteration();
                });

        return partM;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<Layer> getLayers(ConfigurationItemKey pKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pKey.getWorkspace());
        return layerDAO.findAllLayers(pKey);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Layer getLayer(int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, LayerNotFoundException, WorkspaceNotEnabledException {
        Layer layer = layerDAO.loadLayer(pId);
        userManager.checkWorkspaceReadAccess(layer.getConfigurationItem().getWorkspaceId());
        return layer;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Layer createLayer(ConfigurationItemKey pKey, String pName, String color) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspace());
        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(pKey);
        Layer layer = new Layer(pName, user, ci, color);
        Date now = new Date();
        layer.setCreationDate(now);
        layerDAO.createLayer(layer);
        return layer;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Layer updateLayer(ConfigurationItemKey pKey, int pId, String pName, String color) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, ConfigurationItemNotFoundException, LayerNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        Layer layer = getLayer(pId);
        userManager.checkWorkspaceWriteAccess(layer.getConfigurationItem().getWorkspaceId());
        layer.setName(pName);
        layer.setColor(color);
        return layer;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Marker createMarker(int pLayerId, String pTitle, String pDescription, double pX, double pY, double pZ) throws LayerNotFoundException, UserNotFoundException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        Layer layer = layerDAO.loadLayer(pLayerId);
        User user = userManager.checkWorkspaceWriteAccess(layer.getConfigurationItem().getWorkspaceId());
        Marker marker = new Marker(pTitle, user, pDescription, pX, pY, pZ);
        Date now = new Date();
        marker.setCreationDate(now);
        markerDAO.createMarker(marker);
        layer.addMarker(marker);
        return marker;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteMarker(int pLayerId, int pMarkerId) throws WorkspaceNotFoundException, UserNotActiveException, LayerNotFoundException, UserNotFoundException, AccessRightException, MarkerNotFoundException, WorkspaceNotEnabledException {
        Layer layer = layerDAO.loadLayer(pLayerId);
        userManager.checkWorkspaceWriteAccess(layer.getConfigurationItem().getWorkspaceId());

        Marker marker = markerDAO.loadMarker(pMarkerId);

        if (layer.getMarkers().contains(marker)) {
            layer.removeMarker(marker);
            em.flush();
            markerDAO.removeMarker(pMarkerId);
        } else {
            throw new MarkerNotFoundException(pMarkerId);
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMasterTemplate[] getPartMasterTemplates(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<PartMasterTemplate> templates = partMasterTemplateDAO.findAllPartMTemplates(pWorkspaceId);

        ListIterator<PartMasterTemplate> ite = templates.listIterator();
        while (ite.hasNext()) {
            PartMasterTemplate template = ite.next();
            if (!hasPartTemplateReadAccess(user, template)) {
                ite.remove();
            }
        }

        return templates.toArray(new PartMasterTemplate[templates.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMasterTemplate getPartMasterTemplate(PartMasterTemplateKey pKey) throws WorkspaceNotFoundException, PartMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pKey.getWorkspaceId());
        return partMasterTemplateDAO.loadPartMTemplate(pKey);
    }


    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMasterTemplate createPartMasterTemplate(String pWorkspaceId, String pId, String pPartType, String pWorkflowModelId, String pMask, List<InstanceAttributeTemplate> pAttributeTemplates, String[] lovNames, List<InstanceAttributeTemplate> pAttributeInstanceTemplates, String[] instanceLovNames, boolean idGenerated, boolean attributesLocked) throws WorkspaceNotFoundException, AccessRightException, PartMasterTemplateAlreadyExistsException, UserNotFoundException, NotAllowedException, CreationException, WorkflowModelNotFoundException, ListOfValuesNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);

        checkNameValidity(pId);

        //Check pMask
        if (pMask != null && !pMask.isEmpty() && !NamingConvention.correctNameMask(pMask)) {
            throw new NotAllowedException("MaskCreationException");
        }

        PartMasterTemplate template = new PartMasterTemplate(user.getWorkspace(), pId, user, pPartType, pMask, attributesLocked);
        Date now = new Date();
        template.setCreationDate(now);
        template.setIdGenerated(idGenerated);

        List<InstanceAttributeTemplate> attrs = new ArrayList<>();
        for (int i = 0; i < pAttributeTemplates.size(); i++) {
            if (attributesLocked) {
                pAttributeTemplates.get(i).setLocked(true);
            }
            attrs.add(pAttributeTemplates.get(i));
            if (pAttributeTemplates.get(i) instanceof ListOfValuesAttributeTemplate) {
                ListOfValuesAttributeTemplate lovAttr = (ListOfValuesAttributeTemplate) pAttributeTemplates.get(i);
                ListOfValuesKey lovKey = new ListOfValuesKey(user.getWorkspaceId(), lovNames[i]);
                lovAttr.setLov(lovDAO.loadLOV(lovKey));
            }
        }
        if (!AttributesConsistencyUtils.isTemplateAttributesValid(attrs, attributesLocked)) {
            throw new NotAllowedException("NotAllowedException59");
        }
        template.setAttributeTemplates(attrs);

        List<InstanceAttributeTemplate> instanceAttrs = new ArrayList<>();
        for (int i = 0; i < pAttributeInstanceTemplates.size(); i++) {
            instanceAttrs.add(pAttributeInstanceTemplates.get(i));
            if (pAttributeInstanceTemplates.get(i) instanceof ListOfValuesAttributeTemplate) {
                ListOfValuesAttributeTemplate lovAttr = (ListOfValuesAttributeTemplate) pAttributeInstanceTemplates.get(i);
                ListOfValuesKey lovKey = new ListOfValuesKey(user.getWorkspaceId(), instanceLovNames[i]);
                lovAttr.setLov(lovDAO.loadLOV(lovKey));
            }
        }
        if (!AttributesConsistencyUtils.isTemplateAttributesValid(instanceAttrs, false)) {
            throw new NotAllowedException("NotAllowedException59");
        }
        template.setAttributeInstanceTemplates(instanceAttrs);

        if (pWorkflowModelId != null) {
            WorkflowModel workflowModel = workflowModelDAO.loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
            template.setWorkflowModel(workflowModel);
        }

        partMasterTemplateDAO.createPartMTemplate(template);
        return template;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMasterTemplate updatePartMasterTemplate(PartMasterTemplateKey pKey, String pPartType, String pWorkflowModelId, String pMask, List<InstanceAttributeTemplate> pAttributeTemplates, String[] lovNames, List<InstanceAttributeTemplate> pAttributeInstanceTemplates, String[] instanceLovNames, boolean idGenerated, boolean attributesLocked) throws WorkspaceNotFoundException, AccessRightException, PartMasterTemplateNotFoundException, UserNotFoundException, WorkflowModelNotFoundException, UserNotActiveException, ListOfValuesNotFoundException, NotAllowedException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pKey.getWorkspaceId());

        PartMasterTemplate template = partMasterTemplateDAO.loadPartMTemplate(pKey);

        checkPartTemplateWriteAccess(template, user);

        Date now = new Date();
        template.setModificationDate(now);
        template.setAuthor(user);
        template.setPartType(pPartType);
        template.setMask(pMask);
        template.setIdGenerated(idGenerated);
        template.setAttributesLocked(attributesLocked);

        List<InstanceAttributeTemplate> attrs = new ArrayList<>();
        for (int i = 0; i < pAttributeTemplates.size(); i++) {
            pAttributeTemplates.get(i).setLocked(attributesLocked);
            attrs.add(pAttributeTemplates.get(i));
            if (pAttributeTemplates.get(i) instanceof ListOfValuesAttributeTemplate) {
                ListOfValuesAttributeTemplate lovAttr = (ListOfValuesAttributeTemplate) pAttributeTemplates.get(i);
                ListOfValuesKey lovKey = new ListOfValuesKey(user.getWorkspaceId(), lovNames[i]);
                lovAttr.setLov(lovDAO.loadLOV(lovKey));
            }
        }
        if (!AttributesConsistencyUtils.isTemplateAttributesValid(attrs, attributesLocked)) {
            throw new NotAllowedException("NotAllowedException59");
        }
        template.setAttributeTemplates(attrs);

        List<InstanceAttributeTemplate> instanceAttrs = new ArrayList<>();
        for (int i = 0; i < pAttributeInstanceTemplates.size(); i++) {
            instanceAttrs.add(pAttributeInstanceTemplates.get(i));
            if (pAttributeInstanceTemplates.get(i) instanceof ListOfValuesAttributeTemplate) {
                ListOfValuesAttributeTemplate lovAttr = (ListOfValuesAttributeTemplate) pAttributeInstanceTemplates.get(i);
                ListOfValuesKey lovKey = new ListOfValuesKey(user.getWorkspaceId(), instanceLovNames[i]);
                lovAttr.setLov(lovDAO.loadLOV(lovKey));
            }
        }
        if (!AttributesConsistencyUtils.isTemplateAttributesValid(instanceAttrs, false)) {
            throw new NotAllowedException("NotAllowedException59");
        }
        template.setAttributeInstanceTemplates(instanceAttrs);

        WorkflowModel workflowModel = null;
        if (pWorkflowModelId != null) {
            workflowModel = workflowModelDAO.loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
        }
        template.setWorkflowModel(workflowModel);

        return template;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deletePartMasterTemplate(PartMasterTemplateKey pKey) throws WorkspaceNotFoundException, AccessRightException, PartMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pKey.getWorkspaceId());

        PartMasterTemplate partMasterTemplate = partMasterTemplateDAO.loadPartMTemplate(pKey);
        checkPartTemplateWriteAccess(partMasterTemplate, user);

        PartMasterTemplate template = partMasterTemplateDAO.removePartMTemplate(pKey);
        BinaryResource file = template.getAttachedFile();
        if (file != null) {
            try {
                storageManager.deleteData(file);
            } catch (StorageException e) {
                LOGGER.log(Level.INFO, null, e);
            }
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource saveFileInTemplate(PartMasterTemplateKey pPartMTemplateKey, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, PartMasterTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pPartMTemplateKey.getWorkspaceId());

        checkNameFileValidity(pName);

        PartMasterTemplate template = partMasterTemplateDAO.loadPartMTemplate(pPartMTemplateKey);

        checkPartTemplateWriteAccess(template, user);

        BinaryResource binaryResource = null;
        String fullName = template.getWorkspaceId() + "/part-templates/" + template.getId() + "/" + pName;

        BinaryResource bin = template.getAttachedFile();
        if (bin != null && bin.getFullName().equals(fullName)) {
            binaryResource = bin;
        } else if (bin != null && !bin.getFullName().equals(fullName)) {
            try {
                storageManager.deleteData(bin);
            } catch (StorageException e) {
                LOGGER.log(Level.WARNING, "Could not delete attached file", e);
            }
        }

        if (binaryResource == null) {
            binaryResource = new BinaryResource(fullName, pSize, new Date());
            binaryResourceDAO.createBinaryResource(binaryResource);
            template.setAttachedFile(binaryResource);
        } else {
            binaryResource.setContentLength(pSize);
            binaryResource.setLastModified(new Date());
        }
        return binaryResource;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartMasterTemplate removeFileFromTemplate(String pFullName) throws WorkspaceNotFoundException, PartMasterTemplateNotFoundException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
        BinaryResource file = binaryResourceDAO.loadBinaryResource(pFullName);

        PartMasterTemplate template = binaryResourceDAO.getPartTemplateHolder(file);
        checkPartTemplateWriteAccess(template, user);

        template.setAttachedFile(null);
        binaryResourceDAO.removeBinaryResource(file);

        try {
            storageManager.deleteData(file);
        } catch (StorageException e) {
            LOGGER.log(Level.INFO, null, e);
        }
        return template;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource renameFileInTemplate(String pFullName, String pNewName) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, FileNotFoundException, UserNotActiveException, FileAlreadyExistsException, CreationException, StorageException, NotAllowedException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));

        checkNameFileValidity(pNewName);

        BinaryResource file = binaryResourceDAO.loadBinaryResource(pFullName);

        PartMasterTemplate template = binaryResourceDAO.getPartTemplateHolder(file);

        checkPartTemplateWriteAccess(template, user);

        storageManager.renameFile(file, pNewName);

        template.setAttachedFile(null);
        binaryResourceDAO.removeBinaryResource(file);

        BinaryResource newFile = new BinaryResource(file.getNewFullName(pNewName), file.getContentLength(), file.getLastModified());

        binaryResourceDAO.createBinaryResource(newFile);
        template.setAttachedFile(newFile);

        return newFile;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartMaster> getPartMasters(String pWorkspaceId, int start, int pMaxResults) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return partMasterDAO.getPartMasters(pWorkspaceId, start, pMaxResults);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartRevision> getPartRevisions(String pWorkspaceId, int start, int pMaxResults) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<PartRevision> partRevisions;

        // potential OOM => should restrict pMaxResults
        if (pMaxResults == 0) {
            partRevisions = partRevisionDAO.getAllPartRevisions(pWorkspaceId);
        } else {
            partRevisions = partRevisionDAO.getPartRevisions(pWorkspaceId, start, pMaxResults);
        }

        List<PartRevision> filteredPartRevisions = new ArrayList<>();

        for (PartRevision partRevision : partRevisions) {
            try {
                checkPartRevisionReadAccess(partRevision.getKey());

                if (isCheckoutByAnotherUser(user, partRevision)) {
                    em.detach(partRevision);
                    partRevision.removeLastIteration();
                }

                filteredPartRevisions.add(partRevision);

            } catch (AccessRightException | PartRevisionNotFoundException e) {
                LOGGER.log(Level.FINER, null, e);
            }
        }
        return filteredPartRevisions;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public int getPartsInWorkspaceCount(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException, AccountNotFoundException {
        return partRevisionDAO.getTotalNumberOfParts(pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deletePartRevision(PartRevisionKey partRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, EntityConstraintException, AccessRightException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(partRevisionKey.getPartMaster().getWorkspace());


        PartRevision partR = partRevisionDAO.loadPartR(partRevisionKey);
        if (!hasPartOrWorkspaceWriteAccess(user, partR)) {
            throw new AccessRightException(user);
        }
        PartMaster partMaster = partR.getPartMaster();
        boolean isLastRevision = partMaster.getPartRevisions().size() == 1;

        //TODO all the 3 removal restrictions may be performed
        //more precisely on PartRevision rather on PartMaster
        // check if part is linked to a product
        if (configurationItemDAO.isPartMasterLinkedToConfigurationItem(partMaster)) {
            throw new EntityConstraintException("EntityConstraintException1");
        }
        // check if this part is in a partUsage
        if (partUsageLinkDAO.hasPartUsages(partMaster.getWorkspaceId(), partMaster.getNumber())) {
            throw new EntityConstraintException("EntityConstraintException2");
        }

        // check if this part is in a partSubstitute
        if (partUsageLinkDAO.hasPartSubstitutes(partMaster.getWorkspaceId(), partMaster.getNumber())) {
            throw new EntityConstraintException("EntityConstraintException22");
        }

        // check if part is baselined
        if (productBaselineDAO.existBaselinedPart(partMaster.getWorkspaceId(), partMaster.getNumber())) {
            throw new EntityConstraintException("EntityConstraintException5");
        }

        if (changeItemDAO.hasChangeItems(partRevisionKey)) {
            throw new EntityConstraintException("EntityConstraintException21");
        }

        partRevisionEvent.select(new AnnotationLiteral<Removed>() {
        }).fire(new PartRevisionEvent(partR));

        if (isLastRevision) {
            partMasterDAO.removePartM(partMaster);
        } else {
            partMaster.removeRevision(partR);
            partRevisionDAO.removeRevision(partR);
        }

        for (PartIteration partIteration : partR.getPartIterations()) {
            try {
                indexerManager.removePartIterationFromIndex(partIteration);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, null, e);
            }
            try {
                removeCADFile(partIteration);
                removeAttachedFiles(partIteration);
            } catch (PartIterationNotFoundException e) {
                LOGGER.log(Level.WARNING, null, e);
            }
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public int getNumberOfIteration(PartRevisionKey partRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(partRevisionKey.getPartMaster().getWorkspace());
        return partRevisionDAO.loadPartR(partRevisionKey).getLastIterationNumber();
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision createPartRevision(PartRevisionKey revisionKey, String pDescription, String pWorkflowModelId, Map<String, String> pACLUserEntries, Map<String, String> pACLUserGroupEntries, Map<String, Collection<String>> userRoleMapping, Map<String, Collection<String>> groupRoleMapping)
            throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, PartRevisionNotFoundException, NotAllowedException, FileAlreadyExistsException, CreationException, RoleNotFoundException, WorkflowModelNotFoundException, PartRevisionAlreadyExistsException, UserGroupNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(revisionKey.getPartMaster().getWorkspace());

        PartRevision originalPartR = partRevisionDAO.loadPartR(revisionKey);

        if (originalPartR.isCheckedOut()) {
            throw new NotAllowedException("NotAllowedException40");
        }

        if (originalPartR.getNumberOfIterations() == 0) {
            throw new NotAllowedException("NotAllowedException41");
        }

        PartRevision partR = originalPartR.getPartMaster().createNextRevision(user);

        PartIteration lastPartI = originalPartR.getLastIteration();
        PartIteration firstPartI = partR.createNextIteration(user);


        if (lastPartI != null) {
            String partNumber = partR.getPartNumber();
            for (BinaryResource sourceFile : lastPartI.getAttachedFiles()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                Date lastModified = sourceFile.getLastModified();
                String fullName = partR.getWorkspaceId() + "/parts/" + partNumber + "/" + partR.getVersion() + "/1/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                firstPartI.addAttachedFile(targetFile);
                try {
                    storageManager.copyData(sourceFile, targetFile);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }

            // Copy usage links
            // Create new p2p links

            List<PartUsageLink> newComponents = new LinkedList<>();
            List<PartUsageLink> oldComponents = lastPartI.getComponents();
            for (PartUsageLink usage : lastPartI.getComponents()) {
                PartUsageLink newUsage = usage.clone();
                //PartUsageLink is shared among PartIteration hence there is no cascade persist
                //so we need to persist them explicitly
                em.persist(newUsage);
                newComponents.add(newUsage);
            }
            firstPartI.setComponents(newComponents);
            //flush to ensure the new PartUsageLinks have their id generated
            em.flush();
            pathToPathLinkDAO.cloneAndUpgradePathToPathLinks(oldComponents, newComponents);

            // copy geometries
            for (Geometry sourceFile : lastPartI.getGeometries()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                int quality = sourceFile.getQuality();
                Date lastModified = sourceFile.getLastModified();
                String fullName = partR.getWorkspaceId() + "/parts/" + partNumber + "/" + partR.getVersion() + "/1/" + fileName;
                Geometry targetFile = new Geometry(quality, fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                firstPartI.addGeometry(targetFile);
                try {
                    storageManager.copyData(sourceFile, targetFile);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }

            BinaryResource nativeCADFile = lastPartI.getNativeCADFile();
            if (nativeCADFile != null) {
                String fileName = nativeCADFile.getName();
                long length = nativeCADFile.getContentLength();
                Date lastModified = nativeCADFile.getLastModified();
                String fullName = partR.getWorkspaceId() + "/parts/" + partNumber + "/" + partR.getVersion() + "/1/nativecad/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binaryResourceDAO.createBinaryResource(targetFile);
                firstPartI.setNativeCADFile(targetFile);
                try {
                    storageManager.copyData(nativeCADFile, targetFile);
                } catch (StorageException e) {
                    LOGGER.log(Level.INFO, null, e);
                }
            }


            Set<DocumentLink> links = new HashSet<>();
            for (DocumentLink link : lastPartI.getLinkedDocuments()) {
                DocumentLink newLink = link.clone();
                links.add(newLink);
            }
            firstPartI.setLinkedDocuments(links);

            List<InstanceAttribute> attrs = new ArrayList<>();
            for (InstanceAttribute attr : lastPartI.getInstanceAttributes()) {
                InstanceAttribute clonedAttribute = attr.clone();
                attrs.add(clonedAttribute);
            }
            firstPartI.setInstanceAttributes(attrs);

        }


        Collection<Task> runningTasks = null;
        if (pWorkflowModelId != null) {

            Map<Role, Collection<User>> roleUserMap = new HashMap<>();
            for (Map.Entry<String, Collection<String>> pair : userRoleMapping.entrySet()) {
                String roleName = pair.getKey();
                Collection<String> userLogins = pair.getValue();
                Role role = roleDAO.loadRole(new RoleKey(originalPartR.getWorkspaceId(), roleName));
                Set<User> users = new HashSet<>();
                roleUserMap.put(role, users);
                for (String login : userLogins) {
                    User u = userDAO.loadUser(new UserKey(originalPartR.getWorkspaceId(), login));
                    users.add(u);
                }
            }

            Map<Role, Collection<UserGroup>> roleGroupMap = new HashMap<>();
            for (Map.Entry<String, Collection<String>> pair : groupRoleMapping.entrySet()) {
                String roleName = pair.getKey();
                Collection<String> groupIds = pair.getValue();
                Role role = roleDAO.loadRole(new RoleKey(originalPartR.getWorkspaceId(), roleName));
                Set<UserGroup> groups = new HashSet<>();
                roleGroupMap.put(role, groups);
                for (String groupId : groupIds) {
                    UserGroup g = userGroupDAO.loadUserGroup(new UserGroupKey(originalPartR.getWorkspaceId(), groupId));
                    groups.add(g);
                }
            }

            WorkflowModel workflowModel = workflowModelDAO.loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
            Workflow workflow = workflowModel.createWorkflow(roleUserMap, roleGroupMap);
            partR.setWorkflow(workflow);

            for (Task task : workflow.getTasks()) {
                if (!task.hasPotentialWorker()) {
                    throw new NotAllowedException("NotAllowedException56");
                }
            }

            runningTasks = workflow.getRunningTasks();
            runningTasks.forEach(Task::start);
        }

        partR.setDescription(pDescription);

        if (pACLUserEntries != null && !pACLUserEntries.isEmpty() || pACLUserGroupEntries != null && !pACLUserGroupEntries.isEmpty()) {
            ACL acl = aclFactory.createACL(user.getWorkspace().getId(), pACLUserEntries, pACLUserGroupEntries);
            partR.setACL(acl);
        }

        Date now = new Date();
        partR.setCreationDate(now);
        partR.setCheckOutUser(user);
        partR.setCheckOutDate(now);
        firstPartI.setCreationDate(now);
        firstPartI.setModificationDate(now);

        partRevisionDAO.createPartR(partR);

        if (runningTasks != null) {
            mailer.sendApproval(partR.getWorkspaceId(), runningTasks, partR);
        }

        return partR;

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public String generateId(String pWorkspaceId, String pPartMTemplateId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, PartMasterTemplateNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        PartMasterTemplateKey partMasterTemplateKey = new PartMasterTemplateKey(user.getWorkspaceId(), pPartMTemplateId);
        PartMasterTemplate template = partMasterTemplateDAO.loadPartMTemplate(partMasterTemplateKey);

        String newId = null;
        try {
            String latestId = partMasterDAO.findLatestPartMId(pWorkspaceId, template.getPartType());
            String inputMask = template.getMask();
            String convertedMask = Tools.convertMask(inputMask);
            newId = Tools.increaseId(latestId, convertedMask);
        } catch (ParseException ex) {
            LOGGER.log(Level.WARNING, "Different mask has been used for the same document type", ex);
        } catch (NoResultException ex) {
            LOGGER.log(Level.FINE, "No document of the specified type has been created", ex);
        }
        return newId;

    }


    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public long getDiskUsageForPartsInWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException {
        userManager.checkAdmin(pWorkspaceId);
        return partMasterDAO.getDiskUsageForPartsInWorkspace(pWorkspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public long getDiskUsageForPartTemplatesInWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException {
        userManager.checkAdmin(pWorkspaceId);
        return partMasterDAO.getDiskUsageForPartTemplatesInWorkspace(pWorkspaceId);
    }

    @Override
    public PartRevision[] getCheckedOutPartRevisions(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<PartRevision> partRevisions = partRevisionDAO.findCheckedOutPartRevisionsForUser(pWorkspaceId, user.getLogin());
        return partRevisions.toArray(new PartRevision[partRevisions.size()]);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public PartRevision[] getAllCheckedOutPartRevisions(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException {
        userManager.checkAdmin(pWorkspaceId);
        List<PartRevision> partRevisions = partRevisionDAO.findAllCheckedOutPartRevisions(pWorkspaceId);
        return partRevisions.toArray(new PartRevision[partRevisions.size()]);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public SharedPart createSharedPart(PartRevisionKey pPartRevisionKey, String pPassword, Date pExpireDate) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, PartRevisionNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(pPartRevisionKey.getPartMaster().getWorkspace());
        SharedPart sharedPart = new SharedPart(user.getWorkspace(), user, pExpireDate, pPassword, getPartRevision(pPartRevisionKey));
        sharedEntityDAO.createSharedPart(sharedPart);
        return sharedPart;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void deleteSharedPart(SharedEntityKey pSharedEntityKey) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, SharedEntityNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceWriteAccess(pSharedEntityKey.getWorkspace());
        SharedPart sharedPart = sharedEntityDAO.loadSharedPart(pSharedEntityKey.getUuid());
        sharedEntityDAO.deleteSharedPart(sharedPart);
    }


    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean canAccess(PartRevisionKey partRKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(partRKey.getPartMaster().getWorkspace());
        return canUserAccess(user, partRKey);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean canAccess(PartIterationKey partIKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, PartIterationNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(partIKey.getWorkspaceId());
        return canUserAccess(user, partIKey);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean canUserAccess(User user, PartRevisionKey partRKey) throws PartRevisionNotFoundException {
        PartRevision partRevision = partRevisionDAO.loadPartR(partRKey);
        return hasPartRevisionReadAccess(user, partRevision);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean canUserAccess(User user, PartIterationKey partIKey) throws PartRevisionNotFoundException, PartIterationNotFoundException {
        PartRevision partR = partRevisionDAO.loadPartR(partIKey.getPartRevision());
        return hasPartRevisionReadAccess(user, partR) &&
                (!partRevisionDAO.isCheckedOutIteration(partIKey) ||
                        user.equals(partR.getCheckOutUser()));
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public User checkPartRevisionReadAccess(PartRevisionKey partRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(partRevisionKey.getPartMaster().getWorkspace());
        if (!canUserAccess(user, partRevisionKey)) {
            throw new AccessRightException(user);
        }
        return user;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean canWrite(PartRevisionKey partRKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {

        String workspace = partRKey.getPartMaster().getWorkspace();

        User user = userManager.checkWorkspaceReadAccess(workspace);

        if (user.isAdministrator()) {
            return true;
        }

        PartRevision partRevision;

        try {
            partRevision = getPartRevision(partRKey);
        } catch (AccessRightException e) {
            return false;
        }

        if (partRevision.getACL() != null) {
            return partRevision.getACL().hasWriteAccess(user);
        }

        try {
            userManager.checkWorkspaceWriteAccess(workspace);
            return true;
        } catch (AccessRightException e) {
            return false;
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Component filterProductStructure(ConfigurationItemKey ciKey, ProductStructureFilter filter, List<PartLink> path, Integer pDepth) throws ConfigurationItemNotFoundException, WorkspaceNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, PartUsageLinkNotFoundException, AccessRightException, PartMasterNotFoundException, EntityConstraintException, WorkspaceNotEnabledException {
        String workspaceId = ciKey.getWorkspace();
        userManager.checkWorkspaceReadAccess(workspaceId);

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {
        };

        if (path == null) {
            ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(ciKey);
            return psFilterVisitor.visit(workspaceId, filter, ci.getDesignItem(), pDepth, callbacks);
        } else {
            return psFilterVisitor.visit(workspaceId, filter, path, pDepth, callbacks);
        }

    }


    @Override
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    public Set<PartRevision> getWritablePartRevisionsFromPath(ConfigurationItemKey configurationItemKey, String path) throws EntityConstraintException, PartMasterNotFoundException, NotAllowedException, UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, ConfigurationItemNotFoundException, PartUsageLinkNotFoundException, WorkspaceNotEnabledException {
        String workspaceId = configurationItemKey.getWorkspace();
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Set<PartRevision> partRevisions = new HashSet<>();

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {
            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                PartMaster pm = parts.get(parts.size() - 1);
                try {
                    if (!hasPartRevisionReadAccess(user, pm.getLastRevision())) {
                        //Don't visit this branch anymore
                        return false;
                    }
                    if (!hasPartOrWorkspaceWriteAccess(user, pm.getLastRevision())) {
                        return true;
                    }
                    partRevisions.add(pm.getLastRevision());

                } catch (WorkspaceNotFoundException | WorkspaceNotEnabledException e) {
                    LOGGER.log(Level.SEVERE, "Could not check access to part revision", e);
                    return false;
                }
                return true;
            }
        };

        psFilterVisitor.visit(workspaceId, new WIPPSFilter(user), decodePath(configurationItemKey, path), null, callbacks);
        return partRevisions;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Component filterProductStructureOnLinkType(ConfigurationItemKey ciKey, ProductStructureFilter filter, String configSpecType, String path, String linkType)
            throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, PartUsageLinkNotFoundException, ProductInstanceMasterNotFoundException, BaselineNotFoundException, WorkspaceNotEnabledException, NotAllowedException, EntityConstraintException, PartMasterNotFoundException {

        String workspaceId = ciKey.getWorkspace();
        User user = userManager.checkWorkspaceReadAccess(workspaceId);


        // 1 Get flat list of paths in the structure for filtering path to path links
        List<String> discoveredPaths = new ArrayList<>();

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {
            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                discoveredPaths.add(Tools.getPathAsString(path));
                return true;
            }
        };

        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(ciKey);
        psFilterVisitor.visit(workspaceId, filter, ci.getDesignItem(), -1, callbacks);

        // 2 find all path to path source and target links in configuration item and retain only discovered paths
        Component component;

        Set<String> links = new HashSet<>();


        // If path is null => get Root links embedded as subComponents of a virtual component
        if (path == null) {

            List<PathToPathLink> rootPathToPathLinks = getRootPathToPathLinks(configSpecType, ci, linkType);

            links.addAll(rootPathToPathLinks
                    .stream()
                    .map(PathToPathLink::getSourcePath)
                    .collect(Collectors.toList()));

            component = createVirtualComponent(user, ci, linkType);

        } else { // If path is not null => get next path to path links
            List<PathToPathLink> sourcesPathToPathLinksInProduct = getSourcesPathToPathLinksInProduct(configSpecType, ci, path, linkType);

            links.addAll(sourcesPathToPathLinksInProduct.stream()
                    .map(PathToPathLink::getTargetPath)
                    .collect(Collectors.toList()));

            List<PartLink> decodedSourcePath = decodePath(ciKey, path);
            PartLink link = decodedSourcePath.get(decodedSourcePath.size() - 1);
            List<PartIteration> partIterations = filter.filter(link.getComponent());
            PartIteration retainedIteration = partIterations.get(partIterations.size() - 1);

            component = new Component();
            component.setUser(user);
            component.setComponents(new ArrayList<>());

            component.setPath(decodedSourcePath);
            component.setPartMaster(link.getComponent());
            component.setRetainedIteration(retainedIteration);

        }

        // 3. Retain only discovered paths
        links.retainAll(discoveredPaths);

        for (String link : links) {
            Component subComponent = new Component();

            List<PartLink> decodedPath = decodePath(ciKey, link);
            subComponent.setPath(decodedPath);

            PartLink partLink = decodedPath.get(decodedPath.size() - 1);
            subComponent.setPartMaster(partLink.getComponent());

            List<PartIteration> partIterations = filter.filter(partLink.getComponent());

            if (partIterations.size() > 0) {
                PartIteration retainedIteration = partIterations.get(partIterations.size() - 1);
                subComponent.setRetainedIteration(retainedIteration);
            }

            subComponent.setUser(user);
            subComponent.setComponents(new ArrayList<>());
            component.addComponent(subComponent);
        }

        return component;
    }

    private List<PathToPathLink> getRootPathToPathLinks(String configSpecType, ConfigurationItem ci, String linkType)
            throws ProductInstanceMasterNotFoundException, BaselineNotFoundException {
        List<PathToPathLink> rootPathToPathLinks;
        if (configSpecType.startsWith("pi-")) {
            String serialNumber = configSpecType.substring(3);
            ProductInstanceMasterKey productInstanceMasterKey = new ProductInstanceMasterKey(serialNumber, ci.getWorkspace().getId(), ci.getId());
            ProductInstanceIteration pii = productInstanceMasterDAO.loadProductInstanceMaster(productInstanceMasterKey).getLastIteration();
            rootPathToPathLinks = pathToPathLinkDAO.findRootPathToPathLinks(pii, linkType);

        } else if (!"wip".equals(configSpecType) && !"latest".equals(configSpecType) && !"released".equals(configSpecType)) {
            int baselineId = 0;
            try {
                baselineId = Integer.parseInt(configSpecType);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
            ProductBaseline pb = productBaselineDAO.loadBaseline(baselineId);
            rootPathToPathLinks = pathToPathLinkDAO.findRootPathToPathLinks(pb, linkType);

        } else {
            rootPathToPathLinks = pathToPathLinkDAO.findRootPathToPathLinks(ci, linkType);
        }
        return rootPathToPathLinks;
    }

    private Component createVirtualComponent(User user, ConfigurationItem ci, String linkType) {
        Component component = new Component();
        component.setUser(user);
        component.setComponents(new ArrayList<>());
        PartMaster virtualPartMaster = new PartMaster(ci.getWorkspace(), linkType, user);
        PartRevision virtualPartRevision = new PartRevision(virtualPartMaster, user);
        virtualPartMaster.getPartRevisions().add(virtualPartRevision);
        PartIteration virtualPartIteration = new PartIteration(virtualPartRevision, user);
        virtualPartRevision.getPartIterations().add(virtualPartIteration);

        component.setPartMaster(virtualPartMaster);
        component.setRetainedIteration(virtualPartIteration);
        component.setPath(new ArrayList<>());
        component.setVirtual(true);

        component.getPath().add(new PartLink() {
            @Override
            public int getId() {
                return 0;
            }

            @Override
            public Character getCode() {
                return null;
            }

            @Override
            public String getFullId() {
                return null;
            }

            @Override
            public double getAmount() {
                return 1;
            }

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public String getComment() {
                return null;
            }

            @Override
            public boolean isOptional() {
                return false;
            }

            @Override
            public PartMaster getComponent() {
                return virtualPartMaster;
            }

            @Override
            public List<PartSubstituteLink> getSubstitutes() {
                return null;
            }

            @Override
            public String getReferenceDescription() {
                return linkType;
            }

            @Override
            public List<CADInstance> getCadInstances() {
                return null;
            }
        });
        return component;
    }

    private List<PathToPathLink> getSourcesPathToPathLinksInProduct(String configSpecType, ConfigurationItem ci, String path, String linkType)
            throws ProductInstanceMasterNotFoundException, BaselineNotFoundException {
        List<PathToPathLink> sourcesPathToPathLinksInProduct;

        if ("wip".equals(configSpecType) || "latest".equals(configSpecType) || "released".equals(configSpecType)) {
            sourcesPathToPathLinksInProduct = pathToPathLinkDAO.getSourcesPathToPathLinksInProduct(ci, linkType, path);
        } else {
            ProductBaseline productBaseline;

            if (configSpecType.startsWith("pi-")) {
                String serialNumber = configSpecType.substring(3);
                productBaseline = productBaselineDAO.findLastBaselineWithSerialNumber(ci.getKey(), serialNumber);

            } else {
                int baselineId = 0;
                try {
                    baselineId = Integer.parseInt(configSpecType);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.FINEST, null, e);
                }
                productBaseline = productBaselineDAO.loadBaseline(baselineId);
            }

            sourcesPathToPathLinksInProduct = pathToPathLinkDAO.getSourcesPathToPathLinksInBaseline(productBaseline, linkType, path);

        }
        return sourcesPathToPathLinksInProduct;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartLink> decodePath(ConfigurationItemKey ciKey, String path) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartUsageLinkNotFoundException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(ciKey.getWorkspace());

        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        List<PartLink> decodedPath = new ArrayList<>();

        PartLink rootPartUsageLink = getRootPartUsageLink(ciKey);
        decodedPath.add(rootPartUsageLink);

        if ("-1".equals(path)) {
            return decodedPath;
        }

        // Remove the -1- in front of string
        String[] split = path.substring(3).split("-");

        for (String codeAndId : split) {

            int id = Integer.valueOf(codeAndId.substring(1));

            if (codeAndId.startsWith("u")) {
                decodedPath.add(getPartUsageLink(id));
            } else if (codeAndId.startsWith("s")) {
                decodedPath.add(getPartSubstituteLink(id));
            } else {
                throw new IllegalArgumentException("Missing code");
            }

        }

        return decodedPath;
    }


    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<PartRevision> searchPartRevisions(String workspaceId, Query query) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);

        Workspace workspace = workspaceDAO.loadWorkspace(workspaceId);

        List<PartRevision> parts = partRevisionQueryDAO.runQuery(user.getTimeZone(), workspace, query);

        ListIterator<PartRevision> ite = parts.listIterator();

        while (ite.hasNext()) {
            PartRevision partR = ite.next();

            if (isCheckoutByAnotherUser(user, partR)) {
                em.detach(partR);
                partR.removeLastIteration();
            }

            if (partR.getLastIteration() != null && !hasPartRevisionReadAccess(user, partR)) {
                ite.remove();
            }
        }

        return parts;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Query getQuery(String workspaceId, int queryId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);

        Query query = queryDAO.loadQuery(queryId);

        if (!query.getAuthor().getWorkspace().getId().equals(workspaceId)) {
            userManager.checkWorkspaceReadAccess(query.getAuthor().getWorkspace().getId());
        }

        return query;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void createQuery(String workspaceId, Query query) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, QueryAlreadyExistsException, CreationException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(workspaceId);

        Query existingQuery = queryDAO.findQueryByName(workspaceId, query.getName());
        if (existingQuery != null) {
            deleteQuery(workspaceId, existingQuery.getId());
        }

        query.setAuthor(user);
        query.setCreationDate(new Date());
        queryDAO.createQuery(query);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteQuery(String workspaceId, int queryId) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceWriteAccess(workspaceId);
        Query query = queryDAO.loadQuery(queryId);

        Workspace workspace = query.getAuthor().getWorkspace();
        User userInQueryWorkspace = userManager.checkWorkspaceWriteAccess(workspace.getId());

        if (query.getAuthor().equals(userInQueryWorkspace) || userInQueryWorkspace.isAdministrator()) {
            queryDAO.removeQuery(query);
        } else {
            throw new AccessRightException(userInQueryWorkspace);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<Query> getQueries(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return queryDAO.loadQueries(workspaceId);
    }

    private PartUsageLink getPartUsageLink(int id) throws PartUsageLinkNotFoundException {
        return partUsageLinkDAO.loadPartUsageLink(id);
    }

    private PartSubstituteLink getPartSubstituteLink(int id) throws PartUsageLinkNotFoundException {
        return partUsageLinkDAO.loadPartSubstituteLink(id);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartLink getRootPartUsageLink(ConfigurationItemKey pKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pKey.getWorkspace());
        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(pKey);

        return new PartLink() {
            @Override
            public int getId() {
                return 1;
            }

            @Override
            public Character getCode() {
                return '-';
            }

            @Override
            public String getFullId() {
                return "-1";
            }

            @Override
            public double getAmount() {
                return 1;
            }

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public String getComment() {
                return null;
            }

            @Override
            public boolean isOptional() {
                return false;
            }

            @Override
            public PartMaster getComponent() {
                return ci.getDesignItem();
            }

            @Override
            public List<PartSubstituteLink> getSubstitutes() {
                return null;
            }

            @Override
            public String getReferenceDescription() {
                return null;
            }

            @Override
            public List<CADInstance> getCadInstances() {
                List<CADInstance> cads = new ArrayList<>();
                CADInstance cad = new CADInstance(0d, 0d, 0d, 0d, 0d, 0d);
                cad.setId(0);
                cads.add(cad);
                return cads;
            }
        };

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public void checkCyclicAssemblyForPartIteration(PartIteration partIteration) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, EntityConstraintException, PartMasterNotFoundException, WorkspaceNotEnabledException {

        PartMaster partMaster = partIteration.getPartRevision().getPartMaster();
        Workspace workspace = partMaster.getWorkspace();
        String workspaceId = workspace.getId();
        User user = userManager.checkWorkspaceReadAccess(workspaceId);

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {
        };
        psFilterVisitor.visit(workspaceId, new UpdatePartIterationPSFilter(partIteration), partMaster, -1, callbacks);

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ProductStructureFilter getLatestCheckedInPSFilter(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return new LatestCheckedInPSFilter(false);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean hasModificationNotification(ConfigurationItemKey ciKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, NotAllowedException, EntityConstraintException, PartMasterNotFoundException, WorkspaceNotEnabledException {
        String workspaceId = ciKey.getWorkspace();
        userManager.checkWorkspaceReadAccess(workspaceId);
        ConfigurationItem configurationItem = configurationItemDAO.loadConfigurationItem(ciKey);

        List<String> visitedPartNumbers = new ArrayList<>();
        LatestCheckedInPSFilter latestCheckedInPSFilter = new LatestCheckedInPSFilter(true);
        final boolean[] hasModificationNotification = {false};

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {
            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                PartMaster partMaster = parts.get(parts.size() - 1);
                if (!visitedPartNumbers.contains(partMaster.getNumber()) && !hasModificationNotification[0]) {
                    visitedPartNumbers.add(partMaster.getNumber());
                    List<PartIteration> partIterations = latestCheckedInPSFilter.filter(partMaster);
                    // As we use a latest checked-in filter, partIterations array can be empty
                    if (!partIterations.isEmpty()) {
                        PartIteration partIteration = partIterations.get(partIterations.size() - 1);
                        if (modificationNotificationDAO.hasModificationNotifications(partIteration.getKey())) {
                            hasModificationNotification[0] = true;
                        }
                    }
                }
                return true;
            }
        };

        psFilterVisitor.visit(workspaceId, latestCheckedInPSFilter, configurationItem.getDesignItem(), -1, callbacks);

        return hasModificationNotification[0];
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<InstanceAttribute> getPartIterationsInstanceAttributesInWorkspace(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return instanceAttributeDAO.getPartIterationsInstanceAttributesInWorkspace(workspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<InstanceAttribute> getPathDataInstanceAttributesInWorkspace(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return instanceAttributeDAO.getPathDataInstanceAttributesInWorkspace(workspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<QueryResultRow> filterProductBreakdownStructure(String workspaceId, Query query) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, BaselineNotFoundException, ProductInstanceMasterNotFoundException, ConfigurationItemNotFoundException, NotAllowedException, PartMasterNotFoundException, EntityConstraintException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        List<QueryResultRow> rows = new ArrayList<>();
        for (QueryContext queryContext : query.getContexts()) {
            rows.addAll(filterPBS(query, workspaceId, queryContext, user));
        }
        return rows;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<BinaryResource> getBinaryResourceFromBaseline(int baselineId) {
        List<BinaryResource> binaryResources = new ArrayList<>();

        for (Map.Entry<BaselinedDocumentKey, BaselinedDocument> doc : productBaselineDAO.findBaselineById(baselineId).getBaselinedDocuments().entrySet()) {
            doc.getValue().getTargetDocument().getAttachedFiles()
                    .stream()
                    .filter(binary -> !binaryResources.contains(binary))
                    .forEach(binaryResources::add);
        }
        return binaryResources;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Map<String, Set<BinaryResource>> getBinariesInTree(Integer baselineId, String workspaceId, ConfigurationItemKey ciKey, ProductStructureFilter psFilter, boolean exportNativeCADFiles, boolean exportDocumentLinks) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, NotAllowedException, EntityConstraintException, PartMasterNotFoundException, WorkspaceNotEnabledException {

        userManager.checkWorkspaceReadAccess(workspaceId);
        Map<String, Set<BinaryResource>> result = new HashMap<>();

        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(ciKey);
        PartMaster root = ci.getDesignItem();

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {

            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                PartMaster part = parts.get(parts.size() - 1);
                List<PartIteration> partIterations = psFilter.filter(part);

                if (!partIterations.isEmpty()) {

                    PartIteration partIteration = partIterations.get(0);
                    String partFolderName = partIteration.toString();
                    Set<BinaryResource> binaryResources = result.get(partFolderName);

                    if (binaryResources == null) {
                        binaryResources = new HashSet<>();
                        result.put(partFolderName, binaryResources);
                    }

                    if (exportNativeCADFiles) {
                        BinaryResource nativeCADFile = partIteration.getNativeCADFile();
                        if (nativeCADFile != null) {
                            binaryResources.add(nativeCADFile);
                        }

                        if (exportDocumentLinks) {
                            for (BinaryResource attachedFile : partIteration.getAttachedFiles()) {
                                if (attachedFile != null) {
                                    binaryResources.add(attachedFile);
                                }
                            }
                        }
                    }

                    if (exportDocumentLinks && baselineId == null) {
                        Set<DocumentLink> linkedDocuments = partIteration.getLinkedDocuments();

                        for (DocumentLink documentLink : linkedDocuments) {

                            DocumentIteration lastCheckedInIteration = documentLink.getTargetDocument().getLastCheckedInIteration();

                            if (null != lastCheckedInIteration) {

                                String linkedDocumentFolderName = "links/" + lastCheckedInIteration.toString();

                                Set<BinaryResource> linkedBinaryResources = result.get(linkedDocumentFolderName);

                                if (linkedBinaryResources == null) {
                                    linkedBinaryResources = new HashSet<>();
                                    result.put(linkedDocumentFolderName, linkedBinaryResources);
                                }

                                Set<BinaryResource> attachedFiles = lastCheckedInIteration.getAttachedFiles();

                                for (BinaryResource binary : attachedFiles) {
                                    if (!linkedBinaryResources.contains(binary)) {
                                        linkedBinaryResources.add(binary);
                                    }
                                }

                            }
                        }
                    }

                }
                return true;
            }
        };

        psFilterVisitor.visit(workspaceId, psFilter, root, -1, callbacks);

        return result;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ProductBaseline loadProductBaselineForProductInstanceMaster(ConfigurationItemKey ciKey, String serialNumber) throws ProductInstanceMasterNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(ciKey.getWorkspace());
        return productBaselineDAO.findLastBaselineWithSerialNumber(ciKey, serialNumber);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Query loadQuery(String workspaceId, int queryId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return queryDAO.loadQuery(queryId);
    }

    private List<QueryResultRow> filterPBS(Query query, String workspaceId, QueryContext queryContext, User user) throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, BaselineNotFoundException, ProductInstanceMasterNotFoundException, ConfigurationItemNotFoundException, NotAllowedException, EntityConstraintException, PartMasterNotFoundException, WorkspaceNotEnabledException {

        String configurationItemId = queryContext.getConfigurationItemId();
        String serialNumber = queryContext.getSerialNumber();

        ConfigurationItemKey ciKey = new ConfigurationItemKey(workspaceId, configurationItemId);


        List<QueryResultRow> rows = new ArrayList<>();

        ProductStructureFilter filter = serialNumber != null ? psFilterManager.getPSFilter(ciKey, "pi-" + serialNumber, false) : psFilterManager.getPSFilter(ciKey, "latest", false);

        ProductInstanceIteration productInstanceIteration = null;
        if (serialNumber != null) {
            ProductInstanceMaster productIM = productInstanceMasterDAO.loadProductInstanceMaster(new ProductInstanceMasterKey(serialNumber, ciKey));
            productInstanceIteration = productIM.getLastIteration();
        }

        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(ciKey);
        PartMaster root = ci.getDesignItem();

        List<PathToPathLink> pathToPathLinks = ci.getPathToPathLinks();

        List<PathDataIteration> lastPathDataIterations = pathDataIterationDAO.getLastPathDataIterations(productInstanceIteration);
        Map<String, PathDataIteration> lastPathDataIterationsMap = new HashMap<>();

        for (PathDataIteration iteration : lastPathDataIterations) {
            lastPathDataIterationsMap.put(iteration.getPathDataMaster().getPath(), iteration);
        }

        final ProductInstanceIteration finalProductInstanceIteration = productInstanceIteration;

        List<String> filteredPathsFromQuery = null;
        QueryRule pathDataQueryRule = query.getPathDataQueryRule();

        boolean shouldFilterPathDataWithCriteriaBuilder = finalProductInstanceIteration != null && pathDataQueryRule != null;

        if (shouldFilterPathDataWithCriteriaBuilder) {
            filteredPathsFromQuery = pathDataQueryDAO.runQuery(user.getTimeZone(), finalProductInstanceIteration, query);
        }

        final List<String> finalFilteredPathsFromQuery = filteredPathsFromQuery;

        PSFilterVisitorCallbacks callbacks = new PSFilterVisitorCallbacks() {

            @Override
            public boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
                QueryResultRow row = new QueryResultRow();
                double totalAmount = 1;
                for (PartLink pl : path) {
                    if (pl.getUnit() == null) {
                        totalAmount *= pl.getAmount();
                    }
                }
                String pathAsString = Tools.getPathAsString(path);
                row.setPath(pathAsString);
                int depth = parts.size() - 1;
                PartMaster part = parts.get(parts.size() - 1);
                List<PartIteration> partIterations = filter.filter(part);
                if (!partIterations.isEmpty()) {
                    PartRevision partRevision = partIterations.get(0).getPartRevision();
                    row.setPartRevision(partRevision);
                    row.setDepth(depth);
                    row.setContext(queryContext);
                    row.setAmount(totalAmount);

                    // try block and decodePath method are time consuming (db access) May need refactor
                    for (PathToPathLink pathToPathLink : pathToPathLinks) {
                        try {
                            if (pathToPathLink.getSourcePath().equals(pathAsString)) {
                                row.addSource(pathToPathLink.getType(), decodePath(ciKey, pathToPathLink.getTargetPath()));
                            }
                            if (pathToPathLink.getTargetPath().equals(pathAsString)) {
                                row.addTarget(pathToPathLink.getType(), decodePath(ciKey, pathToPathLink.getTargetPath()));
                            }
                        } catch (WorkspaceNotFoundException | WorkspaceNotEnabledException | UserNotFoundException | ConfigurationItemNotFoundException | PartUsageLinkNotFoundException | UserNotActiveException e) {
                            LOGGER.log(Level.SEVERE, null, e);
                        }
                    }

                    if (finalProductInstanceIteration != null) {
                        row.setPathDataIteration(lastPathDataIterationsMap.get(pathAsString));
                    }

                    if (shouldFilterPathDataWithCriteriaBuilder) {
                        if (finalFilteredPathsFromQuery.contains(pathAsString)) {
                            rows.add(row);
                        }
                    } else {
                        rows.add(row);
                    }
                }
                return true;
            }
        };

        psFilterVisitor.visit(workspaceId, filter, root, -1, callbacks);

        return rows;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<PartIteration> getInversePartsLink(DocumentRevisionKey docKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, PartIterationNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(docKey.getWorkspaceId());

        DocumentRevision documentRevision = documentRevisionDAO.loadDocR(docKey);

        List<PartIteration> iterations = documentLinkDAO.getInversePartsLinks(documentRevision);
        ListIterator<PartIteration> ite = iterations.listIterator();

        while (ite.hasNext()) {
            PartIteration next = ite.next();
            if (!canAccess(next.getKey())) {
                ite.remove();
            }
        }
        return iterations;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public Set<ProductInstanceMaster> getInverseProductInstancesLink(DocumentRevisionKey docKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, PartIterationNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(docKey.getWorkspaceId());

        DocumentRevision documentRevision = documentRevisionDAO.loadDocR(docKey);

        List<ProductInstanceIteration> iterations = documentLinkDAO.getInverseProductInstanceIteration(documentRevision);
        Set<ProductInstanceMaster> productInstanceMasterSet = new HashSet<>();
        for (ProductInstanceIteration productInstanceIteration : iterations) {
            productInstanceMasterSet.add(productInstanceIteration.getProductInstanceMaster());

        }
        return productInstanceMasterSet;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public Set<PathDataMaster> getInversePathDataLink(DocumentRevisionKey docKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, PartIterationNotFoundException, PartRevisionNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(docKey.getWorkspaceId());

        DocumentRevision documentRevision = documentRevisionDAO.loadDocR(docKey);

        List<PathDataIteration> pathDataIterations = documentLinkDAO.getInversefindPathData(documentRevision);
        return pathDataIterations.stream()
                .map(PathDataIteration::getPathDataMaster).collect(Collectors.toSet());
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public PathToPathLink createPathToPathLink(String workspaceId, String configurationItemId, String type, String pathFrom, String pathTo, String description) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, PathToPathLinkAlreadyExistsException, CreationException, PathToPathCyclicException, PartUsageLinkNotFoundException, UserNotActiveException, NotAllowedException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(workspaceId);


        // Load the product
        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(new ConfigurationItemKey(workspaceId, configurationItemId));

        if (type == null || type.isEmpty()) {
            throw new NotAllowedException("NotAllowedException54");
        }

        if (pathFrom != null && pathTo != null && pathFrom.equals(pathTo)) {
            throw new NotAllowedException("NotAllowedException57");
        }

        // Decode the paths to insure path validity
        List<PartLink> sourcePath = decodePath(ci.getKey(), pathFrom);
        List<PartLink> targetPath = decodePath(ci.getKey(), pathTo);

        // Check for substitute linking
        PartLink sourceLink = sourcePath.get(sourcePath.size() - 1);
        PartLink targetLink = targetPath.get(targetPath.size() - 1);

        if (sourceLink.getSubstitutes() != null && sourceLink.getSubstitutes().contains(targetLink) || targetLink.getSubstitutes() != null && targetLink.getSubstitutes().contains(sourceLink)) {
            throw new NotAllowedException("NotAllowedException58");
        }

        PathToPathLink pathToPathLink = new PathToPathLink(type, pathFrom, pathTo, description);
        PathToPathLink samePathToPathLink = pathToPathLinkDAO.getSamePathToPathLink(ci, pathToPathLink);

        if (samePathToPathLink != null) {
            throw new PathToPathLinkAlreadyExistsException(pathToPathLink);
        }

        pathToPathLinkDAO.createPathToPathLink(pathToPathLink);

        ci.addPathToPathLink(pathToPathLink);

        checkCyclicPathToPathLink(ci, pathToPathLink, new ArrayList<>());

        return pathToPathLink;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public PathToPathLink updatePathToPathLink(String workspaceId, String configurationItemId, int pathToPathLinkId, String description)
            throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, PartUsageLinkNotFoundException, UserNotActiveException, NotAllowedException, PathToPathLinkNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceWriteAccess(workspaceId);

        // Load the product
        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(new ConfigurationItemKey(workspaceId, configurationItemId));
        PathToPathLink pathToPathLink = pathToPathLinkDAO.loadPathToPathLink(pathToPathLinkId);

        if (!ci.getPathToPathLinks().contains(pathToPathLink)) {
            throw new NotAllowedException("NotAllowedException60");
        }

        pathToPathLink.setDescription(description);

        return pathToPathLink;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<String> getPathToPathLinkTypes(String workspaceId, String configurationItemId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        ConfigurationItem configurationItem = configurationItemDAO.loadConfigurationItem(new ConfigurationItemKey(workspaceId, configurationItemId));
        return pathToPathLinkDAO.getDistinctPathToPathLinkTypes(configurationItem);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<PathToPathLink> getPathToPathLinkFromSourceAndTarget(String workspaceId, String configurationItemId, String sourcePath, String targetPath) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        ConfigurationItem configurationItem = configurationItemDAO.loadConfigurationItem(new ConfigurationItemKey(workspaceId, configurationItemId));
        return pathToPathLinkDAO.getPathToPathLinkFromSourceAndTarget(configurationItem, sourcePath, targetPath);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public ProductInstanceMaster findProductByPathMaster(String workspaceId, PathDataMaster pathDataMaster) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);
        return pathDataMasterDAO.findByPathData(pathDataMaster);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public PartMaster getPartMasterFromPath(String workspaceId, String configurationItemId, String partPath) throws ConfigurationItemNotFoundException, UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, PartUsageLinkNotFoundException, WorkspaceNotEnabledException {
        ConfigurationItem configurationItem = configurationItemDAO.loadConfigurationItem(new ConfigurationItemKey(workspaceId, configurationItemId));
        List<PartLink> path = decodePath(configurationItem.getKey(), partPath);
        return path.get(path.size() - 1).getComponent();
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void deletePathToPathLink(String workspaceId, String configurationItemId, int pathToPathLinkId) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, ConfigurationItemNotFoundException, PathToPathLinkNotFoundException, WorkspaceNotEnabledException {

        userManager.checkWorkspaceWriteAccess(workspaceId);

        // Load the product
        ConfigurationItem ci = configurationItemDAO.loadConfigurationItem(new ConfigurationItemKey(workspaceId, configurationItemId));

        PathToPathLink pathToPathLink = pathToPathLinkDAO.loadPathToPathLink(pathToPathLinkId);
        ci.removePathToPathLink(pathToPathLink);
        pathToPathLinkDAO.removePathToPathLink(pathToPathLink);

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public List<ResolvedDocumentLink> getDocumentLinksAsDocumentIterations(String workspaceId, String configurationItemId, String configSpec, PartIterationKey partIterationKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, BaselineNotFoundException, PartIterationNotFoundException, ProductInstanceMasterNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(workspaceId);

        PartIteration partIteration = partIterationDAO.loadPartI(partIterationKey);

        if (null == configSpec) {
            throw new IllegalArgumentException("Config spec cannot be null");
        }

        ProductBaseline baseline;

        if (configSpec.startsWith("pi-")) {
            String serialNumber = configSpec.substring(3);
            ProductInstanceMaster productInstanceMaster = productInstanceMasterDAO.loadProductInstanceMaster(new ProductInstanceMasterKey(serialNumber, workspaceId, configurationItemId));
            baseline = productInstanceMaster.getLastIteration().getBasedOn();
        } else {
            int baselineId = Integer.parseInt(configSpec);
            baseline = productBaselineDAO.loadBaseline(baselineId);
        }

        List<ResolvedDocumentLink> resolvedDocumentLinks = new ArrayList<>();

        if (baseline != null) {
            DocumentCollection documentCollection = baseline.getDocumentCollection();
            Map<BaselinedDocumentKey, BaselinedDocument> baselinedDocuments = documentCollection.getBaselinedDocuments();

            for (Map.Entry<BaselinedDocumentKey, BaselinedDocument> map : baselinedDocuments.entrySet()) {
                BaselinedDocument baselinedDocument = map.getValue();
                DocumentIteration targetDocument = baselinedDocument.getTargetDocument();
                resolvedDocumentLinks.addAll(partIteration.getLinkedDocuments()
                        .stream()
                        .filter(documentLink -> documentLink.getTargetDocument().getKey().equals(targetDocument.getDocumentRevisionKey()))
                        .map(documentLink -> new ResolvedDocumentLink(documentLink, targetDocument))
                        .collect(Collectors.toList()));
            }
        }

        return resolvedDocumentLinks;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public PartIteration findPartIterationByBinaryResource(BinaryResource binaryResource) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(binaryResource.getWorkspaceId());
        return binaryResourceDAO.getPartHolder(binaryResource);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision[] getPartRevisionsWithAssignedTasksForGivenUser(String pWorkspaceId, String assignedUserLogin)
            throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<PartRevision> partRs = partRevisionDAO.findPartsWithAssignedTasksForGivenUser(pWorkspaceId, assignedUserLogin);

        ListIterator<PartRevision> ite = partRs.listIterator();
        while (ite.hasNext()) {
            PartRevision partR = ite.next();
            if (!hasPartRevisionReadAccess(user, partR)) {
                ite.remove();
            } else if (isCheckoutByAnotherUser(user, partR)) {
                em.detach(partR);
                partR.removeLastIteration();
            }
        }

        return partRs.toArray(new PartRevision[partRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public PartRevision[] getPartRevisionsWithOpenedTasksForGivenUser(String pWorkspaceId, String assignedUserLogin)
            throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<PartRevision> partRs = partRevisionDAO.findPartsWithOpenedTasksForGivenUser(pWorkspaceId, assignedUserLogin);

        ListIterator<PartRevision> ite = partRs.listIterator();
        while (ite.hasNext()) {
            PartRevision partR = ite.next();
            if (!hasPartRevisionReadAccess(user, partR)) {
                ite.remove();
            } else if (isCheckoutByAnotherUser(user, partR)) {
                em.detach(partR);
                partR.removeLastIteration();
            }
        }

        return partRs.toArray(new PartRevision[partRs.size()]);
    }

    private void checkCyclicPathToPathLink(ConfigurationItem ci, PathToPathLink startLink, List<PathToPathLink> visitedLinks) throws PathToPathCyclicException {

        List<PathToPathLink> nextPathToPathLinks = pathToPathLinkDAO.getNextPathToPathLinkInProduct(ci, startLink);
        for (PathToPathLink nextPathToPathLink : nextPathToPathLinks) {
            if (visitedLinks.contains(nextPathToPathLink)) {
                throw new PathToPathCyclicException();
            }
            visitedLinks.add(nextPathToPathLink);
            checkCyclicPathToPathLink(ci, startLink, visitedLinks);
        }
    }

    private User checkPartRevisionWriteAccess(PartRevisionKey partRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        String workspaceId = partRevisionKey.getPartMaster().getWorkspace();
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        if (user.isAdministrator()) {                                                                                     // Check if it is the workspace's administrator
            return user;
        }

        PartRevision partRevision = partRevisionDAO.loadPartR(partRevisionKey);
        if (partRevision.getACL() == null) {                                                                                // Check if the part haven't ACL
            return userManager.checkWorkspaceWriteAccess(workspaceId);
        }
        if (partRevision.getACL().hasWriteAccess(user)) {                                                                 // Check if the ACL grant write access
            return user;
        }
        throw new AccessRightException(user);                                            // Else throw a AccessRightException
    }

    private User checkPartTemplateWriteAccess(PartMasterTemplate template, User user) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, WorkspaceNotEnabledException {

        if (user.isAdministrator()) {
            // Check if it is the workspace's administrator
            return user;
        }
        if (template.getAcl() == null) {
            // Check if the item haven't ACL
            return userManager.checkWorkspaceWriteAccess(template.getWorkspaceId());
        } else if (template.getAcl().hasWriteAccess(user)) {
            // Check if there is a write access
            return user;
        } else {
            // Else throw a AccessRightException
            throw new AccessRightException(user);
        }


    }

    /**
     * Say if a user, which have access to the workspace, have read access to a part revision
     *
     * @param user         A user which have read access to the workspace
     * @param partRevision The part revision wanted
     * @return True if access is granted, False otherwise
     */
    private boolean hasPartRevisionReadAccess(User user, PartRevision partRevision) {
        return user.isAdministrator() || isACLGrantReadAccess(user, partRevision);
    }

    private boolean hasPartTemplateReadAccess(User user, PartMasterTemplate template) {
        return user.isAdministrator() || isACLGrantReadAccess(user, template);
    }

    /**
     * Say if a user, which have access to the workspace, have write access to a part revision
     *
     * @param user         A user which have read access to the workspace
     * @param partRevision The part revision wanted
     * @return True if access is granted, False otherwise
     */
    private boolean hasPartRevisionWriteAccess(User user, PartRevision partRevision) {
        return user.isAdministrator() || isACLGrantWriteAccess(user, partRevision);
    }

    /**
     * Say if a user can write on a part Revision through ACL or Workspace Access
     *
     * @param user         A user with at least Read access to the workspace
     * @param partRevision the part revision to access
     * @return True if access is granted, False otherwise
     * @throws WorkspaceNotFoundException
     */
    private boolean hasPartOrWorkspaceWriteAccess(User user, PartRevision partRevision) throws WorkspaceNotFoundException, WorkspaceNotEnabledException {
        return partRevision.getACL() == null ?
                userManager.hasWorkspaceWriteAccess(user, partRevision.getWorkspaceId()) : partRevision.getACL().hasWriteAccess(user);
    }

    private boolean isAuthor(User user, PartRevision partRevision) {
        return partRevision.getAuthor().getLogin().equals(user.getLogin());
    }

    private boolean isACLGrantReadAccess(User user, PartRevision partRevision) {
        return partRevision.getACL() == null || partRevision.getACL().hasReadAccess(user);
    }

    private boolean isACLGrantReadAccess(User user, PartMasterTemplate template) {
        return template.getAcl() == null || template.getAcl().hasReadAccess(user);
    }

    private boolean isACLGrantWriteAccess(User user, PartRevision partRevision) {
        return partRevision.getACL() == null || partRevision.getACL().hasWriteAccess(user);
    }

    private boolean isCheckoutByUser(User user, PartRevision partRevision) {
        return partRevision.isCheckedOut() && partRevision.getCheckOutUser().equals(user);
    }

    private boolean isCheckoutByAnotherUser(User user, PartRevision partRevision) {
        return partRevision.isCheckedOut() && !partRevision.getCheckOutUser().equals(user);
    }

    private void checkNameValidity(String name) throws NotAllowedException {
        if (!NamingConvention.correct(name)) {
            throw new NotAllowedException("NotAllowedException9", name);
        }
    }

    private void checkNumberValidity(String name) throws NotAllowedException {
        if (!NamingConvention.correctPartNumber(name)) {
            throw new NotAllowedException("NotAllowedException69", name);
        }
    }

    private void checkNameFileValidity(String name) throws NotAllowedException {
        if (name != null) {
            name = name.trim();
        }
        if (!NamingConvention.correctNameFile(name)) {
            throw new NotAllowedException("NotAllowedException9", name);
        }
    }
}
//TODO when using layers and markers, check for concordance
//TODO add a method to update a marker
