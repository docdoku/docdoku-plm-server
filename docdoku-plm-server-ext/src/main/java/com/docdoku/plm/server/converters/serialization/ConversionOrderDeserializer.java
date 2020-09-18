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

package com.docdoku.plm.server.converters.serialization;

import org.apache.kafka.common.serialization.Deserializer;
import com.docdoku.plm.server.converters.ConversionOrder;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author
 */
public class ConversionOrderDeserializer implements Deserializer<ConversionOrder> {

    private final static Logger LOGGER = Logger.getLogger(ConversionOrderDeserializer.class.getName());
    private Jsonb jsonb = JsonbBuilder.create();

    public ConversionOrderDeserializer() {
    }

    @Override
    public ConversionOrder deserialize(String s, byte[] bytes) {
        try {
            return jsonb.fromJson(new String(bytes), ConversionOrder.class);
        } catch (JsonbException e){
            LOGGER.warning("Cannot deserialize " + s);
            return null;
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public void close() {

    }
}
