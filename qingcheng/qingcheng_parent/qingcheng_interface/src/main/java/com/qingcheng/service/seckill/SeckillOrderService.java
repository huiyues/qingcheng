package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;

public interface SeckillOrderService {

    Boolean add(String time,Long id,String username);

    SeckillStatus queryStatus(String username);

    void updatePayStatus(String out_trade_no,String transactionId,String username);

    SeckillOrder findByUserName(String username);
}
