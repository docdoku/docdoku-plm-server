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
package com.docdoku.plm.server.hooks;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.docdoku.plm.server.core.hooks.SNSWebhookApp;
import com.docdoku.plm.server.core.hooks.Webhook;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SNSWebhookRunner implements WebhookRunner {

    private static final Logger LOGGER = Logger.getLogger(SNSWebhookRunner.class.getName());

    public SNSWebhookRunner() {
    }

    @Override
    public void run(Webhook webhook, String login, String email, String name, String subject, String content) {

        SNSWebhookApp webhookApp = (SNSWebhookApp) webhook.getWebhookApp();
        String topicArn = webhookApp.getTopicArn();
        String awsAccount = webhookApp.getAwsAccount();
        String awsSecret = webhookApp.getAwsSecret();
        String region = webhookApp.getRegion();
        //AmazonSNSClient snsClient = new AmazonSNSClient(new BasicAWSCredentials(awsAccount, awsSecret));
        //snsClient.setRegion(Region.getRegion(Regions.fromName(region)));


        AmazonSNS snsClient = AmazonSNSClient.builder()
                .withRegion(Regions.fromName(region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccount, awsSecret)))
                .build();


        try {
            PublishRequest publishReq = new PublishRequest()
                    .withTopicArn(topicArn)
                    .withMessage(getMessage(login, email, name, subject, content));
            snsClient.publish(publishReq);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot send notification to SNS service", e);
        } finally {
            LOGGER.log(Level.INFO, "Webhook runner terminated");
        }
    }

    private String getMessage(String login, String email, String name, String subject, String content) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("login", login);
        objectBuilder.add("email", email);
        objectBuilder.add("name", name);
        objectBuilder.add("subject", subject);
        objectBuilder.add("content", content);
        return objectBuilder.build().toString();
    }

}
