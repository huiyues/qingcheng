package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.utils.DateUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/seckill/order")
public class SeckillOrderController {

    @Reference
    private SeckillOrderService seckillOrderService;


    /**
     * 根据用户名查询该用户的订单状态
     *
     * @return
     */
    @GetMapping("/queryStatus")
    public Result queryStatus() {
        //获取登陆用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        //判断用户是否登陆
        if ("anonymousUser".equals(username)) {
            //未登陆
            return new Result(403, "用户未登陆！");
        }

        try {
            //已登录
            SeckillStatus seckillStatus = seckillOrderService.queryStatus(username);
            if (seckillStatus == null) {
                //设置默认值
                seckillStatus = new SeckillStatus();
                seckillStatus.setUsername(username);
                seckillStatus.setTime(DateUtil.date2Str(new Date()));
                seckillStatus.setStatus(1);
                seckillStatus.setCreateTime(new Date());
            }
            //排队状态
            Result result = new Result(seckillStatus.getStatus(), "抢购状态");
            result.setOther(seckillStatus);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            //发生错误
            return new Result(4, e.getMessage());
        }
    }
}
