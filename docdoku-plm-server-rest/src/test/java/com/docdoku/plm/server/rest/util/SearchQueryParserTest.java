package com.docdoku.plm.server.rest.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static com.docdoku.plm.server.core.query.SearchQuery.AbstractAttributeQuery;
import static com.docdoku.plm.server.core.query.SearchQuery.TextAttributeQuery;

public class SearchQueryParserTest {

    @Test
    public void shouldParseAttributesThatContainsEscapedCharactersInName() {
        String query = "TEXT:myname\\;:myValue";

        List<AbstractAttributeQuery> attributes = SearchQueryParser.parseAttributeStringQuery(query);

        assertEquals(1, attributes.size());
        assertTextAttribute(attributes.get(0), "myname\\;", "myValue");

    }

    @Test
    public void shouldParseAttributesThatContainsEscapedCharactersInValue() {
        String query = "TEXT:myname\\;:myV\\;alue";

        List<AbstractAttributeQuery> attributes = SearchQueryParser.parseAttributeStringQuery(query);

        assertEquals(1, attributes.size());
        assertTextAttribute(attributes.get(0), "myname\\;", "myV\\;alue");
    }

    @Test
    public void shouldParseQueryWithMoreThanOneAttributesThatContainsEscapedCharacters() {
        String query = "TEXT:myname\\;:myV\\;alue;TEXT:myname\\:fgdfg:myvalue";

        List<AbstractAttributeQuery> attributes = SearchQueryParser.parseAttributeStringQuery(query);

        assertEquals(2, attributes.size());
        assertTextAttribute(attributes.get(0), "myname\\;", "myV\\;alue");
        assertTextAttribute(attributes.get(1), "myname\\:fgdfg", "myvalue");
    }

    @Test
    public void shouldParseQueryWithMoreThanOneAttributesWithoutEscapedCharacters() {
        String query = "TEXT:myname:myValue;TEXT:myname2:myvalue2";

        List<AbstractAttributeQuery> attributes = SearchQueryParser.parseAttributeStringQuery(query);

        assertEquals(2, attributes.size());
        assertTextAttribute(attributes.get(0), "myname", "myValue");
        assertTextAttribute(attributes.get(1), "myname2", "myvalue2");
    }

    private void assertTextAttribute(AbstractAttributeQuery attribute, String expectedName, String expectedValue) {
        assertEquals(TextAttributeQuery.class, attribute.getClass());
        assertEquals(expectedName, attribute.getName());
        assertEquals(expectedValue, ((TextAttributeQuery) attribute).getTextValue());
    }

}