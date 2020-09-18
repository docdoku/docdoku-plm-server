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

package com.docdoku.plm.server.rest;

import org.jose4j.keys.HmacKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.meta.Folder;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartRevision;
import com.docdoku.plm.server.core.product.PartRevisionKey;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.core.sharing.SharedDocument;
import com.docdoku.plm.server.core.sharing.SharedEntity;
import com.docdoku.plm.server.core.sharing.SharedPart;
import com.docdoku.plm.server.core.util.HashUtils;
import com.docdoku.plm.server.config.AuthConfig;

import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;

import static org.mockito.MockitoAnnotations.initMocks;

public class SharedResourceTest {

    @InjectMocks
    private SharedResource sharedResource = new SharedResource();

    @Mock
    private IPublicEntityManagerLocal publicEntityManager;
    @Mock
    private IDocumentManagerLocal documentManager;
    @Mock
    private IProductManagerLocal productManager;
    @Mock
    private IShareManagerLocal shareManager;
    @Mock
    private IContextManagerLocal contextManager;
    @Mock
    private AuthConfig authConfig;
    @Mock
    private ITokenManagerLocal tokenManager;

    private String workspaceId = "wks";
    private String partNumber = "partM";
    private String documentId = "docM";
    private String documentVersion = "A";
    private String partVersion = "A";

    @Before
    public void setup() throws Exception {
        initMocks(this);
        sharedResource.init();
        Key key = new HmacKey("verySecretPhrase".getBytes("UTF-8"));
        Mockito.when(authConfig.getJWTKey()).thenReturn(key);
    }

    @Test
    public void getPublicSharedDocumentRevisionTest() throws ApplicationException {

        DocumentRevisionKey docKey = new DocumentRevisionKey(workspaceId, documentId, documentVersion);
        DocumentRevision documentRevision = new DocumentRevision();
        documentRevision.setLocation(new Folder(workspaceId, "somepath"));

        Mockito.when(publicEntityManager.getPublicDocumentRevision(docKey))
            .thenReturn(null);
        Response response = sharedResource.getPublicSharedDocumentRevision(workspaceId, documentId, documentVersion);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        Mockito.when(documentManager.getDocumentRevision(docKey))
                .thenReturn(null);
        Mockito.when(contextManager.isCallerInRole(UserGroupMapping.REGULAR_USER_ROLE_ID))
                .thenReturn(true);

        response = sharedResource.getPublicSharedDocumentRevision(workspaceId, documentId, documentVersion);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());


        Mockito.when(publicEntityManager.getPublicDocumentRevision(docKey))
                .thenReturn(documentRevision);

        response = sharedResource.getPublicSharedDocumentRevision(workspaceId, documentId, documentVersion);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

    @Test
    public void getPublicSharedPartRevisionTest() throws ApplicationException {
        PartRevisionKey partKey = new PartRevisionKey(workspaceId, partNumber, partVersion);
        PartRevision partRevision = new PartRevision();
        partRevision.setPartMasterNumber(partNumber);
        partRevision.setVersion(partVersion);
        PartMaster partMaster = new PartMaster();
        partMaster.setType("type");
        partRevision.setPartMaster(partMaster);
        User author = new User();
        partRevision.setAuthor(author);

        Mockito.when(publicEntityManager.getPublicPartRevision(partKey))
                .thenReturn(null);
        Response response = sharedResource.getPublicSharedPartRevision(workspaceId, partNumber, partVersion);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        Mockito.when(productManager.getPartRevision(partKey))
                .thenReturn(null);
        Mockito.when(contextManager.isCallerInRole(UserGroupMapping.REGULAR_USER_ROLE_ID))
                .thenReturn(true);

        response = sharedResource.getPublicSharedPartRevision(workspaceId, partNumber, partVersion);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());


        Mockito.when(publicEntityManager.getPublicPartRevision(partKey))
                .thenReturn(partRevision);

        response = sharedResource.getPublicSharedPartRevision(workspaceId, partNumber, partVersion);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

    @Test
    public void getDocumentWithSharedEntityTest() throws ApplicationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String uuid = "uuid";
        Workspace workspace = new Workspace(workspaceId);

        User user = new User();
        DocumentRevision document = new DocumentRevision();
        SharedEntity sharedEntity = new SharedDocument(workspace, user, document);
        sharedEntity.setUuid(uuid);

        Mockito.when(shareManager.findSharedEntityForGivenUUID(uuid))
                        .thenReturn(sharedEntity);

        Response response = sharedResource.getDocumentWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR, -2);

        sharedEntity.setExpireDate(c.getTime());
        response = sharedResource.getDocumentWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        Assert.assertEquals("{\"forbidden\":\"entity-expired\"}", response.getEntity());

        c.setTime(new Date());
        c.add(Calendar.HOUR, 4);

        sharedEntity.setExpireDate(c.getTime());
        response = sharedResource.getDocumentWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());

        sharedEntity.setPassword(HashUtils.md5Sum("foo"));
        response = sharedResource.getDocumentWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        response = sharedResource.getDocumentWithSharedEntity("foo", uuid);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());

    }

    @Test
    public void getPartWithSharedEntityTest() throws ApplicationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String uuid = "uuid";
        Workspace workspace = new Workspace(workspaceId);

        User user = new User();
        PartRevision partRevision = new PartRevision();
        partRevision.setPartMasterNumber(partNumber);
        partRevision.setVersion(partVersion);
        PartMaster partMaster = new PartMaster();
        partMaster.setType("type");
        partRevision.setPartMaster(partMaster);
        User author = new User();
        partRevision.setAuthor(author);
        SharedEntity sharedEntity = new SharedPart(workspace, user, partRevision);
        sharedEntity.setUuid(uuid);

        Mockito.when(shareManager.findSharedEntityForGivenUUID(uuid))
                .thenReturn(sharedEntity);

        Response response = sharedResource.getPartWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR, -2);

        sharedEntity.setExpireDate(c.getTime());
        response = sharedResource.getPartWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        Assert.assertEquals("{\"forbidden\":\"entity-expired\"}", response.getEntity());

        c.setTime(new Date());
        c.add(Calendar.HOUR, 4);

        sharedEntity.setExpireDate(c.getTime());
        response = sharedResource.getPartWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());

        sharedEntity.setPassword(HashUtils.md5Sum("foo"));
        response = sharedResource.getPartWithSharedEntity(null, uuid);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        response = sharedResource.getPartWithSharedEntity("foo", uuid);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());

    }

}
