package com.viettel.vtskit.elasticsearch.configuration;

import com.viettel.vtskit.elasticsearch.search.ElasticsearchService;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.http.HttpHeaders;

@Configuration
public class ElasticsearchAutoConfiguration extends AbstractElasticsearchConfiguration {

    private final ElasticsearchProperties elasticsearchProperties;

    @Autowired
    public ElasticsearchAutoConfiguration(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Bean
    @ConfigurationProperties(
            prefix = "spring.elasticsearch"
    )
    @Primary
    public CustomElasticsearchProperties elasticsearchProperties(){
        return new CustomElasticsearchProperties();
    }

    @Bean
    ElasticsearchService elasticsearchService(){
        return new ElasticsearchService();
    }

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {

        HttpHeaders compatibilityHeaders = new HttpHeaders();
        compatibilityHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");
        compatibilityHeaders.add("Content-Type", "application/vnd.elasticsearch+json;"
                + "compatible-with=7");

        final ClientConfiguration config = ClientConfiguration.builder()
                .connectedTo(elasticsearchProperties.getUris().get(0))
                .withBasicAuth(elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword())
                .withDefaultHeaders(compatibilityHeaders)
                .build();

        return RestClients.create(config).rest();
    }


}
