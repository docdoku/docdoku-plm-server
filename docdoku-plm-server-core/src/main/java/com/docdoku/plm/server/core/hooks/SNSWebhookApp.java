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

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A {@link WebhookApp} subclass which relies on Amazon SNS (Simple Notification Service)
 * in order to notify either end-users or third-party applications.
 *
 * @author Morgan Guimard
 * @version 2.5, 14/10/17
 * @see     WebhookApp
 * @see     SimpleWebhookApp
 * @since V2.5
 */
@Table(name = "SNSWEBHOOKAPP")
@Entity
public class SNSWebhookApp extends WebhookApp {

    public static final String APP_NAME = "SNSWEBHOOK";

    private String topicArn;
    private String region;
    private String awsAccount;
    private String awsSecret;

    public SNSWebhookApp(String topicArn, String region, String awsAccount, String awsSecret) {
        this.topicArn = topicArn;
        this.region = region;
        this.awsAccount = awsAccount;
        this.awsSecret = awsSecret;
    }

    public SNSWebhookApp() {
    }


    @Override
    public String getAppName() {
        return APP_NAME;
    }

    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAwsAccount() {
        return awsAccount;
    }

    public void setAwsAccount(String awsAccount) {
        this.awsAccount = awsAccount;
    }

    public String getAwsSecret() {
        return awsSecret;
    }

    public void setAwsSecret(String awsSecret) {
        this.awsSecret = awsSecret;
    }
}
