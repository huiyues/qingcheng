package com.qingcheng.service.order;

import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.pojo.order.DataReport;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CategoryReportService {

    List<CategoryReport> yearDate(LocalDate date);

    void createDate();

    List<Map> findCountDate(String date1, String date2);

    void DataReport();

    List<DataReport> findDataReport();
}
