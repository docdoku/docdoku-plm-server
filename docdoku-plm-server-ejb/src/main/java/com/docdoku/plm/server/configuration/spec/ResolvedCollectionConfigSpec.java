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

package com.docdoku.plm.server.configuration.spec;

import com.docdoku.plm.server.core.configuration.*;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartLink;
import com.docdoku.plm.server.core.product.PartMaster;
import com.docdoku.plm.server.core.product.PartSubstituteLink;
import com.docdoku.plm.server.core.util.Tools;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Morgan Guimard
 */
public class ResolvedCollectionConfigSpec extends ProductConfigSpec {

    private PartCollection partCollection;
    private Set<String> optionalUsageLinks;
    private Set<String> substitutesUsageLinks;

    public ResolvedCollectionConfigSpec(@NotNull ResolvedCollection resolvedCollection) {
        this.partCollection = resolvedCollection.getPartCollection();
        this.optionalUsageLinks= resolvedCollection.getOptionalUsageLinks();
        this.substitutesUsageLinks = resolvedCollection.getSubstituteLinks();
    }

    @Override
    public PartIteration filterPartIteration(PartMaster part) {
        if(partCollection != null) {
            BaselinedPartKey baselinedRootPartKey = new BaselinedPartKey(partCollection.getId(), part.getWorkspaceId(), part.getNumber());
            BaselinedPart baselinedRootPart = partCollection.getBaselinedPart(baselinedRootPartKey);
            if (baselinedRootPart != null) {
                return baselinedRootPart.getTargetPart();
            }
        }
        return null;
    }

    @Override
    public PartLink filterPartLink(List<PartLink> path) {
        // No ambiguities here, must return 1 value
        // Check if optional or substitute, nominal link else
        PartLink nominalLink = path.get(path.size()-1);

        if(nominalLink.isOptional() && !optionalUsageLinks.contains(Tools.getPathAsString(path))){
            return null;
        }

        for(PartSubstituteLink substituteLink:nominalLink.getSubstitutes()){
            List<PartLink> substitutePath = new ArrayList<>(path);
            substitutePath.set(substitutePath.size()-1,substituteLink);

            if(substitutesUsageLinks.contains(Tools.getPathAsString(substitutePath))){
                return substituteLink;
            }

        }

        return nominalLink;

    }

}
