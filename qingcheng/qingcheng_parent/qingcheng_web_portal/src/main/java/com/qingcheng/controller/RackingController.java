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
public class RackingController implements MessageListener {

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

        //更新索引
        createIndex(spuId);

        System.out.println("------------sku importIndex success --------");

        //生成页面
        createPage(spuId);

        System.out.println("-------- itemPage createPage success  ----------");
    }


    public void createPage(String spuId) {
        //获取spu
        Goods goods = spuService.findGoodsById(spuId);
        Spu spu = goods.getSpu();

        //获取每一个sku
        List<Sku> skuList = goods.getSkuList();

        //获取每级分类的名称
        //用list集合封装
        List<String> categoryList = new ArrayList<>();
        categoryList.add(categoryService.findById(spu.getCategory1Id()).getName());  //获取一级分列的名称
        categoryList.add(categoryService.findById(spu.getCategory2Id()).getName()); //获取二级分列的名称
        categoryList.add(categoryService.findById(spu.getCategory3Id()).getName()); //获取三级分列的名称

        Map<String, String> urlMap = new HashMap<>();
        //给所有的规格列表添加URL
        for (Sku sku : skuList) {
            //判断无效的规格
            if ("1".equals(sku.getStatus())) {
                //对规格json字符串进行排序
                String specJson = JSON.toJSONString(JSON.parseObject(sku.getSpec()), SerializerFeature.MapSortField);
                //给每一个规格添加URL
                urlMap.put(specJson, sku.getId() + ".html");
            }
        }


        //生成每一个sku页面
        for (Sku sku : skuList) {
            //创建存储数据map
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("spu", spu);
            dataMap.put("sku", sku);
            dataMap.put("categoryList", categoryList);
            dataMap.put("spuImages", spu.getImages().split(","));
            dataMap.put("skuImages", sku.getImages().split(","));

            //添加参数列表
            Map paraItems = JSON.parseObject(spu.getParaItems());
            dataMap.put("paraItems", paraItems);

            //添加规格列表
            Map<String, String> specItems = (Map) JSON.parseObject(sku.getSpec());
            dataMap.put("specItems", specItems);

            //添加规格面板
            Map<String, List> specMap = (Map) JSON.parseObject(spu.getSpecItems());

            //遍历规格
            for (String key : specMap.keySet()) {
                //得到每一个value
                List<String> list = specMap.get(key);

                //创建一个新的规格值的集合
                List<Map> mapList = new ArrayList<>();
                //再遍历规格的值
                for (String value : list) {
                    Map map = new HashMap();
                    map.put("option", value);

                    //判断规格列表的值是否和规格面板的值相同
                    if (specItems.get(key) != null && specItems.get(key).equals(value)) {
                        map.put("checked", true);
                    } else {
                        map.put("checked", false);
                    }

                    //得到当前遍历的sku规格
                    Map spec = JSON.parseObject(sku.getSpec());
                    //添加当前用户点击的规格
                    spec.put(key, value);

                    //对当前的规格进行排序
                    String specJson = JSON.toJSONString(spec, SerializerFeature.MapSortField);
                    //得到对应的页面
                    map.put("url", urlMap.get(specJson));

                    //最后产生新的规格值集合
                    mapList.add(map);
                }
                //覆盖之前的集合
                specMap.put(key, mapList);
            }
            dataMap.put("specMap", specMap);

            //创建上下文对象
            Context context = new Context();
            //封装页信息
            context.setVariables(dataMap);

            //创建文件对象,读取创建地址
            File dir = new File(pagePath);
            //判断目录是否存在
            if (!dir.exists()) {
                dir.mkdir();
            }

            //如果目录存在则创建子文件
            File dirText = new File(dir, sku.getId() + ".html");
            try {
                //获取输出流
                PrintWriter printWriter = new PrintWriter(dirText, "UTF-8");

                //获取模板引擎
                templateEngine.process("item", context, printWriter);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 生成索引库
     */
    public void createIndex(String spuId) {
        try {
            HttpHost host = new HttpHost("localhost", 9200, "http");
            RestClientBuilder restClientBuilder = RestClient.builder(host);
            RestHighLevelClient restHighLevelClient = new RestHighLevelClient(restClientBuilder);

            Spu spu = spuService.findById(spuId);
            Map maps = new HashMap();
            List<Sku> brandList = null;
            if (spu != null && "1".equals(spu.getIsMarketable())) {
                maps.put("spuId", spu.getId());
                brandList = skuService.findList(maps);
            }
            //List<Sku> brandList = skuService.findAll();

            BulkRequest bulkRequest = new BulkRequest();
            for (Sku brand : brandList) {
                IndexRequest indexRequest = new IndexRequest("sku", "doc", brand.getId().toString());
                Map<String, Object> map = new HashMap();
                map.put("name", brand.getName());
                map.put("brandName", brand.getBrandName());
                map.put("commentNum", brand.getCommentNum());
                map.put("image", brand.getImage());
                map.put("createTime", brand.getCreateTime());
                map.put("price", brand.getPrice());
                map.put("spuId", brand.getSpuId());
                map.put("categoryName", brand.getCategoryName());
                map.put("saleNum", brand.getSaleNum());
                map.put("spec", JSON.parseObject(brand.getSpec()));

                indexRequest.source(map);
                bulkRequest.add(indexRequest);
            }

            BulkResponse responses = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            int status = responses.status().getStatus();
            System.out.println("status code-->"+status);

            //String message = responses.buildFailureMessage();
            //System.out.println(message);

            restHighLevelClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
