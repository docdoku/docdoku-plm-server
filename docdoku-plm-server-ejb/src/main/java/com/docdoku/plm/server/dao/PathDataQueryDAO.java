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

package com.docdoku.plm.server.dao;

import com.docdoku.plm.server.core.configuration.PathDataIteration;
import com.docdoku.plm.server.core.configuration.PathDataMaster;
import com.docdoku.plm.server.core.configuration.ProductInstanceIteration;
import com.docdoku.plm.server.core.meta.*;
import com.docdoku.plm.server.core.product.InstancePartNumberAttribute;
import com.docdoku.plm.server.core.query.Query;
import com.docdoku.plm.server.core.query.QueryRule;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morgan Guimard on 09/04/15.
 */

@RequestScoped
public class PathDataQueryDAO {

    private static final String STRING = "string";
    private static final String INSTANCE_ATTRIBUTES = "instanceAttributes";

    @Inject
    private EntityManager em;

    private CriteriaBuilder cb;

    private CriteriaQuery<PathDataMaster> cq;
    private Root<PathDataMaster> pdm;
    private Root<PathDataIteration> pdi;

    private Root<InstanceURLAttribute> iua;
    private Root<InstanceBooleanAttribute> iba;
    private Root<InstanceNumberAttribute> ina;
    private Root<InstanceListOfValuesAttribute> ila;
    private Root<InstanceDateAttribute> ida;
    private Root<InstanceTextAttribute> ita;
    private Root<InstanceLongTextAttribute> ilta;
    private Root<InstancePartNumberAttribute> ipna;

    @PostConstruct
    private void setup() {
        cb = em.getCriteriaBuilder();
        cq = cb.createQuery(PathDataMaster.class);

        pdm = cq.from(PathDataMaster.class);
        pdi = cq.from(PathDataIteration.class);

        iua = cq.from(InstanceURLAttribute.class);
        iba = cq.from(InstanceBooleanAttribute.class);
        ina = cq.from(InstanceNumberAttribute.class);
        ila = cq.from(InstanceListOfValuesAttribute.class);
        ida = cq.from(InstanceDateAttribute.class);
        ilta = cq.from(InstanceLongTextAttribute.class);
        ipna = cq.from(InstancePartNumberAttribute.class);
        ita = cq.from(InstanceTextAttribute.class);
    }

