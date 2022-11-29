package com.viettel.vtskit.elasticsearch.configuration;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.http.HttpHeaders;

@Configuration
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${elasticsearch.port}")
    public String elasticsearchPort;

    @Value("${elasticsearch.host}")
    public String elasticsearchHost;

    @Value("${elasticsearch.username}")
    public String username;

    @Value("${elasticsearch.password}")
    public String password;

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {

        HttpHeaders compatibilityHeaders = new HttpHeaders();
        compatibilityHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");
        compatibilityHeaders.add("Content-Type", "application/vnd.elasticsearch+json;"
                + "compatible-with=7");

        final StringBuilder ElasticSearchURLStrBuilder = new StringBuilder();
        ElasticSearchURLStrBuilder.append(elasticsearchHost).append(":").append(elasticsearchPort);

        final ClientConfiguration config = ClientConfiguration.builder()
                .connectedTo(ElasticSearchURLStrBuilder.toString())
                .withBasicAuth(username, password)
                .withDefaultHeaders(compatibilityHeaders)
                .build();

        return RestClients.create(config).rest();
    }
}
