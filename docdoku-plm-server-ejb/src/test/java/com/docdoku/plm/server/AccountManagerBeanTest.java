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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.exceptions.AccountNotFoundException;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.services.IContextManagerLocal;
import com.docdoku.plm.server.config.ServerConfig;
import com.docdoku.plm.server.dao.AccountDAO;
import java.util.Date;
import org.junit.Assert;
public class AccountManagerBeanTest {
    @InjectMocks
    private AccountManagerBean accountManager = new AccountManagerBean();
    @Mock
    private IContextManagerLocal contextManager;
    @Mock
    private AccountDAO accountDAO;
    @Mock
    private ServerConfig serverConfig;
    @Spy
    private Account account = new Account("login", "user", "test@docdoku.com", "en", new Date(), null);
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void updateAccountTest()throws AccountNotFoundException,NotAllowedException{
        Account accountClone =  account.clone();
        Mockito.when(accountDAO.loadAccount(contextManager.getCallerPrincipalLogin())).thenReturn(accountClone);
        Account accountResult = accountManager.updateAccount(account.getName(), "test@test.com","fr", "test", "GMT");
        Assert.assertNotEquals(account.getEmail(), accountResult.getEmail());
        try {
            accountManager.updateAccount(account.getName(), account.getEmail(),"bidon", "test", "GMT");
            Assert.fail();
        }catch(NotAllowedException e){
            Assert.assertNotNull(e.getMessage());
        }
        try {
            accountManager.updateAccount(account.getName(), account.getEmail(),"fr", "test", "bidon");
            Assert.fail();
        }catch(NotAllowedException e){
            Assert.assertNotNull(e.getMessage());
        }
    }
    @Test
    public void updateAccountTestAdmin()throws AccountNotFoundException,NotAllowedException{
        Account accountClone =  account.clone();
        Mockito.when(accountManager.getAccount("login")).thenReturn(accountClone);
        Account accountResult = accountManager.updateAccount(account.getLogin(), account.getName(), "test@test.com", "fr", "test", "GMT");
        Assert.assertNotEquals(account.getEmail(), accountResult.getEmail());
        try {
            accountManager.updateAccount(account.getLogin(), account.getName(), account.getEmail(), "bidon", "test", "GMT");
            Assert.fail();
        }catch(NotAllowedException e){
            Assert.assertNotNull(e.getMessage());
        }
        try {
            accountManager.updateAccount(account.getLogin(), account.getName(), account.getEmail(), "fr", "test", "bidon");
            Assert.fail();
        }catch(NotAllowedException e){
            Assert.assertNotNull(e.getMessage());
        }
    }
}