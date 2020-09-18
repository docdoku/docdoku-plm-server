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

package com.docdoku.plm.server.documents;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.configuration.DocumentBaseline;
import com.docdoku.plm.server.core.configuration.DocumentBaselineType;
import com.docdoku.plm.server.core.document.DocumentMaster;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.meta.Folder;
import com.docdoku.plm.server.core.services.IDocumentManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.i18n.PropertiesLoader;
import com.docdoku.plm.server.dao.DocumentBaselineDAO;
import com.docdoku.plm.server.dao.DocumentRevisionDAO;
import com.docdoku.plm.server.dao.WorkspaceDAO;
import com.docdoku.plm.server.util.DocumentUtil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class DocumentBaselineManagerBeanTest {

    private static final String PROPERTIES_BASE_NAME = "/com/docdoku/core/i18n/LocalStrings";

    @Mock
    private WorkspaceDAO workspaceDAO;
    @Mock
    private IUserManagerLocal userManager;
    @Mock
    private EntityManager em;
    @Mock
    private TypedQuery<Folder> folderTypedQuery;
    @Mock
    private IDocumentManagerLocal documentService;
    @Mock
    private DocumentRevisionDAO documentRevisionDAO;
    @Mock
    private DocumentBaselineDAO documentBaselineDAO;

    @InjectMocks
    private DocumentBaselineManagerBean docBaselineManagerBean = new DocumentBaselineManagerBean();

    private Account account = new Account(DocumentUtil.USER_2_LOGIN, DocumentUtil.USER_2_NAME, DocumentUtil.USER2_MAIL, DocumentUtil.LANGUAGE, new Date(), null);
    private Workspace workspace = new Workspace("workspace01", account, DocumentUtil.WORKSPACE_DESCRIPTION, false);
    private User user = new User(workspace, new Account(DocumentUtil.USER_1_LOGIN, DocumentUtil.USER_1_NAME, DocumentUtil.USER1_MAIL, DocumentUtil.LANGUAGE, new Date(), null));
    private Folder folder = new Folder("workspace01");

    /**
     * test that we cannot baseline an empty collection of documents
     *
     * @throws Exception
     */
    @Test
    public void shouldNotBaselineAnEmptyCollection() throws Exception {
        //Given
        Mockito.when(userManager.checkWorkspaceWriteAccess(workspace.getId())).thenReturn(user);
        //Mockito.when(em.find(Workspace.class, workspace.getId())).thenReturn(workspace);
        //Mockito.when(em.createQuery("SELECT DISTINCT f FROM Folder f WHERE f.parentFolder.completePath = :completePath", Folder.class)).thenReturn(folderTypedQuery);
        //Mockito.when(folderTypedQuery.getResultList()).thenReturn(new ArrayList<>(0));
        //Mockito.when(workspaceDAO.loadWorkspace(workspace.getId())).thenReturn(workspace);
        //Mockito.when(em.find(Folder.class, workspace.getId())).thenReturn(folder);
        //Mockito.when(documentService.getAllDocumentsInWorkspace(workspace.getId())).thenReturn(new DocumentRevision[0]);

        //when
        try {
            docBaselineManagerBean.createBaseline(workspace.getId(), "name", DocumentBaselineType.RELEASED, "description", new ArrayList<>());
        } catch (NotAllowedException e) {
            Properties properties = PropertiesLoader.loadLocalizedProperties(user.getLocale(), PROPERTIES_BASE_NAME, getClass());
            String expected = properties.getProperty("NotAllowedException66");
            Assert.assertEquals(expected, e.getMessage());
        }

    }

    /**
     * test that we can baseline documents
     *
     * @throws Exception
     */
    @Test
    public void baselineDocuments() throws Exception {
        //Given
        Mockito.when(userManager.checkWorkspaceWriteAccess(workspace.getId())).thenReturn(user);
        //Mockito.when(em.find(Workspace.class, workspace.getId())).thenReturn(workspace);
        //Mockito.when(em.createQuery("SELECT DISTINCT f FROM Folder f WHERE f.parentFolder.completePath = :completePath", Folder.class)).thenReturn(folderTypedQuery);
        //Mockito.when(folderTypedQuery.getResultList()).thenReturn(new ArrayList<>(0));
        //Mockito.when(workspaceDAO.loadWorkspace(workspace.getId())).thenReturn(workspace);
        //Mockito.when(em.find(Folder.class, workspace.getId())).thenReturn(folder);
        DocumentRevision[] revisions = new DocumentRevision[2];

        DocumentMaster documentMaster1 = new DocumentMaster(workspace, "doc1", user);
        DocumentMaster documentMaster2 = new DocumentMaster(workspace, "doc2", user);
        documentMaster1.setId("doc001");
        documentMaster2.setId("doc002");

        DocumentRevision documentRevision1 = documentMaster1.createNextRevision(user);
        DocumentRevision documentRevision2 = documentMaster2.createNextRevision(user);

        revisions[0] = documentRevision2;
        revisions[1] = documentRevision1;
        documentRevision1.createNextIteration(user);
        documentRevision2.createNextIteration(user);
        documentRevision1.setLocation(folder);
        documentRevision2.setLocation(folder);
        //Mockito.when(em.find(DocumentRevision.class, documentRevision1.getKey())).thenReturn(documentRevision1);
        //Mockito.when(em.find(DocumentRevision.class, documentRevision2.getKey())).thenReturn(documentRevision2);

        //Mockito.when(documentService.getAllDocumentsInWorkspace(workspace.getId())).thenReturn(revisions);
        //Mockito.when(userManager.checkWorkspaceReadAccess(workspace.getId())).thenReturn(user);
        Mockito.when(documentRevisionDAO.loadDocR(documentRevision1.getKey())).thenReturn(documentRevision1);
        Mockito.when(documentRevisionDAO.loadDocR(documentRevision2.getKey())).thenReturn(documentRevision2);

        //when
        List<DocumentRevisionKey> documentRevisionKeys = new ArrayList<>();
        documentRevisionKeys.add(new DocumentRevisionKey(workspace.getId(), documentMaster1.getId(), documentMaster1.getLastRevision().getVersion()));
        documentRevisionKeys.add(new DocumentRevisionKey(workspace.getId(), documentMaster2.getId(), documentMaster2.getLastRevision().getVersion()));
        DocumentBaseline documentBaseline = docBaselineManagerBean.createBaseline(workspace.getId(), "name", DocumentBaselineType.LATEST, "description", documentRevisionKeys);

        //Then
        Assert.assertNotNull(documentBaseline);
        Assert.assertTrue(documentBaseline.hasBaselinedDocument(documentRevision1.getKey()));
        Assert.assertTrue(documentBaseline.hasBaselinedDocument(documentRevision2.getKey()));
        Assert.assertNotNull(documentBaseline.getBaselinedDocument(documentRevision1.getKey()));
        Assert.assertNotNull(documentBaseline.getBaselinedDocument(documentRevision2.getKey()));
    }

}
