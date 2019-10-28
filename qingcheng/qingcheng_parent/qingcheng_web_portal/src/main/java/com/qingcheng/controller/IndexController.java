package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.util.List;
import java.util.Map;

@Controller
@EnableDubboConfig
//@RequestMapping("/portal")
public class IndexController {


    @Reference
    private AdService adService;

    @Reference
    private CategoryService categoryService;

    @GetMapping("/index")
    public String index(Model model){


        List<Ad> index_1b = adService.findByList("web_index_lb");
        model.addAttribute("ibt",index_1b );

        //查询树形菜单
        List<Map> mapList = categoryService.categoryTree();
        model.addAttribute("mapList",mapList );

        return "index";
    }
}
