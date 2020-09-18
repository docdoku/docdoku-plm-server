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

import com.docdoku.plm.server.dao.ConversionDAO;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.logging.Logger;


@Singleton
@Startup
public class PendingConversionsCleaner {

    private final static Integer RETENTION_TIME_MS = 60 * 60 * 1000;
    private final static String TIMER_HOURS = "*";
    private final static String TIMER_MINUTES = "*/5";
    private Logger LOGGER = Logger.getLogger(PendingConversionsCleaner.class.getName());

    @Inject
    private ConversionDAO conversionDAO;

    @PostConstruct
    private void start() {
        LOGGER.info("PendingConversionsCleaner registered");
    }

    @Schedule(hour = TIMER_HOURS, minute = TIMER_MINUTES, persistent = false)
    public void run() {
        LOGGER.info("Cleaning pending conversions");
        Integer conversionsSetAsFailed = conversionDAO.setPendingConversionsAsFailedIfOver(RETENTION_TIME_MS);
        LOGGER.info(conversionsSetAsFailed + " conversion(s) set as failed");
    }

}
