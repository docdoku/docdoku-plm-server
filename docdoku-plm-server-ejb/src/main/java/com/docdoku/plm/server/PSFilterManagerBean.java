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

import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.configuration.*;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.product.ConfigurationItemKey;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.IPSFilterManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.configuration.filter.LatestCheckedInPSFilter;
import com.docdoku.plm.server.configuration.filter.LatestReleasedPSFilter;
import com.docdoku.plm.server.configuration.filter.ReleasedPSFilter;
import com.docdoku.plm.server.configuration.filter.WIPPSFilter;
import com.docdoku.plm.server.configuration.spec.ResolvedCollectionConfigSpec;
import com.docdoku.plm.server.dao.ProductBaselineDAO;
import com.docdoku.plm.server.dao.ProductInstanceMasterDAO;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.ADMIN_ROLE_ID})
@Local(IPSFilterManagerLocal.class)
@Stateless(name = "PSFilterManagerBean")
public class PSFilterManagerBean implements IPSFilterManagerLocal {

    @Inject
    private ProductBaselineDAO productBaselineDAO;

    @Inject
    private ProductInstanceMasterDAO productInstanceMasterDAO;

    @Inject
    private IUserManagerLocal userManager;

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ProductStructureFilter getBaselinePSFilter(int baselineId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, BaselineNotFoundException, WorkspaceNotEnabledException {
        ProductBaseline productBaseline = productBaselineDAO.loadBaseline(baselineId);
        userManager.checkWorkspaceReadAccess(productBaseline.getConfigurationItem().getWorkspaceId());
        return new ResolvedCollectionConfigSpec(productBaseline);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ProductStructureFilter getProductInstanceConfigSpec(ConfigurationItemKey ciKey, String serialNumber) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ProductInstanceMasterNotFoundException, WorkspaceNotEnabledException {
        userManager.checkWorkspaceReadAccess(ciKey.getWorkspace());
        ProductInstanceMasterKey productInstanceMasterKey = new ProductInstanceMasterKey(serialNumber, ciKey);
        ProductInstanceMaster productIM = productInstanceMasterDAO.loadProductInstanceMaster(productInstanceMasterKey);
        ProductInstanceIteration productII = productIM.getLastIteration();
        return new ResolvedCollectionConfigSpec(productII);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public ProductStructureFilter getPSFilter(ConfigurationItemKey ciKey, String filterType, boolean diverge) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, ProductInstanceMasterNotFoundException, BaselineNotFoundException, WorkspaceNotEnabledException {

        User user = userManager.checkWorkspaceReadAccess(ciKey.getWorkspace());

        if (filterType == null) {
            return new WIPPSFilter(user);
        }

        ProductStructureFilter filter;

        switch (filterType) {

            case "wip":
            case "undefined":
                filter = new WIPPSFilter(user, diverge);
                break;
            case "latest":
                filter = new LatestCheckedInPSFilter(diverge);
                break;
            case "released":
                filter = new ReleasedPSFilter(diverge);
                break;
            case "latest-released":
                filter = new LatestReleasedPSFilter(diverge);
                break;
            default:
                if (filterType.startsWith("pi-")) {
                    String serialNumber = filterType.substring(3);
                    filter = getProductInstanceConfigSpec(ciKey, serialNumber);
                } else {
                    filter = getBaselinePSFilter(Integer.parseInt(filterType));
                }
                break;
        }
        return filter;
    }

}
