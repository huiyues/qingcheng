package com.qingcheng.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.task.MultiThreadingCreateOrder;
import com.qingcheng.utils.DateUtil;
import com.qingcheng.utils.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;


@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    /**
     * 秒杀下单
     * @param time
     * @param id
     * @param username
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public Boolean add(String time, Long id, String username) {

        //异步队列下单
        System.out.println("排队下单中........------------");

        //封装用户秒杀抢单信息
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(), 1, id,time);


        //用户的排队数量
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1); //1
        //如果大于了2则代表重复下单
        if (userQueueCount > 1){ //2
            //重复排队错误码
            throw new RuntimeException("100");
        }

        //防止无效排队
        Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();
        if (size <= 0){
            //没有库存
            throw new RuntimeException("101");
        }

        //添加下单队列
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);

        //添加用户状态信息队列
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus );

        //开启多线程下单
        multiThreadingCreateOrder.createOrder();

        return true;
    }


    /**
     * 根据用户查询该用户的订单状态
     * @param username
     * @return
     */
    @Override
    public SeckillStatus queryStatus(String username) {
        return (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
    }


    /**
     * 更新订单状态的方法
     * @param out_trade_no
     * @param transactionId 交易流水号
     * @param username 用户
     */
    @Override
    public void updatePayStatus(String out_trade_no, String transactionId, String username) {
        //从redis数据库中查询订单数据
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        //修改状态
        seckillOrder.setStatus("1");
        //支付时间
        seckillOrder.setPayTime(new Date());
        //交易流水号
        seckillOrder.setTransactionId(transactionId);
        //同步到mysql数据库
        seckillOrderMapper.insertSelective(seckillOrder);
        //清除缓存
        redisTemplate.boundHashOps("seckillOrder").delete(username);
        //清除用户排队信息
        redisTemplate.boundHashOps("UserQueueCount").delete(username);
        //清除抢购状态信息
        redisTemplate.boundHashOps("UserQueueStatus").delete(username);
    }


    /**
     * 根据用户名查询订单信息
     * @param username
     * @return
     */
    @Override
    public SeckillOrder findByUserName(String username) {
        return (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
    }
}














