package com.viettel.vtskit.elasticsearch.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

@ConfigurationProperties(prefix = "vtskit.elasticsearch")
public class ElasticsearchProperties {

    /**
     * Validate properties at here if necessary
     */
    private void validateProperties(){

    }

    @PostConstruct
    void init(){
        validateProperties();
    }

}
