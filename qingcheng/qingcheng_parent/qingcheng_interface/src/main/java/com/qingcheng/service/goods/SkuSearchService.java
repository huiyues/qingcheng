package com.qingcheng.service.goods;

import java.util.List;
import java.util.Map;

public interface SkuSearchService {

    Map findByMap(Map<String,String> map);

    List<Map> findByBrand(String categoryName);

    List<Map> findBySpec(String categoryName);

    void saveToRedisByCategoryName(String categoryName);
}
