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
package com.docdoku.plm.server.core.hooks;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A {@link WebhookApp} subclass which performs an HTTP request on a given URI.
 * The HTTP method and authorization header can also be customized.
 *
 * @author Morgan Guimard
 * @version 2.5, 14/10/17
 * @see     WebhookApp
 * @see     SNSWebhookApp
 * @since V2.5
 */
@Table(name = "SIMPLEWEBHOOKAPP")
@Entity
public class SimpleWebhookApp extends WebhookApp {

    public static final String APP_NAME = "SIMPLEWEBHOOK";

    private String method;

    private String uri;

    @Column(name="AUTH")
    private String authorization;

    public SimpleWebhookApp(String method, String authorization, String uri) {
        this.method = method;
        this.authorization = authorization;
        this.uri = uri;
    }

    public SimpleWebhookApp() {
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    @Override
    public String getAppName() {
        return APP_NAME;
    }
}
