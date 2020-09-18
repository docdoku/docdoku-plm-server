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

import com.docdoku.plm.server.core.change.*;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserKey;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentIterationKey;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.meta.Tag;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartIterationKey;
import com.docdoku.plm.server.core.security.ACL;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IChangeManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.dao.*;
import com.docdoku.plm.server.factory.ACLFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Florent Garin
 */
@Local(IChangeManagerLocal.class)
@Stateless(name = "ChangeManagerBean")
public class ChangeManagerBean implements IChangeManagerLocal {

    @Inject
    private EntityManager em;

    @Inject
    private ACLDAO aclDAO;

    @Inject
    private ACLFactory aclFactory;

    @Inject
    private ChangeItemDAO changeItemDAO;

    @Inject
    private DocumentRevisionDAO documentRevisionDAO;

    @Inject
    private MilestoneDAO milestoneDAO;

    @Inject
    private PartRevisionDAO partRevisionDAO;

    @Inject
    private TagDAO tagDAO;

    @Inject
    private IUserManagerLocal userManager;

    private static final Logger LOGGER = Logger.getLogger(ChangeManagerBean.class.getName());

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue getChangeIssue(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemReadAccess(changeIssue, user);
        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeIssue> getChangeIssues(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<ChangeIssue> allChangeIssues = changeItemDAO.findAllChangeIssues(pWorkspaceId);
        List<ChangeIssue> visibleChangeIssues = new ArrayList<>();
        for (ChangeIssue changeIssue : allChangeIssues) {
            try {
                checkChangeItemReadAccess(changeIssue, user);
                visibleChangeIssues.add(changeIssue);
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeIssues;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeIssue> getIssuesWithName(String pWorkspaceId, String q, int maxResults) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<ChangeIssue> allChangeIssues = changeItemDAO.findAllChangeIssuesWithReferenceLike(pWorkspaceId, q, maxResults);
        List<ChangeIssue> visibleChangeIssues = new ArrayList<>();
        for (ChangeIssue changeIssue : allChangeIssues) {
            try {
                checkChangeItemReadAccess(changeIssue, user);
                visibleChangeIssues.add(changeIssue);
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeIssues;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue createChangeIssue(String pWorkspaceId, String name, String description, String initiator, ChangeItemPriority priority, String assignee, ChangeItemCategory category)
            throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, WorkspaceNotEnabledException, NotAllowedException, AccountNotFoundException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        User assigneeUser = null;
        if (assignee != null && !assignee.isEmpty() && pWorkspaceId != null && !pWorkspaceId.isEmpty()) {
            if (!userManager.isUserEnabled(assignee, pWorkspaceId)) {
                throw new NotAllowedException("NotAllowedException71");
            }
            assigneeUser = em.find(User.class, new UserKey(pWorkspaceId, assignee));
        }
        ChangeIssue change = new ChangeIssue(name,
                user.getWorkspace(),
                user,
                assigneeUser,
                new Date(),
                description,
                priority,
                category,
                initiator);
        changeItemDAO.createChangeItem(change);
        return change;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue updateChangeIssue(int pId, String pWorkspaceId, String description, ChangeItemPriority priority, String assignee, ChangeItemCategory category) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException, AccountNotFoundException, NotAllowedException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemWriteAccess(changeIssue, user);
        changeIssue.setDescription(description);
        changeIssue.setPriority(priority);
        changeIssue.setCategory(category);

        if (assignee != null && !assignee.isEmpty()) {
            if (!userManager.isUserEnabled(assignee, pWorkspaceId)) {
                throw new NotAllowedException("NotAllowedException71");
            }
            changeIssue.setAssignee(em.find(User.class, new UserKey(pWorkspaceId, assignee)));
        } else {
            changeIssue.setAssignee(null);
        }

        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteChangeIssue(int pId) throws ChangeIssueNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, EntityConstraintException, WorkspaceNotEnabledException {
        ChangeIssue changeIssue = loadChangeIssue(pId);
        User user = userManager.checkWorkspaceReadAccess(changeIssue.getWorkspaceId());
        checkChangeItemWriteAccess(changeIssue, user);

        if (changeItemDAO.hasChangeRequestsLinked(changeIssue)) {
            throw new EntityConstraintException("EntityConstraintException26");
        }

        changeItemDAO.deleteChangeItem(changeIssue);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue saveChangeIssueAffectedDocuments(String pWorkspaceId, int pId, DocumentIterationKey[] pAffectedDocuments) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemWriteAccess(changeIssue, user);
        changeIssue.setAffectedDocuments(getDocumentIterations(pAffectedDocuments));
        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue saveChangeIssueAffectedParts(String pWorkspaceId, int pId, PartIterationKey[] pAffectedParts) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemWriteAccess(changeIssue, user);

        Set<PartIteration> partIterations = getPartIterations(pAffectedParts);

        changeIssue.setAffectedParts(partIterations);
        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue saveChangeIssueTags(String pWorkspaceId, int pId, String[] tagsLabel) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemWriteAccess(changeIssue, user);

        Set<Tag> tags = new HashSet<>();
        for (String label : tagsLabel) {
            tags.add(new Tag(user.getWorkspace(), label));
        }

        List<Tag> existingTags = Arrays.asList(tagDAO.findAllTags(user.getWorkspaceId()));

        Set<Tag> tagsToCreate = new HashSet<>(tags);
        tagsToCreate.removeAll(existingTags);

        for (Tag t : tagsToCreate) {
            try {
                tagDAO.createTag(t, true);
            } catch (CreationException | TagAlreadyExistsException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        changeIssue.setTags(tags);
        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue removeChangeIssueTag(String pWorkspaceId, int pId, String tagName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemWriteAccess(changeIssue, user);
        return (ChangeIssue) changeItemDAO.removeTag(changeIssue, tagName);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest getChangeRequest(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemReadAccess(changeRequest, user);
        return filterLinkedChangeIssues(changeRequest, user);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeRequest> getChangeRequests(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<ChangeRequest> allChangeRequests = changeItemDAO.findAllChangeRequests(pWorkspaceId);
        List<ChangeRequest> visibleChangeRequests = new ArrayList<>();

        for (ChangeRequest changeRequest : allChangeRequests) {
            try {
                checkChangeItemReadAccess(changeRequest, user);
                visibleChangeRequests.add(filterLinkedChangeIssues(changeRequest, user));
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeRequests;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeRequest> getRequestsWithName(String pWorkspaceId, String name, int maxResults) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<ChangeRequest> allChangeRequests = changeItemDAO.findAllChangeRequestsWithReferenceLike(pWorkspaceId, name, maxResults);
        List<ChangeRequest> visibleChangeRequests = new ArrayList<>();
        for (ChangeRequest changeRequest : allChangeRequests) {
            try {
                checkChangeItemReadAccess(changeRequest, user);
                visibleChangeRequests.add(filterLinkedChangeIssues(changeRequest, user));
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeRequests;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest createChangeRequest(String pWorkspaceId, String name, String description, int milestoneId, ChangeItemPriority priority, String assignee, ChangeItemCategory category) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccountNotFoundException, NotAllowedException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        User assigneeUser = null;
        if (assignee != null && !assignee.isEmpty() && pWorkspaceId != null && !pWorkspaceId.isEmpty()) {
            if (!userManager.isUserEnabled(assignee, pWorkspaceId)) {
                throw new NotAllowedException("NotAllowedException71");
            }
            assigneeUser = em.find(User.class, new UserKey(pWorkspaceId, assignee));
        }
        ChangeRequest changeRequest = new ChangeRequest(name,
                user.getWorkspace(),
                user,
                assigneeUser,
                new Date(),
                description,
                priority,
                category,
                em.find(Milestone.class, milestoneId));
        changeItemDAO.createChangeItem(changeRequest);
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest updateChangeRequest(int pId, String pWorkspaceId, String description, int milestoneId, ChangeItemPriority priority, String assignee, ChangeItemCategory category) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException, AccountNotFoundException, NotAllowedException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemWriteAccess(changeRequest, user);
        changeRequest.setDescription(description);
        changeRequest.setPriority(priority);
        changeRequest.setCategory(category);

        if (assignee != null && !assignee.isEmpty()) {
            if (!userManager.isUserEnabled(assignee, pWorkspaceId)) {
                throw new NotAllowedException("NotAllowedException71");
            }
            changeRequest.setAssignee(em.find(User.class, new UserKey(pWorkspaceId, assignee)));
        } else {
            changeRequest.setAssignee(null);
        }

        changeRequest.setMilestone(em.find(Milestone.class, milestoneId));
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteChangeRequest(String pWorkspaceId, int pId) throws ChangeRequestNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, EntityConstraintException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);

        if (changeItemDAO.hasChangeOrdersLinked(changeRequest)) {
            throw new EntityConstraintException("EntityConstraintException10");
        }

        checkChangeItemWriteAccess(changeRequest, user);
        changeItemDAO.deleteChangeItem(changeRequest);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest saveChangeRequestAffectedDocuments(String pWorkspaceId, int pId, DocumentIterationKey[] pAffectedDocuments) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemWriteAccess(changeRequest, user);
        changeRequest.setAffectedDocuments(getDocumentIterations(pAffectedDocuments));
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest saveChangeRequestAffectedParts(String pWorkspaceId, int pId, PartIterationKey[] pAffectedParts) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemWriteAccess(changeRequest, user);

        Set<PartIteration> partIterations = getPartIterations(pAffectedParts);

        changeRequest.setAffectedParts(partIterations);
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest saveChangeRequestAffectedIssues(String pWorkspaceId, int pId, int[] pLinkIds) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemWriteAccess(changeRequest, user);

        Set<ChangeIssue> changeIssues = Arrays.stream(pLinkIds)
                .mapToObj(changeItemDAO::loadChangeIssue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        changeRequest.setAddressedChangeIssues(changeIssues);
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest saveChangeRequestTags(String pWorkspaceId, int pId, String[] tagsLabel) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemWriteAccess(changeRequest, user);

        Set<Tag> tags = new HashSet<>();
        for (String label : tagsLabel) {
            tags.add(new Tag(user.getWorkspace(), label));
        }

        List<Tag> existingTags =
                Arrays.asList(tagDAO.findAllTags(user.getWorkspaceId()));

        Set<Tag> tagsToCreate = new HashSet<>(tags);
        tagsToCreate.removeAll(existingTags);

        for (Tag t : tagsToCreate) {
            try {
                tagDAO.createTag(t, true);
            } catch (CreationException | TagAlreadyExistsException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        changeRequest.setTags(tags);
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest removeChangeRequestTag(String pWorkspaceId, int pId, String tagName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeRequest = loadChangeIssue(pId);
        checkChangeItemWriteAccess(changeRequest, user);
        return (ChangeRequest) changeItemDAO.removeTag(changeRequest, tagName);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder getChangeOrder(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemReadAccess(changeOrder, user);
        return filterLinkedChangeRequests(changeOrder, user);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeOrder> getChangeOrders(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<ChangeOrder> allChangeOrders = changeItemDAO.findAllChangeOrders(pWorkspaceId);
        List<ChangeOrder> visibleChangeOrders = new ArrayList<>();
        for (ChangeOrder changeOrder : allChangeOrders) {
            try {
                checkChangeItemReadAccess(changeOrder, user);
                visibleChangeOrders.add(filterLinkedChangeRequests(changeOrder, user));
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeOrders;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder createChangeOrder(String pWorkspaceId, String name, String description, int milestoneId, ChangeItemPriority priority, String assignee, ChangeItemCategory category) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, WorkspaceNotEnabledException, AccountNotFoundException, NotAllowedException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        User assigneeUser = null;
        if (assignee != null && !assignee.isEmpty() && pWorkspaceId != null && !pWorkspaceId.isEmpty()) {
            if (!userManager.isUserEnabled(assignee, pWorkspaceId)) {
                throw new NotAllowedException("NotAllowedException71");
            }
            assigneeUser = em.find(User.class, new UserKey(pWorkspaceId, assignee));
        }
        ChangeOrder changeOrder = new ChangeOrder(name,
                user.getWorkspace(),
                user,
                assigneeUser,
                new Date(),
                description,
                priority,
                category,
                em.find(Milestone.class, milestoneId));
        changeItemDAO.createChangeItem(changeOrder);
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder updateChangeOrder(int pId, String pWorkspaceId, String description, int milestoneId, ChangeItemPriority priority, String assignee, ChangeItemCategory category) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException, AccountNotFoundException, NotAllowedException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemWriteAccess(changeOrder, user);
        changeOrder.setDescription(description);
        changeOrder.setPriority(priority);
        changeOrder.setCategory(category);

        if (assignee != null && !assignee.isEmpty()) {
            if (!userManager.isUserEnabled(assignee, pWorkspaceId)) {
                throw new NotAllowedException("NotAllowedException71");
            }
            changeOrder.setAssignee(em.find(User.class, new UserKey(pWorkspaceId, assignee)));
        } else {
            changeOrder.setAssignee(null);
        }

        changeOrder.setMilestone(em.find(Milestone.class, milestoneId));
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteChangeOrder(int pId) throws ChangeOrderNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        ChangeOrder changeOrder = loadChangeOrder(pId);
        User user = userManager.checkWorkspaceReadAccess(changeOrder.getWorkspaceId());
        checkChangeItemWriteAccess(changeOrder, user);
        changeItemDAO.deleteChangeItem(changeOrder);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder saveChangeOrderAffectedDocuments(String pWorkspaceId, int pId, DocumentIterationKey[] pAffectedDocuments) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, DocumentRevisionNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemWriteAccess(changeOrder, user);
        changeOrder.setAffectedDocuments(getDocumentIterations(pAffectedDocuments));
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder saveChangeOrderAffectedParts(String pWorkspaceId, int pId, PartIterationKey[] pAffectedParts) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemWriteAccess(changeOrder, user);

        Set<PartIteration> partIterations = getPartIterations(pAffectedParts);

        changeOrder.setAffectedParts(partIterations);
        return changeOrder;
    }

    private Set<PartIteration> getPartIterations(PartIterationKey[] pAffectedParts) {
        return Arrays.stream(pAffectedParts)
                .map(partKey -> partRevisionDAO.loadPartR(partKey.getPartRevision()).getIteration(partKey.getIteration()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder saveChangeOrderAffectedRequests(String pWorkspaceId, int pId, int[] pLinkIds) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemWriteAccess(changeOrder, user);

        Set<ChangeRequest> changeRequests = Arrays.stream(pLinkIds)
                .mapToObj(changeItemDAO::loadChangeRequest)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        changeOrder.setAddressedChangeRequests(changeRequests);
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder saveChangeOrderTags(String pWorkspaceId, int pId, String[] tagsLabel) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemWriteAccess(changeOrder, user);

        Set<Tag> tags = new HashSet<>();
        for (String label : tagsLabel) {
            tags.add(new Tag(user.getWorkspace(), label));
        }

        List<Tag> existingTags =
                Arrays.asList(tagDAO.findAllTags(user.getWorkspaceId()));

        Set<Tag> tagsToCreate = new HashSet<>(tags);
        tagsToCreate.removeAll(existingTags);

        for (Tag t : tagsToCreate) {
            try {
                tagDAO.createTag(t, true);
            } catch (CreationException | TagAlreadyExistsException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        changeOrder.setTags(tags);
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder removeChangeOrderTag(String pWorkspaceId, int pId, String tagName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeOrder = loadChangeIssue(pId);

        checkChangeItemWriteAccess(changeOrder, user);
        return (ChangeOrder) changeItemDAO.removeTag(changeOrder, tagName);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Milestone getMilestone(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pId);
        checkMilestoneReadAccess(milestone, user);
        return milestone;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Milestone getMilestoneByTitle(String pWorkspaceId, String pTitle) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pTitle, pWorkspaceId);
        checkMilestoneReadAccess(milestone, user);
        return milestone;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<Milestone> getMilestones(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<Milestone> allMilestones = milestoneDAO.findAllMilestone(pWorkspaceId);
        List<Milestone> visibleMilestones = new ArrayList<>(allMilestones);
        for (Milestone milestone : allMilestones) {
            try {
                checkMilestoneReadAccess(milestone, user);
            } catch (AccessRightException e) {
                visibleMilestones.remove(milestone);
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleMilestones;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Milestone createMilestone(String pWorkspaceId, String title, String description, Date dueDate) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, MilestoneAlreadyExistsException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        Milestone milestone = new Milestone(title,
                dueDate,
                description,
                user.getWorkspace());
        milestoneDAO.createMilestone(milestone);
        return milestone;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Milestone updateMilestone(int pId, String pWorkspaceId, String title, String description, Date dueDate) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pId);
        checkMilestoneWriteAccess(milestone, user);
        milestone.setTitle(title);
        milestone.setDescription(description);
        milestone.setDueDate(dueDate);
        return milestone;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteMilestone(String pWorkspaceId, int pId) throws MilestoneNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, EntityConstraintException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);

        Milestone milestone = milestoneDAO.loadMilestone(pId);

        checkMilestoneWriteAccess(milestone, user);

        int numberOfOrders = milestoneDAO.getNumberOfOrders(milestone.getId(), milestone.getWorkspaceId());

        if (numberOfOrders > 0) {
            throw new EntityConstraintException("EntityConstraintException8");
        }

        int numberOfRequests = milestoneDAO.getNumberOfRequests(milestone.getId(), milestone.getWorkspaceId());

        if (numberOfRequests > 0) {
            throw new EntityConstraintException("EntityConstraintException9");
        }

        milestoneDAO.deleteMilestone(milestone);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeRequest> getChangeRequestsByMilestone(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pId);
        checkMilestoneReadAccess(milestone, user);
        List<ChangeRequest> affectedRequests = milestoneDAO.getAllRequests(pId, pWorkspaceId);
        List<ChangeRequest> visibleChangeRequests = new ArrayList<>();
        for (ChangeRequest changeRequest : affectedRequests) {
            try {
                checkChangeItemReadAccess(changeRequest, user);
                visibleChangeRequests.add(filterLinkedChangeIssues(changeRequest, user));
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeRequests;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public List<ChangeOrder> getChangeOrdersByMilestone(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pId);
        checkMilestoneReadAccess(milestone, user);
        List<ChangeOrder> affectedOrders = milestoneDAO.getAllOrders(pId, pWorkspaceId);
        List<ChangeOrder> visibleChangeOrders = new ArrayList<>();
        for (ChangeOrder changeOrder : affectedOrders) {
            try {
                checkChangeItemReadAccess(changeOrder, user);
                visibleChangeOrders.add(filterLinkedChangeRequests(changeOrder, user));
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        return visibleChangeOrders;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public int getNumberOfRequestByMilestone(String pWorkspaceId, int milestoneId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return milestoneDAO.getNumberOfRequests(milestoneId, pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public int getNumberOfOrderByMilestone(String pWorkspaceId, int milestoneId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return milestoneDAO.getNumberOfOrders(milestoneId, pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue updateACLForChangeIssue(String pWorkspaceId, int pId, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemGrantAccess(changeIssue, user);

        updateACLForChangeItem(pWorkspaceId, changeIssue, pUserEntries, pGroupEntries);
        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest updateACLForChangeRequest(String pWorkspaceId, int pId, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemGrantAccess(changeRequest, user);

        updateACLForChangeItem(pWorkspaceId, changeRequest, pUserEntries, pGroupEntries);
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder updateACLForChangeOrder(String pWorkspaceId, int pId, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemGrantAccess(changeOrder, user);

        updateACLForChangeItem(pWorkspaceId, changeOrder, pUserEntries, pGroupEntries);
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Milestone updateACLForMilestone(String pWorkspaceId, int pId, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pId);
        checkMilestoneWriteAccess(milestone, user);
        if (milestone.getACL() == null) {
            // Check if already a ACL Rule
            ACL acl = aclFactory.createACL(pWorkspaceId, pUserEntries, pGroupEntries);
            milestone.setACL(acl);
        } else {
            ACL acl = milestone.getACL();
            aclFactory.updateACL(pWorkspaceId, acl, pUserEntries, pGroupEntries);
        }
        return milestone;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeIssue removeACLFromChangeIssue(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeIssueNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeIssue changeIssue = loadChangeIssue(pId);
        checkChangeItemGrantAccess(changeIssue, user);

        removeACLFromChangeItem(changeIssue);
        return changeIssue;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeRequest removeACLFromChangeRequest(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeRequestNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeRequest changeRequest = loadChangeRequest(pId);
        checkChangeItemGrantAccess(changeRequest, user);

        removeACLFromChangeItem(changeRequest);
        return changeRequest;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ChangeOrder removeACLFromChangeOrder(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ChangeOrderNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        ChangeOrder changeOrder = loadChangeOrder(pId);
        checkChangeItemGrantAccess(changeOrder, user);

        removeACLFromChangeItem(changeOrder);
        return changeOrder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Milestone removeACLFromMilestone(String pWorkspaceId, int pId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, MilestoneNotFoundException, AccessRightException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Milestone milestone = milestoneDAO.loadMilestone(pId);
        checkMilestoneWriteAccess(milestone, user);

        ACL acl = milestone.getACL();
        if (acl != null) {
            aclDAO.removeACLEntries(acl);
            milestone.setACL(null);
        }

        return milestone;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean isChangeItemWritable(ChangeItem pChangeItem) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pChangeItem.getWorkspaceId());
        try {
            checkChangeItemWriteAccess(pChangeItem, user);
            return true;
        } catch (AccessRightException e) {
            LOGGER.log(Level.FINEST, null, e);
            return false;
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean isMilestoneWritable(Milestone pMilestone) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        User user = userManager.checkWorkspaceReadAccess(pMilestone.getWorkspaceId());
        try {
            checkMilestoneWriteAccess(pMilestone, user);
            return true;
        } catch (AccessRightException e) {
            LOGGER.log(Level.FINEST, null, e);
            return false;
        }
    }

    private void updateACLForChangeItem(String pWorkspaceId, ChangeItem changeItem, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) {
        if (changeItem.getACL() == null) {
            ACL acl = aclFactory.createACL(pWorkspaceId, pUserEntries, pGroupEntries);
            changeItem.setACL(acl);
        } else {

            aclFactory.updateACL(pWorkspaceId, changeItem.getACL(), pUserEntries, pGroupEntries);
        }
    }

    private void removeACLFromChangeItem(ChangeItem changeItem) {
        ACL acl = changeItem.getACL();
        if (acl != null) {
            aclDAO.removeACLEntries(acl);
            changeItem.setACL(null);
        }
    }

    private User checkChangeItemGrantAccess(ChangeItem pChangeItem, User pUser) throws AccessRightException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        if (pUser.isAdministrator()) {
            return pUser;
        } else if (pUser.getLogin().equals(pChangeItem.getAuthor().getLogin())) {
            checkChangeItemWriteAccess(pChangeItem, pUser);
            return pUser;
        } else {
            throw new AccessRightException(pUser);
        }
    }

    private User checkChangeItemWriteAccess(ChangeItem pChangeItem, User pUser) throws AccessRightException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        if (pUser.isAdministrator()) {
            return pUser;
        }
        if (pChangeItem.getACL() == null) {
            return userManager.checkWorkspaceWriteAccess(pChangeItem.getWorkspaceId());
        } else if (pChangeItem.getACL().hasWriteAccess(pUser)) {
            return pUser;
        } else {
            throw new AccessRightException(pUser);
        }
    }

    private User checkChangeItemReadAccess(ChangeItem pChangeItem, User pUser) throws AccessRightException {
        if (pUser.isAdministrator() ||
                pChangeItem.getACL() == null ||
                pChangeItem.getACL().hasReadAccess(pUser)) {
            return pUser;
        } else {
            throw new AccessRightException(pUser);
        }
    }

    private User checkMilestoneWriteAccess(Milestone pMilestone, User pUser) throws AccessRightException, UserNotFoundException, WorkspaceNotFoundException, WorkspaceNotEnabledException {
        if (pUser.isAdministrator()) {
            return pUser;
        }
        if (pMilestone.getACL() == null) {
            return userManager.checkWorkspaceWriteAccess(pMilestone.getWorkspaceId());
        } else if (pMilestone.getACL().hasWriteAccess(pUser)) {
            return pUser;
        } else {
            throw new AccessRightException(pUser);
        }
    }

    private User checkMilestoneReadAccess(Milestone pMilestone, User pUser) throws AccessRightException {
        if (pUser.isAdministrator() ||
                pMilestone.getACL() == null ||
                pMilestone.getACL().hasReadAccess(pUser)) {
            return pUser;
        } else {
            throw new AccessRightException(pUser);
        }
    }

    private ChangeRequest filterLinkedChangeIssues(ChangeRequest changeRequest, User user) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        em.detach(changeRequest);
        Set<ChangeIssue> addressedChangeIssues = changeRequest.getAddressedChangeIssues();
        Set<ChangeIssue> visibleChangeIssues = new HashSet<>();
        for (ChangeIssue changeIssue : addressedChangeIssues) {
            try {
                checkChangeItemReadAccess(changeIssue, user);
                visibleChangeIssues.add(changeIssue);
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        changeRequest.setAddressedChangeIssues(visibleChangeIssues);
        return changeRequest;
    }

    private ChangeOrder filterLinkedChangeRequests(ChangeOrder changeOrder, User user) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        em.detach(changeOrder);
        Set<ChangeRequest> allChangeRequests = changeOrder.getAddressedChangeRequests();
        Set<ChangeRequest> visibleChangeRequests = new HashSet<>();
        for (ChangeRequest changeRequest : allChangeRequests) {
            try {
                checkChangeItemReadAccess(changeRequest, user);
                visibleChangeRequests.add(filterLinkedChangeIssues(changeRequest, user));
            } catch (AccessRightException e) {
                LOGGER.log(Level.FINEST, null, e);
            }
        }
        changeOrder.setAddressedChangeRequests(visibleChangeRequests);
        return changeOrder;
    }

    private Set<DocumentIteration> getDocumentIterations(DocumentIterationKey[] pAffectedDocuments) throws DocumentRevisionNotFoundException {

        Set<DocumentIteration> documentIterations = new HashSet<>();

        for (DocumentIterationKey docKey : pAffectedDocuments) {

            DocumentRevision documentRevision = documentRevisionDAO.loadDocR(docKey.getDocumentRevision());
            DocumentIteration iteration;

            if (docKey.getIteration() > 0) {
                iteration = documentRevision.getIteration(docKey.getIteration());
            } else {
                iteration = documentRevision.getLastCheckedInIteration();
            }

            if (iteration != null) {
                documentIterations.add(iteration);
            }
        }
        return documentIterations;
    }

    private ChangeIssue loadChangeIssue(int pId) throws ChangeIssueNotFoundException {
        return Optional.of(changeItemDAO.loadChangeIssue(pId))
                .orElseThrow(() -> new ChangeIssueNotFoundException(pId));
    }

    private ChangeRequest loadChangeRequest(int pId) throws ChangeRequestNotFoundException {
        return Optional.of(changeItemDAO.loadChangeRequest(pId))
                .orElseThrow(() -> new ChangeRequestNotFoundException(pId));
    }

    private ChangeOrder loadChangeOrder(int pId) throws ChangeOrderNotFoundException {
        return Optional.of(changeItemDAO.loadChangeOrder(pId))
                .orElseThrow(() -> new ChangeOrderNotFoundException(pId));
    }
}
