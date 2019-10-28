package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillGoods;

import java.util.List;
import java.util.ListResourceBundle;

public interface SeckillGoodsService {

    List<SeckillGoods> seckillGoddsList(String time);

    SeckillGoods one(String time,Long id);
}
