package com.qingcheng.controller.order;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.service.order.CategoryReportService;
import com.qingcheng.service.order.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@EnableDubboConfig
public class OrderTask {

    @Reference
    private OrderService orderService;

    @Reference
    private CategoryReportService categoryReportService;

     /**
      * @UpdateUser:     heiye
      * @UpdateRemark:   每两分钟记录超时的订单并进行关闭
      */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void orderTimeLog(){
        orderService.orderTimeLogIc();
        System.out.println("Order timeout checks are performed every 5 minutes"+new Date());
    }

     /**
      * @UpdateUser:     heiye
      * @UpdateRemark:   生成昨天的数据存入到数据库中
      */

     @Scheduled(cron = "0/30 * * * * ?") //每天的凌晨1点
     public void createDate(){
         categoryReportService.createDate();

         categoryReportService.DataReport();
         System.out.println("Generate yesterday's data statistics...");
     }


    @Scheduled(cron = "0/10 * * * * ?") //每天的凌晨1点
    public void dataReport(){
        categoryReportService.DataReport();
        System.out.println("Generate DataReport's data statistics...");
    }
}
