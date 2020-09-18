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


package com.docdoku.plm.server.configuration.filter;

import com.docdoku.plm.server.core.configuration.ProductStructureFilter;
import com.docdoku.plm.server.core.product.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Morgan Guimard
 *
 * Check for cyclic assembly after part iteration update: must check on the wip and on the latest.
 * We also need to walk every substitute branches.
 *
 */
public class UpdatePartIterationPSFilter implements ProductStructureFilter, Serializable {

    private PartMasterKey rootKey;
    private PartIteration partIteration;

    public UpdatePartIterationPSFilter(PartIteration partIteration) {
        this.partIteration = partIteration;
        rootKey = partIteration.getKey().getPartRevision().getPartMaster();
    }

    @Override
    public List<PartIteration> filter(PartMaster part) {

        // Return wip on updated part iteration
        if(part.getKey().equals(rootKey)){
            return Collections.singletonList(partIteration);
        }

        // Return wip and last
        List<PartIteration> partIterations = new ArrayList<>();
        PartRevision partRevision = part.getLastRevision();
        PartIteration lastIteration = partRevision.getLastIteration();
        PartIteration lastCheckedInIteration = partRevision.getLastCheckedInIteration();

        if(partRevision.isCheckedOut() && lastCheckedInIteration != null){
            partIterations.add(lastCheckedInIteration);
        }

        partIterations.add(lastIteration);
        return partIterations;
    }

    @Override
    public List<PartLink> filter(List<PartLink> path) {

        List<PartLink> links = new ArrayList<>();
        PartLink link = path.get(path.size()-1);
        links.add(link);

        for(PartSubstituteLink substituteLink: link.getSubstitutes()){
            links.add(substituteLink);
        }

        return links;
    }

}
