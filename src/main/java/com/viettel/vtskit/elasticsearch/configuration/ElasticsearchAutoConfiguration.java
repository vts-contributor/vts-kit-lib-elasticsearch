package com.viettel.vtskit.elasticsearch.configuration;

import com.viettel.vtskit.elasticsearch.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchAutoConfiguration {

    private ElasticsearchProperties elasticsearchProperties;

    @Bean
    public ElasticsearchService elasticsearchService(){
        return new ElasticsearchService();
    }

    @Autowired
    public void setElasticsearchProperties(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }
}
