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
package com.docdoku.plm.server.factory;

import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserGroup;
import com.docdoku.plm.server.core.common.UserGroupKey;
import com.docdoku.plm.server.core.common.UserKey;
import com.docdoku.plm.server.core.security.ACL;
import com.docdoku.plm.server.core.security.ACLPermission;
import com.docdoku.plm.server.dao.ACLDAO;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Asmae CHADID on 26/02/15.
 */
@Stateless(name = "ACLFactory")
public class ACLFactory {

    @Inject
    private EntityManager em;

    @Inject
    private ACLDAO aclDAO;

    @Inject
    private ACLFactory aclFactory;

    public ACLFactory() {

    }

    public ACL createACL(String pWorkspaceId, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) {
        ACL acl = new ACL();
        if (pUserEntries != null) {
            for (Map.Entry<String, String> entry : pUserEntries.entrySet()) {
                acl.addEntry(em.find(User.class, new UserKey(pWorkspaceId, entry.getKey())),
                        ACLPermission.valueOf(entry.getValue()));
            }
        }
        if (pGroupEntries != null) {
            for (Map.Entry<String, String> entry : pGroupEntries.entrySet()) {
                acl.addEntry(em.find(UserGroup.class, new UserGroupKey(pWorkspaceId, entry.getKey())),
                        ACLPermission.valueOf(entry.getValue()));
            }
        }
        aclDAO.createACL(acl);
        return acl;
    }

    public ACL updateACL(String workspaceId, ACL acl, Map<String, String> pUserEntries, Map<String, String> pGroupEntries) {

        if (acl != null) {
            aclDAO.removeACLEntries(acl);
            acl.setUserEntries(new HashMap<>());
            acl.setGroupEntries(new HashMap<>());
            for (Map.Entry<String, String> entry : pUserEntries.entrySet()) {
                acl.addEntry(em.getReference(User.class, new UserKey(workspaceId, entry.getKey())), ACLPermission.valueOf(entry.getValue()));
            }

            for (Map.Entry<String, String> entry : pGroupEntries.entrySet()) {
                acl.addEntry(em.getReference(UserGroup.class, new UserGroupKey(workspaceId, entry.getKey())), ACLPermission.valueOf(entry.getValue()));
            }
        }
        return acl;
    }
}
