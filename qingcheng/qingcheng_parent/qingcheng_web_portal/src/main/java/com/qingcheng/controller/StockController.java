package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpuService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StockController implements MessageListener {

    @Reference
    private SkuService skuService;

    @Reference
    private SpuService spuService;

    @Reference
    private CategoryService categoryService;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${pagePath}")
    private String pagePath;

    //获取消费消息
    @Override
    public void onMessage(Message message) {
        //获取商品id
        String spuId = new String(message.getBody());
        deleteIndex(spuId);
    }

    /**
     * 删除索引库
     */
    public void deleteIndex(String spuId) {
        try {
            HttpHost host = new HttpHost("localhost", 9200, "http");
            RestClientBuilder restClientBuilder = RestClient.builder(host);
            RestHighLevelClient restHighLevelClient = new RestHighLevelClient(restClientBuilder);

            Spu spu = spuService.findById(spuId);
            Map maps = new HashMap();
            List<Sku> brandList = null;
            if (spu != null && "0".equals(spu.getIsMarketable())) {
                maps.put("spuId", spu.getId());
                brandList = skuService.findList(maps);
            }
            //List<Sku> brandList = skuService.findAll();

            BulkRequest bulkRequest = new BulkRequest();
            for (Sku brand : brandList) {
                DeleteRequest deleteRequest = new DeleteRequest("sku", "doc", brand.getId().toString());
                deleteRequest.id(brand.getId());
                bulkRequest.add(deleteRequest);
            }

            BulkResponse responses = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            int status = responses.status().getStatus();
            System.out.println(status);

            String message = responses.buildFailureMessage();
            System.out.println(message);

            restHighLevelClient.close();
            System.out.println("------------sku deleteIndex success --------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
