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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.exceptions.EntityNotFoundException;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IAccountManagerLocal;
import com.docdoku.plm.server.rest.dto.AccountDTO;

public class AdminResourceTest {

    @InjectMocks
    AdminResource adminResource;

    @Mock
    private IAccountManagerLocal accountManager;

    @Mock
    private UserGroupMapping userGroupMapping;
    @Spy
    private Account account = new Account("test", "test", "test@test.com", "fr", null, "GMT");
    @Mock
    private AccountDTO accountDTO;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        adminResource.init();
    }

    @Test
    public void updateAccount() throws EntityNotFoundException, NotAllowedException {

        Mockito.when(accountManager.updateAccount(accountDTO.getLogin(), accountDTO.getName(), accountDTO.getEmail(),
                accountDTO.getLanguage(), accountDTO.getNewPassword(), accountDTO.getTimeZone())).thenReturn(account);
        Mockito.when(accountManager.getUserGroupMapping(accountDTO.getLogin())).thenReturn(userGroupMapping);

        Mockito.when(accountManager.getUserGroupMapping(accountDTO.getLogin()).getGroupName())
                .thenReturn(UserGroupMapping.ADMIN_ROLE_ID);

        AccountDTO accountDTOResult = adminResource.updateAccount(accountDTO);
        Assert.assertTrue(accountDTOResult.isAdmin());
    }
}
