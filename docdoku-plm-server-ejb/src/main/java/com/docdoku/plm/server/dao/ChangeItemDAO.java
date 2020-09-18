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
package com.docdoku.plm.server.dao;

import com.docdoku.plm.server.core.change.ChangeIssue;
import com.docdoku.plm.server.core.change.ChangeItem;
import com.docdoku.plm.server.core.change.ChangeOrder;
import com.docdoku.plm.server.core.change.ChangeRequest;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.meta.Folder;
import com.docdoku.plm.server.core.meta.Tag;
import com.docdoku.plm.server.core.product.PartRevisionKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;


@RequestScoped
public class ChangeItemDAO {

    private static final String DOCUMENT_MASTER_ID = "documentMasterId";
    private static final String VERSION = "version";
    private static final String PART_MASTER_NUMBER = "partMasterNumber";
    private static final String FOLDER = "folder";

    @Inject
    private EntityManager em;

    @Inject
    private ACLDAO aclDAO;

    @Inject
    private FolderDAO folderDAO;

    private static final String WORKSPACE_ID = "workspaceId";

    public ChangeItemDAO() {
    }


    public List<ChangeIssue> findAllChangeIssues(String pWorkspaceId) {
        return em.createNamedQuery("ChangeIssue.findChangeIssuesByWorkspace", ChangeIssue.class)
                 .setParameter(WORKSPACE_ID, pWorkspaceId)
                 .getResultList();
    }
    public List<ChangeRequest> findAllChangeRequests(String pWorkspaceId) {
        return em.createNamedQuery("ChangeRequest.findChangeRequestsByWorkspace", ChangeRequest.class)
                 .setParameter(WORKSPACE_ID, pWorkspaceId)
                 .getResultList();
    }
    public List<ChangeOrder> findAllChangeOrders(String pWorkspaceId) {
        return em.createNamedQuery("ChangeOrder.findChangeOrdersByWorkspace", ChangeOrder.class)
                 .setParameter(WORKSPACE_ID, pWorkspaceId)
                 .getResultList();
    }
    
    public ChangeIssue loadChangeIssue(int pId) {
        return  em.find(ChangeIssue.class, pId);
    }

    public ChangeOrder loadChangeOrder(int pId) {
        return em.find(ChangeOrder.class, pId);
    }

    public ChangeRequest loadChangeRequest(int pId) {
        return em.find(ChangeRequest.class, pId);
    }

    public void createChangeItem(ChangeItem pChange) {
        if(pChange.getACL()!=null){
            aclDAO.createACL(pChange.getACL());
        }

        em.persist(pChange);
        em.flush();
    }
    public void deleteChangeItem(ChangeItem pChange) {
        em.remove(pChange);
        em.flush();
    }
    public ChangeItem removeTag(ChangeItem pChange, String tagName){
        Tag tagToRemove = new Tag(pChange.getWorkspace(), tagName);
        pChange.getTags().remove(tagToRemove);
        return pChange;
    }

    public List<ChangeIssue> findAllChangeIssuesWithReferenceLike(String pWorkspaceId, String reference, int maxResults) {
        return em.createNamedQuery("ChangeIssue.findByName",ChangeIssue.class)
                .setParameter(WORKSPACE_ID, pWorkspaceId).setParameter("name", "%" + reference + "%").setMaxResults(maxResults).getResultList();
    }

    public List<ChangeRequest> findAllChangeRequestsWithReferenceLike(String pWorkspaceId, String reference, int maxResults) {
        return em.createNamedQuery("ChangeRequest.findByName",ChangeRequest.class)
                .setParameter(WORKSPACE_ID, pWorkspaceId).setParameter("name", "%" + reference + "%").setMaxResults(maxResults).getResultList();
    }

