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

import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.configuration.ProductStructureFilter;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartLink;
import com.docdoku.plm.server.core.product.PartMaster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link com.docdoku.plm.server.core.configuration.ProductStructureFilter} implementation
 * which selects the latest iteration (checked in or not).
 *
 * @author Morgan Guimard
 */
public class WIPPSFilter implements ProductStructureFilter, Serializable {

    private User user;
    private boolean diverge = false;

    public WIPPSFilter(User user) {
        this.user = user;
    }

    public WIPPSFilter(User user, boolean diverge) {
        this.user = user;
        this.diverge = diverge;
    }

    @Override
    public List<PartIteration> filter(PartMaster part) {
        List<PartIteration> partIterations = new ArrayList<>();
        PartIteration partIteration = getLastAccessibleIteration(part);
        if(partIteration != null) {
            partIterations.add(partIteration);
        }
        return partIterations;
    }

    @Override
    public List<PartLink> filter(List<PartLink> path) {

        List<PartLink> links = new ArrayList<>();

        PartLink link = path.get(path.size()-1);
        links.add(link);

        if(diverge){
            links.addAll(link.getSubstitutes().stream().collect(Collectors.toList()));
        }

        return links;
    }

    private PartIteration getLastAccessibleIteration(PartMaster partMaster) {
        PartIteration iteration = null;
        for(int i = partMaster.getPartRevisions().size()-1; i >= 0 && iteration == null; i--) {
            iteration = partMaster.getPartRevisions().get(i).getLastAccessibleIteration(user);
        }
        return iteration;
    }


}
