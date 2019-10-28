package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SkuTask {


    @Reference
    private StockBackService stockBackService;

    /**
     * 定时执行库存回滚扫描
     */
    @Scheduled(cron = "0 0 0/5 * * ?")
    public void SkuBackTask() {
        stockBackService.doBack();
        System.out.println("======== skuBackTask Inventory restoration completed =========");
    }
}