    public List<ChangeItem> findChangeItemByTag(String pWorkspaceId, Tag tag){
        List<ChangeItem> changeItems = new ArrayList<>();
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeIssue c WHERE :tag MEMBER OF c.tags AND c.workspace.id = :workspaceId ", ChangeIssue.class)
                .setParameter("tag", tag)
                .setParameter(WORKSPACE_ID, pWorkspaceId)
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeRequest c WHERE :tag MEMBER OF c.tags AND c.workspace.id = :workspaceId ", ChangeRequest.class)
                .setParameter("tag", tag)
                .setParameter(WORKSPACE_ID, pWorkspaceId)
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeOrder c WHERE :tag MEMBER OF c.tags AND c.workspace.id = :workspaceId ", ChangeOrder.class)
                .setParameter("tag", tag)
                .setParameter(WORKSPACE_ID, pWorkspaceId)
                .getResultList());
        return changeItems;
    }

    public List<ChangeItem> findChangeItemByDoc(DocumentRevisionKey documentRevisionKey){
        String workspaceId = documentRevisionKey.getDocumentMaster().getWorkspace();
        String id = documentRevisionKey.getDocumentMaster().getId();
        List<ChangeItem> changeItems = new ArrayList<>();
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeIssue c , DocumentIteration i WHERE i member of c.affectedDocuments AND i.documentRevision.documentMaster.workspace.id = :workspaceId AND i.documentRevision.version = :version AND i.documentRevision.documentMasterId = :documentMasterId", ChangeIssue.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .setParameter(DOCUMENT_MASTER_ID, id)
                .setParameter(VERSION, documentRevisionKey.getVersion())
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeRequest c , DocumentIteration i WHERE i member of c.affectedDocuments AND i.documentRevision.documentMaster.workspace.id = :workspaceId AND i.documentRevision.version = :version AND i.documentRevision.documentMasterId = :documentMasterId", ChangeRequest.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .setParameter(DOCUMENT_MASTER_ID, id)
                .setParameter(VERSION, documentRevisionKey.getVersion())
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeOrder c , DocumentIteration i WHERE i member of c.affectedDocuments AND i.documentRevision.documentMaster.workspace.id = :workspaceId AND i.documentRevision.version = :version AND i.documentRevision.documentMasterId = :documentMasterId", ChangeOrder.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .setParameter(DOCUMENT_MASTER_ID, id)
                .setParameter(VERSION, documentRevisionKey.getVersion())
                .getResultList());
        return changeItems;
    }

    public List<ChangeItem> findChangeItemByPart(PartRevisionKey partRevisionKey){
        String workspaceId = partRevisionKey.getPartMaster().getWorkspace();
        String id = partRevisionKey.getPartMasterNumber();
        List<ChangeItem> changeItems = new ArrayList<>();
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeIssue c , PartIteration i WHERE i member of c.affectedParts AND i.partRevision.partMaster.workspace.id = :workspaceId AND i.partRevision.version = :version AND i.partRevision.partMasterNumber = :partMasterNumber", ChangeIssue.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .setParameter(PART_MASTER_NUMBER, id)
                .setParameter(VERSION, partRevisionKey.getVersion())
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeRequest c , PartIteration i WHERE i member of c.affectedParts AND i.partRevision.partMaster.workspace.id = :workspaceId AND i.partRevision.version = :version AND i.partRevision.partMasterNumber = :partMasterNumber", ChangeRequest.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .setParameter(PART_MASTER_NUMBER, id)
                .setParameter(VERSION, partRevisionKey.getVersion())
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeOrder c , PartIteration i WHERE i member of c.affectedParts AND i.partRevision.partMaster.workspace.id = :workspaceId AND i.partRevision.version = :version AND i.partRevision.partMasterNumber = :partMasterNumber", ChangeOrder.class)
                .setParameter(WORKSPACE_ID, workspaceId)
                .setParameter(PART_MASTER_NUMBER, id)
                .setParameter(VERSION, partRevisionKey.getVersion())
                .getResultList());
        return changeItems;
    }
    
    public boolean hasChangeItems(DocumentRevisionKey documentRevisionKey){
        return !findChangeItemByDoc(documentRevisionKey).isEmpty();
    }

    public boolean hasChangeItems(PartRevisionKey partRevisionKey){
        return !findChangeItemByPart(partRevisionKey).isEmpty();
    }


    public List<ChangeItem> findChangeItemByFolder(Folder folder){
        List<ChangeItem> changeItems = new ArrayList<>();
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeIssue c , DocumentIteration i WHERE i member of c.affectedDocuments AND i.documentRevision.location = :folder", ChangeIssue.class)
                .setParameter(FOLDER, folder)
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeRequest c , DocumentIteration i WHERE i member of c.affectedDocuments AND i.documentRevision.location = :folder", ChangeRequest.class)
                .setParameter(FOLDER, folder)
                .getResultList());
        changeItems.addAll(em.createQuery("SELECT c FROM ChangeOrder c , DocumentIteration i WHERE i member of c.affectedDocuments AND i.documentRevision.location = :folder", ChangeOrder.class)
                .setParameter(FOLDER, folder)
                .getResultList());
        return changeItems;
    }

    public boolean hasChangeItems(Folder pFolder) {
        return !findChangeItemByFolder(pFolder).isEmpty();
    }

    public boolean hasChangeRequestsLinked(ChangeIssue changeIssue) {
        return !findAllChangeRequestsByChangeIssue(changeIssue).isEmpty();
    }
    
    public boolean hasChangeOrdersLinked(ChangeRequest changeRequest) {
        return !findAllChangeOrdersByChangeRequest(changeRequest).isEmpty();
    }

    private List<ChangeRequest> findAllChangeRequestsByChangeIssue(ChangeIssue changeIssue) {
        return em.createNamedQuery("ChangeRequest.findByChangeIssue", ChangeRequest.class)
                .setParameter(WORKSPACE_ID, changeIssue.getWorkspaceId())
                .setParameter("changeIssue", changeIssue)
                .getResultList();
    }

    private List<ChangeOrder> findAllChangeOrdersByChangeRequest(ChangeRequest changeRequest) {
        return em.createNamedQuery("ChangeOrder.findByChangeRequest", ChangeOrder.class)
                .setParameter(WORKSPACE_ID, changeRequest.getWorkspaceId())
                .setParameter("changeRequest", changeRequest)
                .getResultList();
    }

}
