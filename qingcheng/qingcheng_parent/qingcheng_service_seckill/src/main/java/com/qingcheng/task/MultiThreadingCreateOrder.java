package com.qingcheng.task;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.utils.DateUtil;
import com.qingcheng.utils.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import sun.dc.pr.PRError;
import sun.rmi.runtime.Log;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;


    /**
     * 多线程下单操作
     */
    @Async
    public void createOrder() {
        try {
            //线程睡眠
            Thread.sleep(10000);

            System.out.println("------- Ready to perform ------");

            //从队列中右取一个秒杀订单信息
            SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();

            //获取商品个数信息
            Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillStatus.getGoodsId()).size();
            if (size == null || size <= 0) {
                //没有商品，清除排队信息
                clearQueue(seckillStatus);
                return;
            }


            //商品数据
            String username = null;
            String time = null;
            Long id = null;
            if (seckillStatus != null) {
                username = seckillStatus.getUsername(); //用户信息
                time = DateUtil.formatStr(seckillStatus.getTime()); //秒杀时间段
                id = seckillStatus.getGoodsId();  //商品id
            }

            //根据id查询商品详情
            SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);
            //该商品不存在
            if (goods == null || goods.getStockCount() <= 0) {
                throw new RuntimeException("已售完！");
            }

            //如果有库存，则创建秒杀商品订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            seckillOrder.setSeckillId(id);
            seckillOrder.setMoney(goods.getCostPrice());
            seckillOrder.setUserId(username);
            seckillOrder.setSellerId(goods.getSellerId());
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0");

            //将秒杀订单存入redis中
            redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);

            //扣减库存
            Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(id, -1);
            goods.setStockCount(seckillGoodsCount.intValue());

            //库存不足
            if (seckillGoodsCount <= 0) {
                //数据库更新秒杀商品
                seckillGoodsMapper.updateByPrimaryKeySelective(goods);
                //库存不足清空缓存商品
                redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
            } else {
                //如果有库存则重新更新该商品
                redisTemplate.boundHashOps("SeckillGoods_" + time).put(id, goods);
            }

            /**抢单成功！(更新用户的队列状态信息)*/
            seckillStatus.setOrderId(seckillOrder.getId());
            seckillStatus.setStatus(2); //下单成功
            seckillStatus.setMoney(seckillOrder.getMoney().floatValue());

            //更新队列状态信息
            redisTemplate.boundHashOps("UserQueueStatus").put(username, seckillStatus);

            //发送消息
            sendDelayMessage(seckillStatus);

            System.out.println("------- executing execute --------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除无效排队信息
     *
     * @param
     */
    private void clearQueue(SeckillStatus seckillStatus) {
        //清除排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

        //清除抢单标识
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
    }


    @Autowired
    private RabbitTemplate rabbitTemplate;

    /***
     * 延时消息发送
     * @param seckillStatus
     */
    public void sendDelayMessage(SeckillStatus seckillStatus) {

        rabbitTemplate.convertAndSend(
                "exchange.delay.order.begin",  //死信队列
                "delay",
                JSON.toJSONString(seckillStatus), //发送的信息
                new MessagePostProcessor() {

                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //消息有效期30分钟
                        //message.getMessageProperties().setExpiration(String.valueOf(1800000));
                        message.getMessageProperties().setExpiration(String.valueOf(10000));
                        return message;
                    }
                });
    }
}