    public List<String> runQuery(String pTimeZone, ProductInstanceIteration productInstanceIteration, Query query) {

        cq.select(pdm);

        List<PathDataMaster> pathDataMasterList = productInstanceIteration.getPathDataMasterList();

        // If no path data available, don't even try to run a query
        if (pathDataMasterList.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> pathIds =
                pathDataMasterList.stream()
                        .map(PathDataMaster::getId)
                        .collect(Collectors.toSet());

        Predicate pathFilter = cb.and(pdm.get("id").in(pathIds), cb.equal(pdi.get("pathDataMaster"), pdm));

        Predicate rulesPredicate = getPredicate(pTimeZone, query.getPathDataQueryRule());

        cq.where(cb.and(pathFilter, rulesPredicate));

        TypedQuery<PathDataMaster> tp = em.createQuery(cq);

        Set<String> pathList = tp.getResultList().stream()
                .map(PathDataMaster::getPath)
                .collect(Collectors.toSet());

        return new ArrayList<>(pathList);
    }

    private Predicate getPredicate(String pTimeZone, QueryRule queryRule) {

        String condition = queryRule.getCondition();

        List<QueryRule> subQueryRules = queryRule.getSubQueryRules();

        if (subQueryRules != null && !subQueryRules.isEmpty()) {

            Predicate[] predicates = new Predicate[subQueryRules.size()];

            for (int i = 0; i < predicates.length; i++) {
                Predicate predicate = getPredicate(pTimeZone, subQueryRules.get(i));
                predicates[i] = predicate;
            }

            if ("OR".equals(condition)) {
                return cb.or(predicates);
            } else if ("AND".equals(condition)) {
                return cb.and(predicates);
            }

            throw new IllegalArgumentException("Cannot parse rule or sub rule condition: " + condition + " ");

        } else {
            return getRulePredicate(pTimeZone, queryRule);
        }
    }

    private Predicate getRulePredicate(String pTimeZone, QueryRule queryRule) {

        String field = queryRule.getField();

        if (field == null) {
            return cb.and();
        }

        String operator = queryRule.getOperator();
        List<String> values = queryRule.getValues();

        if (field.startsWith("pd-attr-TEXT.")) {
            return getInstanceTextAttributePredicate(pTimeZone, field.substring(13), operator, values);
        }

        if (field.startsWith("pd-attr-LONG_TEXT.")) {
            return getInstanceLongTextAttributePredicate(pTimeZone, field.substring(18), operator, values);
        }

        if (field.startsWith("pd-attr-DATE.")) {
            return getInstanceDateAttributePredicate(pTimeZone, field.substring(13), operator, values);
        }

        if (field.startsWith("pd-attr-BOOLEAN.")) {
            return getInstanceBooleanAttributePredicate(field.substring(16), operator, values);
        }

        if (field.startsWith("pd-attr-URL.")) {
            return getInstanceURLAttributePredicate(pTimeZone, field.substring(12), operator, values);
        }

        if (field.startsWith("pd-attr-NUMBER.")) {
            return getInstanceNumberAttributePredicate(pTimeZone, field.substring(15), operator, values);
        }

        if (field.startsWith("pd-attr-LOV.")) {
            return getInstanceLovAttributePredicate(field.substring(12), operator, values);
        }

        if (field.startsWith("pd-attr-PART_NUMBER.")) {
            return getInstancePartNumberAttributePredicate(pTimeZone, field.substring(20), operator, values);
        }

        throw new IllegalArgumentException("Unhandled attribute: [" + field + ", " + operator + ", " + values + "]");
    }


    // Instances Attributes
    private Predicate getInstanceURLAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, iua.get("urlValue"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = iua.in(pdi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(iua.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceBooleanAttributePredicate(String field, String operator, List<String> values) {
        if (values.size() == 1) {
            Predicate valuesPredicate = cb.equal(iba.get("booleanValue"), Boolean.parseBoolean(values.get(0)));
            Predicate memberPredicate = iba.in(pdi.get(INSTANCE_ATTRIBUTES));
            switch (operator) {
                case "equal":
                    return cb.and(cb.equal(iba.get("name"), field), valuesPredicate, memberPredicate);
                case "not_equal":
                    return cb.and(cb.equal(iba.get("name"), field), valuesPredicate.not(), memberPredicate);
                default:
                    break;
            }
        }

        throw new IllegalArgumentException("Cannot handle such operator [" + operator + "] on field " + field + "]");
    }

    private Predicate getInstanceNumberAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ina.get("numberValue"), operator, values, "double", pTimeZone);
        Predicate memberPredicate = ina.in(pdi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ina.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceLovAttributePredicate(String field, String operator, List<String> values) {
        if (values.size() == 1) {
            Predicate valuesPredicate = cb.equal(ila.get("indexValue"), Integer.parseInt(values.get(0)));
            Predicate memberPredicate = ila.in(pdi.get(INSTANCE_ATTRIBUTES));
            switch (operator) {
                case "equal":
                    return cb.and(cb.equal(ila.get("name"), field), valuesPredicate, memberPredicate);
                case "not_equal":
                    return cb.and(cb.equal(ila.get("name"), field), valuesPredicate.not(), memberPredicate);
                default:
                    break;
            }
        }

        throw new IllegalArgumentException("Cannot handle such operator [" + operator + "] on field " + field + "]");
    }

    private Predicate getInstanceDateAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ida.get("dateValue"), operator, values, "date", pTimeZone);
        Predicate memberPredicate = ida.in(pdi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ida.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceLongTextAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ilta.get("longTextValue"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = ilta.in(pdi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ilta.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstancePartNumberAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ipna.get("partMasterValue").get("number"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = ipna.in(pdi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ipna.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceTextAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ita.get("textValue"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = ita.in(pdi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ita.get("name"), field), valuesPredicate, memberPredicate);
    }
}
