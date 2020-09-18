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

import com.docdoku.plm.server.core.configuration.ProductBaselineType;
import com.docdoku.plm.server.core.configuration.ProductConfigSpec;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.util.Tools;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morgan Guimard
 */
public class ProductBaselineCreationConfigSpec extends ProductConfigSpec {

    private List<PartIteration> partIterations;
    private List<String> substituteLinks;
    private List<String> optionalUsageLinks;
    private ProductBaselineType type;


    public ProductBaselineCreationConfigSpec(ProductBaselineType type, List<PartIteration> partIterations, List<String> substituteLinks, List<String> optionalUsageLinks) {
        this.partIterations = partIterations;
        this.substituteLinks = substituteLinks;
        this.optionalUsageLinks = optionalUsageLinks;
        this.type = type;
    }

    @Override
    public PartIteration filterPartIteration(PartMaster partMaster) {

        if (type.equals(ProductBaselineType.RELEASED)) {

            for (PartIteration pi : partIterations) {
                if (pi.getPartRevision().getPartMaster().getKey().equals(partMaster.getKey())) {
                    retainedPartIterations.add(pi);
                    return pi;
                }
            }
            // Else, take the latest released
            PartRevision lastReleasedRevision = partMaster.getLastReleasedRevision();
            if (lastReleasedRevision != null) {
                PartIteration pi = lastReleasedRevision.getLastIteration();
                retainedPartIterations.add(pi);
                return pi;
            }

        } else if (type.equals(ProductBaselineType.LATEST)) {

            PartIteration pi = partMaster.getLastRevision().getLastCheckedInIteration();

            if (pi != null) {
                retainedPartIterations.add(pi);
                return pi;
            }
        }

        return null;
    }

    @Override
    public PartLink filterPartLink(List<PartLink> path) {

        // No ambiguities here, must return 1 value
        // Check if optional or substitute, nominal link else

        PartLink nominalLink = path.get(path.size() - 1);

        String pathAsString = Tools.getPathAsString(path);

        if (nominalLink.isOptional() && optionalUsageLinks.contains(pathAsString)) {
            retainedOptionalUsageLinks.add(pathAsString);
        }

        for (PartSubstituteLink substituteLink : nominalLink.getSubstitutes()) {

            List<PartLink> substitutePath = new ArrayList<>(path);
            substitutePath.set(substitutePath.size() - 1, substituteLink);

            String substitutePathAsString = Tools.getPathAsString(substitutePath);
            if (substituteLinks.contains(substitutePathAsString)) {
                retainedSubstituteLinks.add(substitutePathAsString);
                return substituteLink;
            }

        }

        return nominalLink;
    }

}
