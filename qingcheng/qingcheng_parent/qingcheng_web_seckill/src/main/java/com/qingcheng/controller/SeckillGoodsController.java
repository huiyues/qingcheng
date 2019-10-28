package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.utils.DateUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/seckill/goods")
public class SeckillGoodsController {

    @Reference
    private SeckillGoodsService seckillGoodsService;

    @Reference
    private SeckillOrderService seckillOrderService;


    /**
     * 返回当前的区间时间菜单
     *
     * @return
     */
    @RequestMapping(value = "/menus")
    public List<Date> getMenus() {
        List<Date> menus = DateUtil.getDateMenus();
        return menus;
    }


    /**
     * 获取对应的时间的商品
     *
     * @return
     */
    @GetMapping("/list")
    public List<SeckillGoods> findList(String time) {
        List<SeckillGoods> seckillGoods = seckillGoodsService.seckillGoddsList(time);
        return seckillGoods;
    }


    /**
     * 根据活动开始时间和商品id查询详细信息
     *
     * @param time
     * @param id
     * @return
     */
    @GetMapping("/one")
    public SeckillGoods findOne(String time, String id) {
        return seckillGoodsService.one(time, Long.parseLong(id));
    }


    /**
     * 秒杀下单逻辑
     *
     * @param id
     * @return
     */
    @GetMapping("/add")
    public Result add(String time, Long id) {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            //用户未登陆
            if (username != null && "anonymousUser".equals(username)) {
                return new Result(403, "你尚未登陆！");
            }

            //已登录执行下单逻辑
            Boolean b = seckillOrderService.add(time, id, username);
            if (b) {
                return new Result(0,"下单成功！");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Result(2,e.getMessage()); //100  //101
        }
        return new Result(1, "秒杀下单失败！");
    }
}














