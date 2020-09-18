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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.exceptions.ApplicationException;
import com.docdoku.plm.server.core.exceptions.EntityNotFoundException;
import com.docdoku.plm.server.core.exceptions.GCMAccountNotFoundException;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.config.AuthConfig;
import com.docdoku.plm.server.rest.dto.AccountDTO;
import com.docdoku.plm.server.rest.dto.GCMAccountDTO;
import com.docdoku.plm.server.rest.dto.WorkspaceDTO;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.ArrayList;

import static org.mockito.MockitoAnnotations.initMocks;

public class AccountResourceTest {

    @InjectMocks
    private AccountResource accountResource = new AccountResource();

    @Mock
    private EntityManager em;

    @Mock
    private IAccountManagerLocal accountManager;

    @Mock
    private IContextManagerLocal contextManager;

    @Mock
    private IOAuthManagerLocal authManager;

    @Mock
    private AuthConfig authConfig;

    @Mock
    private IUserManagerLocal userManager;

    @Mock
    private ITokenManagerLocal tokenManager;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        accountResource.init();
    }

    @Test
    public void getAccountTest() throws ApplicationException {
        Account account = new Account("FooBar");
        Integer someProviderId = 42;

        Mockito.when(authManager.getProviderId(account)).thenReturn(someProviderId);
        Mockito.when(accountManager.getMyAccount()).thenReturn(account);

        Mockito.when(contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)).thenReturn(true);
        AccountDTO adminAccount = accountResource.getAccount();
        Assert.assertNotNull(adminAccount);
        Assert.assertTrue(adminAccount.isAdmin());

        Mockito.when(contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)).thenReturn(false);
        AccountDTO userAccount = accountResource.getAccount();
        Assert.assertNotNull(userAccount);
        Assert.assertFalse(userAccount.isAdmin());

        Assert.assertEquals(someProviderId, userAccount.getProviderId());
    }

    @Test
    public void updateAccountTest() throws ApplicationException, UnsupportedEncodingException {

        Key key = new HmacKey("verySecretPhrase".getBytes("UTF-8"));
        UserGroupMapping groupMapping = new UserGroupMapping("FooBar", UserGroupMapping.REGULAR_USER_ROLE_ID);
        String authToken = tokenManager.createAuthToken(key, groupMapping);
        Account account = new Account("FooBar");
        Mockito.when(authConfig.getJWTKey()).thenReturn(key);

        Mockito.when(accountManager.updateAccount(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(account);

        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setLogin(account.getLogin());
        Response res = accountResource.updateAccount(null, accountDTO);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), res.getStatus());

        accountDTO.setPassword("");
        res = accountResource.updateAccount(null, accountDTO);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), res.getStatus());

        res = accountResource.updateAccount("WithoutBearer " + authToken, accountDTO);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), res.getStatus());

        Mockito.when(tokenManager.isJWTValidBefore(ArgumentMatchers.any(), ArgumentMatchers.anyInt() , ArgumentMatchers.anyString())).thenReturn(true);
        res = accountResource.updateAccount("Bearer " + authToken, accountDTO);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        accountDTO.setPassword("SomePass");
        Mockito.when(accountManager.authenticateAccount(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(null);
        try {
            accountResource.updateAccount(null, accountDTO);
            Assert.fail("Should have thrown");
        } catch (NotAllowedException e) {
            Assert.assertNotNull(e.getMessage());
        }

        Mockito.when(accountManager.authenticateAccount(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(account);
        res = accountResource.updateAccount(null, accountDTO);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        Mockito.when(contextManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)).thenReturn(true);
        res = accountResource.updateAccount(null, accountDTO);
        Object entity = res.getEntity();
        Assert.assertTrue(((AccountDTO) entity).isAdmin());
    }

    @Test
    public void createAccountTest() throws ApplicationException, IOException, ServletException {
        Key key = new HmacKey("verySecretPhrase".getBytes("UTF-8"));

        Mockito.when(authConfig.getJWTKey()).thenReturn(key);
        Mockito.when(tokenManager.createAuthToken(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn("whatever");


        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse mockedResponse = Mockito.mock(HttpServletResponse.class);
        HttpSession mockedSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockedRequest.getSession()).thenReturn(mockedSession);

        AccountDTO accountDTO = new AccountDTO();
        Account account = new Account();

        Mockito.when(accountManager.createAccount(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(account);


        Response res = accountResource.createAccount(mockedRequest, mockedResponse, accountDTO);
        Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), res.getStatus());

        account.setEnabled(true);
        res = accountResource.createAccount(mockedRequest, mockedResponse, accountDTO);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());


        Mockito.when(authConfig.isJwtEnabled()).thenReturn(true);
        res = accountResource.createAccount(mockedRequest, mockedResponse, accountDTO);
        Assert.assertNotNull(res.getHeaderString("jwt"));

        Mockito.when(authConfig.isJwtEnabled()).thenReturn(false);
        res = accountResource.createAccount(mockedRequest, mockedResponse, accountDTO);
        Assert.assertNull(res.getHeaderString("jwt"));


        Mockito.when(mockedRequest.authenticate(mockedResponse))
                .thenThrow(new IOException("Mocked exception"));
        res = accountResource.createAccount(mockedRequest, mockedResponse, accountDTO);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), res.getStatus());

    }

    @Test
    public void getWorkspacesTest() {
        Workspace[] workspaces = new Workspace[]{new Workspace("wks")};
        Mockito.when(userManager.getWorkspacesWhereCallerIsActive())
                .thenReturn(workspaces);
        Response response = accountResource.getWorkspaces();
        Object entity = response.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        ArrayList workspaceList = (ArrayList) entity;
        Object workspaceObject = workspaceList.get(0);
        Assert.assertTrue(workspaceObject.getClass().isAssignableFrom(WorkspaceDTO.class));
        WorkspaceDTO workspace = (WorkspaceDTO) workspaceObject;
        Assert.assertEquals(workspaces[0].getId(), workspace.getId());
    }

    @Test
    public void setGCMAccountTest() throws ApplicationException {
        GCMAccountDTO data = new GCMAccountDTO();
        Response response = accountResource.setGCMAccount(data);
        Mockito.doNothing().when(accountManager).setGCMAccount(data.getGcmId());
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteGCMAccountTest() throws EntityNotFoundException {

        Response response = accountResource.deleteGCMAccount();
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        Mockito.doThrow(new GCMAccountNotFoundException("foo")).when(accountManager).deleteGCMAccount();

        try {
            accountResource.deleteGCMAccount();
            Assert.fail("Should have thrown");
        } catch (EntityNotFoundException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

}
