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

package com.docdoku.plm.server.indexer.client;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import com.docdoku.plm.server.config.IndexerConfig;
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Produces a Jest client with elasticsearch config
 *
 * @author Morgan Guimard
 */
@Singleton(name = "IndexerClientProducer")
public class IndexerClientProducer {

    private static final Logger LOGGER = Logger.getLogger(IndexerClientProducer.class.getName());

    @Inject
    private IndexerConfig config;

    private JestClient client;

    @PostConstruct
    public void open() {
        LOGGER.log(Level.INFO, "Create Elasticsearch client");

        String serverUri = config.getServerUri();
        String username = config.getUserName();
        String password = config.getPassword();
        String awsService = config.getAWSService();
        String awsRegion = config.getAWSRegion();
        String awsAccessKey = config.getAWSAccessKey();
        String awsSecretKey = config.getAWSSecretKey();

        HttpClientConfig.Builder httpConfigBuilder = new HttpClientConfig.Builder(serverUri)
                .multiThreaded(true)
                .defaultMaxTotalConnectionPerRoute(8);

        if (username != null && password != null)
            httpConfigBuilder.defaultCredentials(username, password);

        JestClientFactory factory;

        if (awsService != null && awsRegion != null) {

            final AWSCredentialsProviderChain providerChain;

            if (awsAccessKey != null && awsSecretKey != null) {
                providerChain = new AWSCredentialsProviderChain(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(awsAccessKey, awsSecretKey)
                        )
                );
            } else {
                providerChain = new DefaultAWSCredentialsProviderChain();
            }

            final AWSSigner awsSigner = new AWSSigner(providerChain, awsRegion, awsService, () -> LocalDateTime.now(ZoneOffset.UTC));
            final AWSSigningRequestInterceptor requestInterceptor = new AWSSigningRequestInterceptor(awsSigner);

            factory = new JestClientFactory() {
                @Override
                protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
                    builder.addInterceptorLast(requestInterceptor);
                    return builder;
                }

                @Override
                protected HttpAsyncClientBuilder configureHttpClient(HttpAsyncClientBuilder builder) {
                    builder.addInterceptorLast(requestInterceptor);
                    return builder;
                }
            };
        } else {
            factory = new JestClientFactory();
        }
        factory.setHttpClientConfig(httpConfigBuilder.build());
        client = factory.getObject();

    }

    @PreDestroy
    public void close() {
        // todo => deprecated fallback ?
        //client.shutdownClient();
    }

    @Lock(LockType.READ)
    @Produces
    @ApplicationScoped
    public JestClient produce() {
        LOGGER.log(Level.INFO, "Producing ElasticSearch rest client");
        return client;
    }

}
