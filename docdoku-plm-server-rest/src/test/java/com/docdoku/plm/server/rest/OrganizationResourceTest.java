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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.Organization;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.services.IAccountManagerLocal;
import com.docdoku.plm.server.core.services.IOrganizationManagerLocal;
import com.docdoku.plm.server.rest.dto.AccountDTO;
import com.docdoku.plm.server.rest.dto.OrganizationDTO;
import com.docdoku.plm.server.rest.dto.UserDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

public class OrganizationResourceTest {

    @InjectMocks
    private OrganizationResource organizationResource = new OrganizationResource();

    @Mock
    private IAccountManagerLocal accountManager;

    @Mock
    private IOrganizationManagerLocal organizationManager;

    private Account account = new Account("test");
    private Organization organization = new Organization("org", account, "description");

    @Before
    public void setup() throws Exception {
        initMocks(this);
        organizationResource.init();
    }

    @Test
    public void getOrganizationTest() throws EntityNotFoundException {

        Mockito.when(organizationManager.getMyOrganization()).thenReturn(organization);
        OrganizationDTO organizationDTO = organizationResource.getOrganization();

        Assert.assertEquals(organization.getName(), organizationDTO.getName());
        Assert.assertEquals(organization.getDescription(), organizationDTO.getDescription());

        Mockito.when(organizationManager.getMyOrganization())
                .thenThrow(new OrganizationNotFoundException("org"));

        organizationDTO = organizationResource.getOrganization();
        Assert.assertNull(organizationDTO);

    }

    @Test
    public void createOrganizationTest() throws NotAllowedException, EntityNotFoundException, EntityAlreadyExistsException, CreationException {
        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setName(organization.getName());
        organizationDTO.setDescription(organization.getDescription());

        Mockito.when(organizationManager.createOrganization(organizationDTO.getName(), organizationDTO.getDescription()))
                .thenReturn(organization);

        OrganizationDTO organization = organizationResource.createOrganization(organizationDTO);
        Assert.assertEquals(organizationDTO.getName(), organization.getName());
        Assert.assertEquals(organizationDTO.getDescription(), organization.getDescription());

    }

    @Test
    public void updateOrganizationTest() throws EntityNotFoundException, AccessRightException {
        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setName(organization.getName());
        organizationDTO.setDescription("new desc");

        Mockito.when(organizationManager.getMyOrganization()).thenReturn(organization);
        Mockito.doNothing().when(organizationManager).updateOrganization(organization);

        OrganizationDTO organization = organizationResource.updateOrganization(organizationDTO);
        Assert.assertEquals(organizationDTO.getName(), organization.getName());
        Assert.assertEquals(organizationDTO.getDescription(), organization.getDescription());
    }



    @Test
    public void deleteOrganizationTest() throws EntityNotFoundException, AccessRightException {
        Mockito.when(organizationManager.getMyOrganization()).thenReturn(organization);
        Mockito.doNothing().when(organizationManager).deleteOrganization(organization.getName());
        Response response = organizationResource.deleteOrganization();
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }


    @Test
    public void getMembersTest() throws EntityNotFoundException, AccessRightException {
        organization.addMember(account);
        Mockito.when(organizationManager.getMyOrganization()).thenReturn(organization);
        Response response = organizationResource.getMembers();
        Object entity = response.getEntity();
        Assert.assertTrue(entity.getClass().isAssignableFrom(ArrayList.class));
        List objects = (ArrayList) entity;
        Assert.assertEquals(1, objects.size());
        Object o = objects.get(0);
        Assert.assertNotNull(o);
        Assert.assertTrue(o.getClass().isAssignableFrom(AccountDTO.class));
        AccountDTO fetchedAccount = (AccountDTO) o;
        Assert.assertNotNull(fetchedAccount);
        Assert.assertEquals(account.getLogin(), fetchedAccount.getLogin());
    }


    @Test
    public void addMemberTest() throws EntityNotFoundException, AccessRightException {
        Account accountToAdd = new Account("batman");
        Mockito.when(organizationManager.getMyOrganization()).thenReturn(organization);
        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(accountToAdd.getLogin());
        Mockito.when(accountManager.getAccount(userDTO.getLogin())).thenReturn(accountToAdd);
        Mockito.doNothing().when(organizationManager).updateOrganization(organization);
        Response response = organizationResource.addMember(userDTO);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void removeMemberTest() throws EntityNotFoundException, AccessRightException {
        Account accountToRemove = new Account("batman");
        Mockito.when(organizationManager.getMyOrganization()).thenReturn(organization);
        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(accountToRemove.getLogin());
        Mockito.when(accountManager.getAccount(userDTO.getLogin())).thenReturn(accountToRemove);
        Mockito.doNothing().when(organizationManager).updateOrganization(organization);
        Response response = organizationResource.removeMember(userDTO);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void moveMemberTest() throws EntityNotFoundException, AccessRightException {

        Organization org = new Organization();
        Account account1 = new Account("batman");
        Account account2 = new Account("robin");

        org.addMember(account1);
        org.addMember(account2);

        UserDTO user1 = new UserDTO();
        user1.setLogin(account1.getLogin());

        UserDTO user2 = new UserDTO();
        user2.setLogin(account2.getLogin());

        Mockito.when(organizationManager.getMyOrganization()).thenReturn(org);
        Mockito.when(accountManager.getAccount(user2.getLogin())).thenReturn(account2);
        Mockito.when(accountManager.getAccount(user1.getLogin())).thenReturn(account1);
        Mockito.doNothing().when(organizationManager).updateOrganization(organization);

        Response response = organizationResource.moveMember(user2, "up");
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        Assert.assertEquals(org.getMembers().get(0), account2);
        Assert.assertEquals(org.getMembers().get(1), account1);

        response = organizationResource.moveMember(user2, "down");
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        Assert.assertEquals(org.getMembers().get(0), account1);
        Assert.assertEquals(org.getMembers().get(1), account2);

        response = organizationResource.moveMember(user2, "up side down !");
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());


    }


}
