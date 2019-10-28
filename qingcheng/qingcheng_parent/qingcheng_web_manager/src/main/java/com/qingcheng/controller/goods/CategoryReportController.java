package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.pojo.order.DataReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categoryReport")
@EnableDubboConfig
public class CategoryReportController {

    @Reference
    private CategoryReportService categoryReportService;


    @GetMapping("/yearDate")
    public List<CategoryReport> yearDate() {
        //获取昨天的时间
        LocalDate localDate = LocalDate.now().minusDays(1);
        List<CategoryReport> categoryReports = categoryReportService.yearDate(localDate);

        return categoryReports;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据日期条件获取一级分类的所有数据
     */

    @GetMapping("/findCountDate")
    public List<Map> findCountDate(String date1, String date2) {
        List<Map> map = categoryReportService.findCountDate(date1, date2);
        return map;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 流量数据统计
     */

    @GetMapping("/findDataReport")
    public List<DataReport> findDataReport() {
        List<DataReport> dataReport = categoryReportService.findDataReport();
        return dataReport;
    }
}
