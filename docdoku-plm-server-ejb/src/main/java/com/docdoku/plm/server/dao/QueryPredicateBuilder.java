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

import com.docdoku.plm.server.core.meta.RevisionStatus;
import com.docdoku.plm.server.core.util.DateUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class QueryPredicateBuilder {

    public static final String AND_OPERATOR = " and operator ";
    public static final String DOUBLE = "double";

    private QueryPredicateBuilder() {}

    // Rule parsing
    // Safe casts for expressions
    @SuppressWarnings("unchecked")
    public static Predicate getExpressionPredicate(CriteriaBuilder cb, Expression fieldExp, String operator, List<String> values, String type, String timeZone) {

        List<?> operands;

        switch (type) {
            case "string":
                operands = values;
                break;
            case "date":
                try {
                    List<Date> temp = new ArrayList<>();
                    for (String string : values) {
                        temp.add(DateUtils.parse(string, timeZone));
                    }
                    operands = temp;
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Parsing exception for dates " + values + AND_OPERATOR + operator);
                }
                break;
            case DOUBLE:
                try {
                    List<Double> operandValues = new ArrayList<>();
                    for(String value:values){
                        operandValues.add(Double.parseDouble(value));
                    }
                    operands = operandValues;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Parsing exception for double " + values + AND_OPERATOR + operator);
                }
                break;
            case "status":
                List<RevisionStatus> operandValues = new ArrayList<>();
                for(String value:values){
                    operandValues.add(RevisionStatus.valueOf(value));
                }
                operands = operandValues;
                break;
            default:
                operands = values;
                break;
        }

        switch (operator) {
            case "between":
                if (operands.size() == 2) {
                    if ("date".equals(type)) {
                        return cb.between(fieldExp, (Date) operands.get(0), (Date) operands.get(1));

                    } else if (DOUBLE.equals(type)) {
                        return cb.between(fieldExp, (Double) operands.get(0), (Double) operands.get(1));
                    }
                }
                break;
            case "equal":
                if ("date".equals(type)) {
                    Date date1 = (Date) operands.get(0);
                    Calendar c = Calendar.getInstance();
                    c.setTime(date1);
                    c.add(Calendar.DATE, 1);
                    Date date2 = c.getTime();

                    return cb.between(fieldExp, date1, date2);

                } else {
                    return cb.equal(fieldExp, operands.get(0));
                }
            case "not_equal":
                return cb.equal(fieldExp, operands.get(0)).not();

            case "contains":
                return cb.like(fieldExp, "%" + operands.get(0) + "%");
            case "not_contains":
                return cb.like(fieldExp, "%" + operands.get(0) + "%").not();

            case "begins_with":
                return cb.like(fieldExp, operands.get(0) + "%");
            case "not_begins_with":
                return cb.like(fieldExp, operands.get(0) + "%").not();

            case "ends_with":
                return cb.like(fieldExp, "%" + operands.get(0));
            case "not_ends_with":
                return cb.like(fieldExp, "%" + operands.get(0)).not();

            case "less":
                if ("date".equals(type)) {
                    return cb.lessThan(fieldExp, (Date) operands.get(0));
                } else if (DOUBLE.equals(type)) {
                    return cb.lessThan(fieldExp, (Double) operands.get(0));
                }
                break;
            case "less_or_equal":
                if ("date".equals(type)) {
                    return cb.lessThanOrEqualTo(fieldExp, (Date) operands.get(0));
                } else if (DOUBLE.equals(type)) {
                    return cb.lessThanOrEqualTo(fieldExp, (Double) operands.get(0));
                }
                break;
            case "greater":
                if ("date".equals(type)) {
                    return cb.greaterThan(fieldExp, (Date) operands.get(0));
                } else if (DOUBLE.equals(type)) {
                    return cb.greaterThan(fieldExp, (Double) operands.get(0));
                }
                break;
            case "greater_or_equal":
                if ("date".equals(type)) {
                    return cb.greaterThanOrEqualTo(fieldExp, (Date) operands.get(0));
                } else if (DOUBLE.equals(type)) {
                    return cb.greaterThanOrEqualTo(fieldExp, (Double) operands.get(0));
                }
                break;
            default:
                break;
        }

        // Should have return a value
        throw new IllegalArgumentException("Parsing exception " + type + " with values" + values + AND_OPERATOR + operator);
    }
}
