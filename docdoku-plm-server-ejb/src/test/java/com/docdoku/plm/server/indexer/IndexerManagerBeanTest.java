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
package com.docdoku.plm.server.indexer;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.core.Bulk;
import io.searchbox.core.DocumentResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.exceptions.AccountNotFoundException;
import com.docdoku.plm.server.core.exceptions.IndexerNotAvailableException;
import com.docdoku.plm.server.core.exceptions.IndexerRequestException;
import com.docdoku.plm.server.core.exceptions.WorkspaceAlreadyExistsException;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.services.IAccountManagerLocal;
import com.docdoku.plm.server.core.services.INotifierLocal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class IndexerManagerBeanTest {

    @InjectMocks
    private IndexerManagerBean indexerManagerBean;

    @Mock
    private JestClient esClient;

    @Mock
    private IndexManagerBean indexManager;

    @Mock
    private IAccountManagerLocal accountManager;

    @Mock
    private INotifierLocal mailer;

    @Mock
    private IndexerQueryBuilder indexerQueryBuilder;

    private String workspaceId = "wks";


    @Test
    public void pingTest() throws IOException {
        Health health = new Health.Builder().build();
        JestResult result = new JestResult(new Gson());
        Mockito.when(esClient.execute(health))
                .thenReturn(result);

        result.setSucceeded(false);
        Assert.assertFalse(indexerManagerBean.ping());

        result.setSucceeded(true);
        Assert.assertTrue(indexerManagerBean.ping());

        Mockito.when(esClient.execute(health)).thenThrow(new IOException());
        Assert.assertFalse(indexerManagerBean.ping());
    }

    @Test
    public void createWorkspaceIndexTest() throws IndexerNotAvailableException {
        Mockito.when(indexManager.indicesExist(workspaceId)).thenReturn(true);

        try {
            indexerManagerBean.createWorkspaceIndex(workspaceId);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().isAssignableFrom(WorkspaceAlreadyExistsException.class));
        }

        Mockito.when(indexManager.indicesExist(workspaceId)).thenReturn(false);

        try {
            indexerManagerBean.createWorkspaceIndex(workspaceId);
            Mockito.verify(indexManager, times(1)).createIndices(workspaceId);
        } catch (WorkspaceAlreadyExistsException | IndexerNotAvailableException | IndexerRequestException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void deleteWorkspaceIndexTest() throws IndexerNotAvailableException, AccountNotFoundException, IndexerRequestException {
        Account account = new Account("foo", "name", "mail", "en", new Date(), "CET");
        Mockito.when(accountManager.getMyAccount()).thenReturn(account);
        Mockito.doNothing().when(indexManager).deleteIndices(workspaceId);

        try {
            indexerManagerBean.deleteWorkspaceIndex(workspaceId);
            Mockito.verify(indexManager, times(1)).deleteIndices(workspaceId);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().isAssignableFrom(WorkspaceAlreadyExistsException.class));
        }

        Mockito.doThrow(new IndexerNotAvailableException()).when(indexManager).deleteIndices(workspaceId);

        indexerManagerBean.deleteWorkspaceIndex(workspaceId);
        Mockito.verify(mailer, times(1)).sendWorkspaceIndexationFailure(ArgumentMatchers.any(Account.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    }

    @Test
    public void indexDocumentIterationTest() throws IndexerNotAvailableException, AccountNotFoundException, IndexerRequestException {
        DocumentResult documentResult = new DocumentResult(new Gson());
        DocumentIteration documentIteration = new DocumentIteration();
        Mockito.when(indexManager.executeUpdate(ArgumentMatchers.isNull()))
            .thenReturn(documentResult);
        indexerManagerBean.indexDocumentIteration(documentIteration);
        Mockito.verify(indexManager,times(1)).executeUpdate(ArgumentMatchers.isNull());

    }

    @Test
    public void indexPartIterationTest() throws IndexerNotAvailableException, AccountNotFoundException, IndexerRequestException {
        DocumentResult documentResult = new DocumentResult(new Gson());
        PartIteration partIteration = new PartIteration();
        Mockito.when(indexManager.executeUpdate(ArgumentMatchers.isNull()))
                .thenReturn(documentResult);
        indexerManagerBean.indexPartIteration(partIteration);
        Mockito.verify(indexManager,times(1)).executeUpdate(ArgumentMatchers.isNull());

    }

    @Test
    public void indexDocumentIterationsTest() throws IndexerRequestException, IndexerNotAvailableException {
        List<DocumentIteration> documentIterations = new ArrayList<>();
        documentIterations.add(new DocumentIteration());
        documentIterations.add(new DocumentIteration());

        indexerManagerBean.indexDocumentIterations(documentIterations);

        Mockito.verify(indexManager,times(1))
                .sendBulk(ArgumentMatchers.any(Bulk.Builder.class));

    }

    @Test
    public void indexPartIterationsTest() throws IndexerRequestException, IndexerNotAvailableException {
        List<PartIteration> partIterations = new ArrayList<>();
        partIterations.add(new PartIteration());
        partIterations.add(new PartIteration());

        indexerManagerBean.indexPartIterations(partIterations);

        Mockito.verify(indexManager,times(1))
                .sendBulk(ArgumentMatchers.any(Bulk.Builder.class));

    }
}
