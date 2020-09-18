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

import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.meta.*;
import com.docdoku.plm.server.core.product.InstancePartNumberAttribute;
import com.docdoku.plm.server.core.product.PartIteration;
import com.docdoku.plm.server.core.product.PartRevision;
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
public class PartRevisionQueryDAO {

    public static final String STRING = "string";
    public static final String INSTANCE_ATTRIBUTES = "instanceAttributes";

    @Inject
    private EntityManager em;

    private CriteriaBuilder cb;
    private CriteriaQuery<PartRevision> cq;

    private Root<PartRevision> pr;
    private Root<PartIteration> pi;
    private Root<Tag> tag;
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

        cq = cb.createQuery(PartRevision.class);
        pr = cq.from(PartRevision.class);
        pi = cq.from(PartIteration.class);
        tag = cq.from(Tag.class);

        iua = cq.from(InstanceURLAttribute.class);
        iba = cq.from(InstanceBooleanAttribute.class);
        ina = cq.from(InstanceNumberAttribute.class);
        ila = cq.from(InstanceListOfValuesAttribute.class);
        ida = cq.from(InstanceDateAttribute.class);
        ilta = cq.from(InstanceLongTextAttribute.class);
        ipna = cq.from(InstancePartNumberAttribute.class);
        ita = cq.from(InstanceTextAttribute.class);
    }

    public List<PartRevision> runQuery(String pTimeZone, Workspace workspace, Query query) {

        cq.select(pr);

        Predicate prJoinPredicate = cb.and(
                cb.equal(pi.get("partRevision"), pr),
                cb.equal(pr.get("partMaster").get("workspace"), workspace)
        );

        Predicate rulesPredicate = getPredicate(pTimeZone, query.getQueryRule());

        cq.where(cb.and(
                prJoinPredicate,
                rulesPredicate
        ));

        TypedQuery<PartRevision> tp = em.createQuery(cq);

        Set<PartRevision> revisions = tp.getResultList().stream()
                .filter(part -> part.getLastCheckedInIteration() != null)
                .collect(Collectors.toSet());

        return new ArrayList<>(revisions);
    }

    private Predicate getPredicate(String pTimeZone, QueryRule queryRule) {

        if (queryRule == null) {
            return cb.and();
        }

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
        String type = queryRule.getType();

        if (field.startsWith("pm.")) {
            return getPartMasterPredicate(pTimeZone, field.substring(3), operator, values, type);
        }

        if (field.startsWith("pr.")) {
            return getPartRevisionPredicate(pTimeZone, field.substring(3), operator, values, type);
        }

        if (field.startsWith("author.")) {
            return getAuthorPredicate(pTimeZone, field.substring(7), operator, values, type);
        }

        if (field.startsWith("attr-TEXT.")) {
            return getInstanceTextAttributePredicate(pTimeZone, field.substring(10), operator, values);
        }

        if (field.startsWith("attr-LONG_TEXT.")) {
            return getInstanceLongTextAttributePredicate(pTimeZone, field.substring(15), operator, values);
        }

        if (field.startsWith("attr-DATE.")) {
            return getInstanceDateAttributePredicate(pTimeZone, field.substring(10), operator, values);
        }

        if (field.startsWith("attr-BOOLEAN.")) {
            return getInstanceBooleanAttributePredicate(field.substring(13), operator, values);
        }

        if (field.startsWith("attr-URL.")) {
            return getInstanceURLAttributePredicate(pTimeZone, field.substring(9), operator, values);
        }

        if (field.startsWith("attr-NUMBER.")) {
            return getInstanceNumberAttributePredicate(pTimeZone, field.substring(12), operator, values);
        }

        if (field.startsWith("attr-LOV.")) {
            return getInstanceLovAttributePredicate(field.substring(9), operator, values);
        }

        if (field.startsWith("attr-PART_NUMBER.")) {
            return getInstancePartNumberAttributePredicate(pTimeZone, field.substring(17), operator, values);
        }

        throw new IllegalArgumentException("Unhandled attribute: [" + field + ", " + operator + ", " + values + "]");
    }

    private Predicate getAuthorPredicate(String pTimeZone, String field, String operator, List<String> values, String type) {
        return QueryPredicateBuilder.getExpressionPredicate(cb, pr.get("author").get("account").get(field), operator, values, type, pTimeZone);
    }

    private Predicate getPartRevisionPredicate(String pTimeZone, String field, String operator, List<String> values, String type) {
        if ("checkInDate".equals(field)) {
            Predicate lastIterationPredicate = cb.equal(cb.size(pr.get("partIterations")), pi.get("iteration"));
            return cb.and(lastIterationPredicate, QueryPredicateBuilder.getExpressionPredicate(cb, pi.get("checkInDate"), operator, values, type, pTimeZone));
        }
        else if ("modificationDate".equals(field)) {
            Predicate lastIterationPredicate = cb.equal(cb.size(pr.get("partIterations")), pi.get("iteration"));
            return cb.and(lastIterationPredicate, QueryPredicateBuilder.getExpressionPredicate(cb, pi.get("modificationDate"), operator, values, type, pTimeZone));
        } else if ("status".equals(field)) {
            if (values.size() == 1) {
                return QueryPredicateBuilder.getExpressionPredicate(cb, pr.get(field), operator, values, "status", pTimeZone);
            }
        } else if ("tags".equals(field)) {
            return getTagsPredicate(values);
        } else if ("linkedDocuments".equals(field)) {
            // should be ignored, returning always true for the moment
            return cb.and();
        }
        return QueryPredicateBuilder.getExpressionPredicate(cb, pr.get(field), operator, values, type, pTimeZone);
    }

    private Predicate getTagsPredicate(List<String> values) {
        Predicate prPredicate = tag.in(pr.get("tags"));
        Predicate valuesPredicate = cb.equal(tag.get("label"), values);
        return cb.and(prPredicate, valuesPredicate);
    }

    private Predicate getPartMasterPredicate(String pTimeZone, String field, String operator, List<String> values, String type) {
        return QueryPredicateBuilder.getExpressionPredicate(cb, pr.get("partMaster").get(field), operator, values, type, pTimeZone);
    }

    // Instances Attributes
    private Predicate getInstanceURLAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {

        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, iua.get("urlValue"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = iua.in(pi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(iua.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceBooleanAttributePredicate(String field, String operator, List<String> values) {
        if (values.size() == 1) {
            Predicate valuesPredicate = cb.equal(iba.get("booleanValue"), Boolean.parseBoolean(values.get(0)));
            Predicate memberPredicate = iba.in(pi.get(INSTANCE_ATTRIBUTES));
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
        Predicate memberPredicate = ina.in(pi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ina.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceLovAttributePredicate(String field, String operator, List<String> values) {
        if (values.size() == 1) {
            Predicate valuesPredicate = cb.equal(ila.get("indexValue"), Integer.parseInt(values.get(0)));
            Predicate memberPredicate = ila.in(pi.get(INSTANCE_ATTRIBUTES));
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
        Predicate memberPredicate = ida.in(pi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ida.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceLongTextAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ilta.get("longTextValue"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = ilta.in(pi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ita.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstancePartNumberAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ipna.get("partMasterValue").get("number"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = ipna.in(pi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ita.get("name"), field), valuesPredicate, memberPredicate);
    }

    private Predicate getInstanceTextAttributePredicate(String pTimeZone, String field, String operator, List<String> values) {
        Predicate valuesPredicate = QueryPredicateBuilder.getExpressionPredicate(cb, ita.get("textValue"), operator, values, STRING, pTimeZone);
        Predicate memberPredicate = ita.in(pi.get(INSTANCE_ATTRIBUTES));
        return cb.and(cb.equal(ita.get("name"), field), valuesPredicate, memberPredicate);
    }
}


