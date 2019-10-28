package com.qingcheng.comsumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.order.OrderItemService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("all")
public class OrderMessageConsumer implements MessageListener {

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //监听订单支付状态
    @Override
    public void onMessage(Message message) {
        //如果获取到订单id则代表该订单超时
        String orderId = new String(message.getBody());

        System.out.println("--------->get orderId ---->：" + orderId);

        //调用查询接口判断用户是否支付该订单
        Map map = wxPayService.queryPay(orderId);

        System.out.println("------->query wxpay result---->：" + map.get("result_code"));

        //如果未支付则调用订单关闭接口
        if (!"SUCCESS".equals(map.get("return_code")) || !"SUCCESS".equals(map.get("result_code"))) {
            //订单关闭需要执行该订单回滚操作
            Map closePay = wxPayService.closePay(orderId);

            System.out.println("------->close wxpay result---->：" + map.get("result_code"));

            //如果关闭订单成功则删除订单并让商品回滚
            if ("SUCCESS".equals(closePay.get("return_code"))) {

                System.out.println("------->order close success<-------");

                //库存回滚
                Order order = orderService.findById(orderId);

                Map map1 = new HashMap();
                map1.put("orderId", order.getId());
                List<OrderItem> orderItemList = orderItemService.findList(map1);
                //发送消息
                rabbitTemplate.convertAndSend("", "queue.back", JSON.toJSONString(orderItemList));
                System.out.println("------------->rabbit success begin rollback......");

                //删除订单
                orderService.delete(orderId);
            }
        }
    }
}
