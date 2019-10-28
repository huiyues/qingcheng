package com.qingcheng.timer;


import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillGoodsPushTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Scheduled(cron = "0/30 * * * * ?")
    public void loadGoodsPushRedis() {

        //获取当前秒杀区间时间
        List<Date> dateList = DateUtil.getDateMenus();
        for (Date startTime : dateList) {
            //创建查询条件
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            //判断审核状态
            criteria.andEqualTo("status", "1");
            //判断库存
            criteria.andGreaterThan("stockCount", 0);
            //判断现在的区间时间大于等于活动开始时间
            criteria.andGreaterThanOrEqualTo("startTime", startTime);
            //活动时间小于当前的区间结束时间
            criteria.andLessThan("endTime", DateUtil.addDateHour(startTime, 2));

            //判断缓存中是否存在相同的数据
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + DateUtil.date2Str(startTime)).keys();
            if (keys != null && keys.size() > 0) {
                criteria.andNotIn("id", keys);
            }

            //查询商品
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);
            //存入缓存
            for (SeckillGoods seckillGood : seckillGoods) {
                //商品列表数据
                redisTemplate.boundHashOps("SeckillGoods_" + DateUtil.date2Str(startTime)).put(seckillGood.getId(), seckillGood);

                //商品库存个数和id
                Long[] ids = pushIds(seckillGood.getStockCount(), seckillGood.getId());
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGood.getId()).leftPushAll(ids);

                //自增计数器
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGood.getId(),seckillGood.getStockCount());
            }
            Set keys1 = redisTemplate.boundHashOps("SeckillGoods_" + DateUtil.date2Str(startTime)).keys();
            System.out.println("======= seckillGoods Preheat finished =====" + keys1 +"号商品");
        }
    }

    /**
     * 超卖解决方案
     * @param len 商品个数
     * @param id  商品id
     * @return
     */
    public Long[] pushIds(int len, Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i < len; i++) {
            ids[i] = id;
        }
        return ids;
    }
}
