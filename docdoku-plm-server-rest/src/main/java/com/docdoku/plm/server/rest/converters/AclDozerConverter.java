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

package com.docdoku.plm.server.rest.converters;

import org.dozer.DozerConverter;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.UserGroup;
import com.docdoku.plm.server.core.security.ACL;
import com.docdoku.plm.server.core.security.ACLUserEntry;
import com.docdoku.plm.server.core.security.ACLUserGroupEntry;
import com.docdoku.plm.server.rest.dto.ACLDTO;

import java.util.Map;


public class AclDozerConverter extends DozerConverter<ACL, ACLDTO> {


    public AclDozerConverter() {
        super(ACL.class, ACLDTO.class);
    }

    @Override
    public ACLDTO convertTo(ACL acl, ACLDTO pAclDTO) {

        ACLDTO aclDTO = new ACLDTO();

        if (acl != null) {

            for (Map.Entry<User, ACLUserEntry> entry : acl.getUserEntries().entrySet()) {
                ACLUserEntry aclEntry = entry.getValue();
                aclDTO.addUserEntry(aclEntry.getPrincipalLogin(), aclEntry.getPermission());
            }

            for (Map.Entry<UserGroup, ACLUserGroupEntry> entry : acl.getGroupEntries().entrySet()) {
                ACLUserGroupEntry aclEntry = entry.getValue();
                aclDTO.addGroupEntry(aclEntry.getPrincipalId(), aclEntry.getPermission());
            }
            return aclDTO;
        }

        return null;
    }

    @Override
    public ACL convertFrom(ACLDTO aclDTO, ACL acl) {
        return acl;
    }

}
