package com.qingcheng.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import com.qingcheng.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;


@Service
public class SeckillServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据区间时间查询该区间下的所有商品信息
     *
     * @param time
     * @return
     */
    @Override
    public List<SeckillGoods> seckillGoddsList(String time) {
        time = "SeckillGoods_" + DateUtil.formatStr(time);
        List<SeckillGoods> list = (List<SeckillGoods>) redisTemplate.boundHashOps(time).values();
        return list;
    }

    /**
     * 根据id和开始时间查询详细信息
     * @param time
     * @param id
     * @return
     */
    @Override
    public SeckillGoods one(String time, Long id) {
        return (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+time).get(id);
    }
}
