package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.qingcheng.dao.CategoryReportMapper;
import com.qingcheng.dao.DataReportMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.pojo.order.DataReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service(interfaceClass = CategoryReportService.class)
public class CategoryReportServiceImp implements CategoryReportService {


    @Autowired
    private CategoryReportMapper categoryReportMapper;


    @Autowired
    private DataReportMapper dataReportMapper;

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 获取昨天统计的所有数据
     */
    @Override
    public List<CategoryReport> yearDate(LocalDate date) {
 
        return categoryReportMapper.yearDate(date);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 将昨天的数据添加到数据库中
     */
    @Override
    public void createDate() {
        //获取昨天的时间
        LocalDate localDate = LocalDate.now().minusDays(1);

        List<CategoryReport> categoryReports = categoryReportMapper.yearDate(localDate);
        for (CategoryReport categoryReport : categoryReports) {
            categoryReportMapper.insertSelective(categoryReport);
        }
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 查询单个分类的详细数据
     */
    @Override
    public List<Map> findCountDate(String date1, String date2) {
        List<Map> Map = categoryReportMapper.countCategory(date1, date2);
        return Map;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 流量数据统计
     */
    @Override
    public void DataReport() {

        DataReport dataReport = new DataReport();
        dataReport.setId(1);

        DataReport date = categoryReportMapper.findDate();
        DataReport date2 = categoryReportMapper.findDate2();
        DataReport date3 = categoryReportMapper.findDate3();
        DataReport date4 = categoryReportMapper.findDate4();

        dataReport.setBrowseNum(1010);
        dataReport.setPayMoney(date.getPayMoney());
        dataReport.setBelowMoney(date.getBelowMoney());
        dataReport.setPlaceNum(date.getPlaceNum());
        dataReport.setOrderNum(date.getOrderNum());
        dataReport.setQuantityNum(date.getQuantityNum());
        dataReport.setPerMoney(date.getPerMoney());

        dataReport.setPayNum(date4.getPayNum());
        dataReport.setPayPerson(date4.getPayPerson());
        dataReport.setPayOrder(date4.getPayOrder());

        if (date3 != null) {
            dataReport.setRetreatMoney(date3.getRetreatMoney());
        }
        dataReport.setRetreatMoney(0);
        dataReport.setValidNum(date2.getValidNum());

        dataReportMapper.update(dataReport);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 查询所有超时数据
     */
    @Override
    public List<DataReport> findDataReport() {

        List<DataReport> dataReports = dataReportMapper.selectAll();
        return dataReports;
    }
}
