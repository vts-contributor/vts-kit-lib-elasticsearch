package com.viettel.vtskit.elasticsearch;

import com.viettel.vtskit.elasticsearch.configuration.ConstantConfiguration;
import com.viettel.vtskit.elasticsearch.configuration.ElasticsearchProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class ElasticsearchService {

    private ElasticsearchProperties elasticsearchProperties;

    public String exampleFunction(String name){
        return String.format(ConstantConfiguration.GREETING_MESSAGE, name);
    }

    @Autowired
    public void setElasticsearchProperties(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }
}
