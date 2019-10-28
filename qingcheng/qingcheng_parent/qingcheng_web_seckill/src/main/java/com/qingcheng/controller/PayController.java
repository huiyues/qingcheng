package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.service.seckill.SeckillOrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private SeckillOrderService seckillOrderService;

    @Reference
    private WxPayService wxPayService;

    /**
     * 生成该订单的微信二维码
     *
     * @param orderId
     * @return
     */
    @GetMapping("/createNative")
    public Map createNative(String orderId) {

        //获取登陆用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SeckillOrder seckillOrder = seckillOrderService.findByUserName(username);

        //判断需要生成二维码的订单
        if (seckillOrder != null) {
            if (username.equals(seckillOrder.getUserId())) {
                //如果是该订单则生成二维码
                int money =  (int) ((seckillOrder.getMoney().doubleValue())*100);
                Map map = wxPayService.wxpayUrl(seckillOrder.getId().toString(), money, " http://heiye.easy.echosite.cn/pay/notify.do",username);
                return map;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * 支付成功后的回调方法
     */
    @PostMapping("/notify")
    public void notifyLogs(HttpServletRequest request) {
        System.out.println("payment success...Will return automatically after three seconds...");

        //获取请求流
        ServletInputStream inputStream;
        try {
            inputStream = request.getInputStream();
            //创建输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            //定义指针
            int len = 0;
            byte[] bytes = new byte[1024];
            while ((len = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
            }

            String xml = new String(outputStream.toByteArray(), "utf-8");
            System.out.println(xml);
            wxPayService.notifyLogic(xml);
            outputStream.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
