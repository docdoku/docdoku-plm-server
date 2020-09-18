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

package com.docdoku.plm.server.core.services;

import com.docdoku.plm.server.core.common.JWTokenUserGroupMapping;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.sharing.SharedEntity;

import javax.servlet.http.HttpServletResponse;
import java.security.Key;

/**
 * @author Morgan Guimard
 */
public interface ITokenManagerLocal {
    String createAuthToken(Key key, UserGroupMapping userGroupMapping);

    String createSharedEntityToken(Key key, SharedEntity sharedEntity) ;

    JWTokenUserGroupMapping validateAuthToken(Key key, String jwt);

    String validateSharedResourceToken(Key key, String jwt) ;

    boolean isJWTValidBefore(Key key, int seconds, String authorizationString) ;

    void refreshTokenIfNeeded(Key key, HttpServletResponse response, JWTokenUserGroupMapping jwTokenUserGroupMapping) ;

    String createEntityToken(Key key, String entityKey);

    String validateEntityToken(Key key, String jwt) ;
}
