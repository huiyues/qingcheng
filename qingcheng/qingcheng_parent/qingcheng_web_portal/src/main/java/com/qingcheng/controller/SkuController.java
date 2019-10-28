package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sku")
@CrossOrigin  //允许跨域请求
public class SkuController {


    @Reference
    private SkuService skuService;


    @RequestMapping("/price")
    public Integer findByPrice(String id){
        Integer byPrice = skuService.findByPrice(id);
        return byPrice;
    }
}
