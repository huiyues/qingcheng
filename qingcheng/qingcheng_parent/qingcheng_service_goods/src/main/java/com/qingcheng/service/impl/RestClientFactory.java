package com.qingcheng.service.impl;


import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * @UpdateUser: heiye
 * @UpdateRemark: rest高级工厂类
 */
public class RestClientFactory {


    public static RestHighLevelClient getRestHighLeveClinet(String position,Integer port) {
        HttpHost host = new HttpHost(position,port,"http");
        RestClientBuilder restClientBuilder = RestClient.builder(host);
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(restClientBuilder);

        return restHighLevelClient;
    }
}
