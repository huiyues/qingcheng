package com.qingcheng.service.impl;

import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
  * @UpdateUser:     heiye
  * @UpdateRemark:   编写缓存预热类(服务启动则加载该类并将缓存加载到redis中)
  */

@Component
public class Init implements InitializingBean {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuService skuService;

    @Override
    public void afterPropertiesSet() throws Exception {

        categoryService.saveCategoryTreeCache();  //预热分类信息
        System.out.println("------categoryTree Cache warm-up load is complete-------");

        skuService.saveAllToRedis();//商品价格的预热
        System.out.println("------skuPrice Cache warm-up load is complete-------");

        skuService.importToEs(); //商品数据导入elasticsearch
        System.out.println("------skuPrice Preheating of commodity data-------");
    }
}
