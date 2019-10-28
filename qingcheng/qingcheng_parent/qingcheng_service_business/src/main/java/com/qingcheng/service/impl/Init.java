package com.qingcheng.service.impl;

import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @UpdateUser: heiye
 * @UpdateRemark: 编写缓存预热类(服务启动则加载该类并将缓存加载到redis中)
 */

@Component
public class Init implements InitializingBean {

    @Autowired
    private AdService adService;


    @Override
    public void afterPropertiesSet() throws Exception {

        adService.saveAllToRedis();//预热所有的广告信息

        System.out.println("------Ad Cache warm-up load is complete-------");
    }
}
