package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.utils.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@EnableDubboConfig
public class SkuSearchController {


    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("/search")
    private String search(Model model, @RequestParam Map searchMap) throws Exception {
        //字符集处理
        searchMap = WebUtil.convertCharsetToUTF8(searchMap);

        //页码处理
        if (searchMap.get("pageNum") == null) {
            searchMap.put("pageNum", "1");
        }
        Integer pageNum = Integer.parseInt(searchMap.get("pageNum").toString());

        //排序
        if (searchMap.get("sort") == null) {  //排序字段
            searchMap.put("sort", "");
        }

        if (searchMap.get("sortOrder") == null) {  //排序规则
            searchMap.put("sortOrder", "DESC");
        }

        Map map = skuSearchService.findByMap(searchMap);

        //手动拼接地址栏参数
        StringBuffer sb = new StringBuffer("/search.do?");
        for (Object key : searchMap.keySet()) {
            sb.append("&" + key + "=" + searchMap.get(key));
        }

        //分页
        long totalPage = (long) map.get("totalPages");
        long startPage = 1; //开始页
        long endPage = totalPage; //结束页
        if (totalPage > 5) {
            startPage = pageNum - 2;
            if (startPage < 1) {
                startPage = 1;
            }
            if (pageNum <= totalPage - 2) {
                endPage = startPage + 4;
            } else {
                startPage = totalPage - 4;
            }
        }

        model.addAttribute("url", sb);
        model.addAttribute("searchMap", searchMap);
        model.addAttribute("result", map);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        return "search";
    }
}
