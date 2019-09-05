package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.StockBackMapper;
import com.qingcheng.pojo.goods.StockBack;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@Service(interfaceClass = StockBackService.class)
public class StockBackServiceimpl implements StockBackService {

    @Autowired
    private StockBackMapper stockBackMapper;


    @Autowired
    private SkuMapper skuMapper;

    /**
     * 存储回滚异常数据
     *
     * @param orderItemList
     */
    @Override
    @Transactional
    public void addList(List<OrderItem> orderItemList) {

        for (OrderItem orderItem : orderItemList) {
            StockBack stockBack = new StockBack();

            stockBack.setOrderId(orderItem.getOrderId());
            stockBack.setSkuId(orderItem.getSkuId());
            stockBack.setBackTime(new Date());
            stockBack.setCreateTime(new Date());
            stockBack.setNum(orderItem.getNum());
            stockBack.setStatus("0");

            stockBackMapper.insert(stockBack);
        }
    }


    /**
     * 执行库存恢复
     */
    @Override
    @Transactional
    public void doBack() {

        StockBack stockBack0 = new StockBack();
        stockBack0.setStatus("0");

        //获取回滚的库存信息
        List<StockBack> stockBackList = stockBackMapper.select(stockBack0);
        for (StockBack stockBack : stockBackList) {

            //执行库存恢复
            skuMapper.deductionStock(stockBack0.getSkuId(), -stockBack.getNum());//库存增加
            skuMapper.addSaleNum(stockBack0.getSkuId(), -stockBack.getNum()); //销量扣减

            stockBack.setStatus("1");
            stockBack.setBackTime(new Date());
            stockBackMapper.updateByPrimaryKey(stockBack);
        }
    }
}
