package com.qingcheng.task;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.order.WxPayService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderMessageListener implements MessageListener {


    @Override
    public void onMessage(Message message) {
        String content = new String(message.getBody());

        //执行库存回滚
        SeckillStatus seckillStatus = JSON.parseObject(content, SeckillStatus.class);
        rollBackSeckillOrder(seckillStatus);

        System.out.println("监听到的信息：" + content);
    }


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 订单回滚操作
     *
     * @param seckillStatus
     */
    public void rollBackSeckillOrder(SeckillStatus seckillStatus) {
        //获取redis中的订单信息
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(seckillStatus.getUsername());
        //如果缓存有订单，说明未支付
        if (seckillOrder != null) {
            //关闭订单
            Map<String, String> map = wxPayService.closePay(seckillOrder.getId() + "");
            if (map.get("return_code").equalsIgnoreCase("SUCCESS") && map.get("result").equalsIgnoreCase("SUCCESS")) {
                //删除订单
                redisTemplate.boundHashOps("SeckillOrder").delete(seckillStatus.getUsername());
                //回滚库存
                //如果缓存中有
                SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + seckillStatus.getTime()).get(seckillStatus.getGoodsId());
                //没有则从数据库加载再执行回滚
                if (seckillGoods == null) {
                    seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());
                }
                //执行库存回滚
                Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), 1);
                seckillGoods.setStockCount(seckillGoodsCount.intValue());
                redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillStatus.getGoodsId()).leftPush(seckillGoods);

                //数据同步到redis
                redisTemplate.boundHashOps("SeckillGoodsCountList_" + seckillStatus.getTime()).put(seckillStatus.getGoodsId(), seckillGoods);

                //清除队列标识
                redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

                //清除清单标识
                redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
            }
        }
    }
}
