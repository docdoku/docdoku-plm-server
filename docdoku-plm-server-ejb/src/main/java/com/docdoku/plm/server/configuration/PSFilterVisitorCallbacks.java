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

package com.docdoku.plm.server.configuration;

import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartLink;
import com.docdoku.plm.server.core.product.PartMaster;

import java.util.List;

public interface PSFilterVisitorCallbacks {
    default void onIndeterminateVersion(PartMaster partMaster, List<PartIteration> partIterations) throws NotAllowedException{
        // Default void implementation
    }
    default  void onUnresolvedVersion(PartMaster partMaster) throws NotAllowedException{
        // Default void implementation
    }
    default void onIndeterminatePath(List<PartLink> pCurrentPath, List<PartIteration> pCurrentPathPartIterations) throws NotAllowedException {
        // Default void implementation
    }
    default void onUnresolvedPath(List<PartLink> pCurrentPath, List<PartIteration> partIterations) throws NotAllowedException {
        // Default void implementation
    }
    default void onBranchDiscovered(List<PartLink> pCurrentPath, List<PartIteration> copyPartIteration) {
        // Default void implementation
    }
    default void onOptionalPath(List<PartLink> path, List<PartIteration> partIterations) {
        // Default void implementation
    }
    default boolean onPathWalk(List<PartLink> path, List<PartMaster> parts) {
        // Default void implementation
        return true;
    }
}
