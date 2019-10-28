package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BackMessageConsumer implements MessageListener {

    @Autowired
    private StockBackService stockBackService;

    /**
     * 获取异常回滚信息
     *
     * @param message
     */
    @Override
    public void onMessage(Message message) {

        //存储信息
        try {
            String jsonString = new String(message.getBody());
            List<OrderItem> orderItemList = JSON.parseArray(jsonString, OrderItem.class);

            stockBackService.addList(orderItemList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
