package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.utils.CacheKey;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SkuSearchServiceImp implements SkuSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Reference
    private SpuService spuService;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpecMapper specMapper;

    @Override
    public Map findByMap(Map<String, String> searchMap) {

        //发起查询请求
        SearchRequest searchRequest = new SearchRequest("sku");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //布尔查询判断
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //关键字查询
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", searchMap.get("keywords"));
        boolQueryBuilder.must(matchQueryBuilder);

        //商品分类过滤
        if (searchMap.get("categoryName") != null) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("categoryName", searchMap.get("categoryName"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //品牌过滤
        if (searchMap.get("brandName") != null) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", searchMap.get("brandName"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //规格过滤
        for (String key : searchMap.keySet()) {
            if (key.startsWith("spec.")) {
                //如果为版本则继续字符串拼接
                if (key.equals("spec.版本")) {
                    String[] split = searchMap.get(key).split(" ");  //GB GB
                    StringBuffer sb = new StringBuffer(split[0]); //GB+GB
                    sb.append("+" + split[1]);
                    TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key + ".keyword", sb.toString());
                    boolQueryBuilder.filter(termQueryBuilder);
                }
                TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key + ".keyword", searchMap.get(key));
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //价格过滤
        if (searchMap.get("price") != null) {
            String[] prices = searchMap.get("price").split("-");
            //最低价格
            if (!prices[0].equals(0)) {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(prices[0] + "0");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
            //最高价格
            if (!prices[1].equals("*")) {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").lte(prices[1] + "0");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
        }

        //聚合查询（商品分类）
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("sku_category").field("categoryName");
        searchSourceBuilder.aggregation(aggregationBuilder);

        //封装查询
        searchSourceBuilder.query(boolQueryBuilder);

        //分页逻辑
        Integer pageNum = Integer.parseInt(searchMap.get("pageNum")); //页码
        Integer pageSize = 10;   //每页显示条数
        Integer fromIndex = (pageNum - 1) * pageSize; //计算开始索引

        searchSourceBuilder.from(fromIndex); //开始索引
        searchSourceBuilder.size(pageSize); //每页条数

        //排序查询
        String sort = searchMap.get("sort");  //排序字段
        String sortOrder = searchMap.get("sortOrder"); //排序规则
        if (!"".equals(sort)) {  //设置排序
            searchSourceBuilder.sort(sort, SortOrder.valueOf(sortOrder));
        }

        //搜索高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //设置高亮属性
        highlightBuilder.field("name").preTags("<font style='color:yellow'><b>").postTags("</b></font>");

        searchSourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(searchSourceBuilder);

        //数据封装
        Map resultMap = new HashMap();
        try {
            //接受返回数据
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            //System.out.println("总记录数：" + hits.getTotalHits());

            //商品列表
            List<Map> skuList = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                if (sourceAsMap != null) {
                    //获取高亮的值
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    Text[] names = highlightFields.get("name").fragments();
                    String name = names[0].toString();
                    sourceAsMap.put("name", name); // 用高亮的值替换原来的值
                    skuList.add(sourceAsMap);
                }
            }
            resultMap.put("rows", skuList);

            //商品分类
            List<String> categoryList = new ArrayList<>();
            Aggregations aggregations = searchResponse.getAggregations();
            Terms categoryName = (Terms) aggregations.getAsMap().get("sku_category");
            List<? extends Terms.Bucket> buckets = categoryName.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                if (bucket.getKeyAsString() != null) {
                    categoryList.add(bucket.getKeyAsString());
                }
            }
            resultMap.put("categoryList", categoryList);

            //公共代码
            String cateName = ""; //分类名称
            if (searchMap.get("categoryName") == null) {
                if (categoryList.size() > 0) {
                    cateName = categoryList.get(0);  //如果前台没有传分类名称则默认第一个
                }
            } else {
                cateName = searchMap.get("categoryName");
            }

            //品牌列表
            if (searchMap.get("brandName") == null) {
                List<Map> brandList = findByBrand(cateName);//对应的分类名称品牌
                resultMap.put("brandList", brandList);
            }

            //规格列表
            List<Map> specList = findBySpec(cateName);//对应的分类名称规格
            for (Map spec : specList) {
                //类型转换加字符串拆分
                String[] option = ((String) spec.get("options")).split(",");
                spec.put("options", option);
            }
            resultMap.put("specList", specList);


            //页码展示
            long totalCount = hits.getTotalHits(); //总记录数
            long pageCount = (totalCount % pageSize == 0) ? (totalCount / pageSize) : (totalCount / pageSize + 1); //总页数
            resultMap.put("totalPages", pageCount);

            //关闭连接
            //restHighLevelClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据商品名称查询品牌缓存
     */
    @Override
    public List<Map> findByBrand(String categoryName) {
        saveToRedisByCategoryName(categoryName);
        //获取品牌缓存
        Map mapList = (Map) redisTemplate.boundHashOps(CacheKey.SEARCH_MAP).get(categoryName);
        List<Map> brandList = (List<Map>) mapList.get("brandList");
        return brandList;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据商品名称查询规格缓存
     */
    @Override
    public List<Map> findBySpec(String categoryName) {
        saveToRedisByCategoryName(categoryName);
        //获取规格缓存
        Map mapList = (Map) redisTemplate.boundHashOps(CacheKey.SEARCH_MAP).get(categoryName);
        List<Map> specList = (List<Map>) mapList.get("specList");
        return specList;
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 添加品牌和规格缓存
     */

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void saveToRedisByCategoryName(String categoryName) {

        //判断缓存中是否有数据
        if (redisTemplate.boundHashOps(CacheKey.SEARCH_MAP).get(categoryName) == null) {
            //添加品牌缓存
            List<Map> brandList = brandMapper.findByMap(categoryName);
            //添加规格缓存
            List<Map> specList = specMapper.findByCategoryName(categoryName);

            //封装品牌和规格数据
            Map map = new HashMap();
            map.put("brandList", brandList);
            map.put("specList", specList);

            redisTemplate.boundHashOps(CacheKey.SEARCH_MAP).put(categoryName, map);
            //设置随机过期时间
            Random random = new Random(15);
            redisTemplate.boundHashOps(CacheKey.SEARCH_MAP).expire(random.nextInt(30), TimeUnit.DAYS);
        } else {
            System.out.println("--------Brand And Spec Existing cache-----------");
        }
    }
}