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


package com.docdoku.plm.server.core.configuration;

import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartLink;
import com.docdoku.plm.server.core.product.PartMaster;

import java.io.Serializable;
import java.util.*;

/**
 * A ConfigSpec is used to select for each {@link PartMaster}s
 * the right {@link PartIteration} according to specific rules.
 *
 * It also selects from a complete {@link PartLink} path the one
 * which has to be considered (itself or a variant).
 *
 * ProductConfigSpec is a restrictive type of {@link ProductStructureFilter}.
 *
 * @author Florent Garin
 * @version 1.1, 30/10/11
 * @since V1.1
 */
public abstract class ProductConfigSpec implements ProductStructureFilter, Serializable{

    protected Set<PartIteration> retainedPartIterations = new HashSet<>();
    protected Set<String> retainedSubstituteLinks = new HashSet<>();
    protected Set<String> retainedOptionalUsageLinks = new HashSet<>();

    public ProductConfigSpec() {
    }

    // Config specs are strict and returns a single value
    // Do not override them
    public final List<PartLink> filter(List<PartLink> path) {
        PartLink partLink = filterPartLink(path);
        return partLink != null ? Collections.singletonList(partLink) : new ArrayList<>();
    }

    public final List<PartIteration> filter(PartMaster partMaster) {
        PartIteration partIteration = filterPartIteration(partMaster);
        return partIteration != null ? Collections.singletonList(partIteration) : new ArrayList<>();
    }

    // All config specs must implement a strict filter
    public abstract PartIteration filterPartIteration(PartMaster partMaster);
    public abstract PartLink filterPartLink(List<PartLink> path);

    public Set<PartIteration> getRetainedPartIterations() {
        return retainedPartIterations;
    }

    public Set<String> getRetainedSubstituteLinks() {
        return retainedSubstituteLinks;
    }

    public Set<String> getRetainedOptionalUsageLinks() {
        return retainedOptionalUsageLinks;
    }
}
