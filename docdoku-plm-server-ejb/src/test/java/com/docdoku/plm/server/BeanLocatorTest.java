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
package com.docdoku.plm.server;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import com.docdoku.plm.server.converters.CADConverter;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class BeanLocatorTest {

    private static Context ctx;

    @BeforeClass
    public static void setup() throws Exception {
        ctx = new InitialContext(new Hashtable<>(Collections.singletonMap(Context.INITIAL_CONTEXT_FACTORY,
                "org.osjava.sj.memory.MemoryContextFactory")));
        CADConverter converter1 = Mockito.mock(CADConverter.class);
        Mockito.when(converter1.canConvertToOBJ("format")).thenReturn(true);
        CADConverter converter2 = Mockito.mock(CADConverter.class);
        Mockito.when(converter2.canConvertToOBJ("format")).thenReturn(true);
        ctx.createSubcontext("java:global");
        ctx.createSubcontext("java:global/application");
        ctx.createSubcontext("java:global/application/module");
        ctx.bind("java:global/application/module/c1Bean!com.docdoku.plm.server.converters.CADConverter", converter1);
        ctx.bind("java:global/application/module/c1Bean", converter1);
        ctx.bind("java:global/application/module/c2Bean", converter2);
        ctx.bind("java:global/application/module/c2Bean!com.docdoku.plm.server.converters.CADConverter", converter2);
    }

    private BeanLocator locator = new BeanLocator();

    @Test
    public void testSearch() {
        List<CADConverter> converters = locator.search(CADConverter.class, ctx);

        Assert.assertEquals(2, converters.size());
        for (CADConverter c : converters) {
            Assert.assertTrue(c.canConvertToOBJ("format"));
        }
    }
}
