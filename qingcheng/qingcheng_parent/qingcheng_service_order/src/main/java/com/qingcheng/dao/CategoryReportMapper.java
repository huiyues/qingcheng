package com.qingcheng.dao;


import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.pojo.order.DataReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CategoryReportMapper extends Mapper<CategoryReport> {

    @Select("select \n" +
            "\toi.category_id1 categoryId1,\n" +
            "\toi.category_id2 categoryId2,\n" +
            "\toi.category_id3 categoryId3,\n" +
            "  DATE_FORMAT(o.pay_time,'%Y-%m-%d') as countDate,\n" +
            "\tSUM(oi.num) num ,sum(oi.pay_money) money\n" +
            "from\n" +
            "\ttb_order o,tb_order_item oi \n" +
            "where \n" +
            "\to.id = oi.order_id and o.pay_status = '1' and o.is_delete = '0' and DATE_FORMAT(o.pay_time,'%Y-%m-%d')<=#{date}\n" +
            "group by \n" +
            "\toi.category_id1,\n" +
            "\toi.category_id2,\n" +
            "\toi.category_id3,\n" +
            "\tDATE_FORMAT(o.pay_time,'%Y-%m-%d')")
    List<CategoryReport> yearDate(@Param("date") LocalDate date);


    @Select("SELECT \n" +
            "\tt.category_id1 categoryId1,c.name categoryName,\n" +
            "\tsum(t.money) moneys,SUM(t.num) sums\n" +
            "FROM \n" +
            "\t`tb_category_report` t,v_category c\n" +
            "where \n" +
            "\t t.category_id1 = c.id and t.count_date >= #{date1} and t.count_date <= #{date2}\n" +
            "GROUP BY\n" +
            "\tt.category_id1,c.name\n" +
            "\n")
    List<Map> countCategory(@Param("date1") String date1,@Param("date2") String date2);


     /**
      * @UpdateUser:     heiye
      * @UpdateRemark:   查询需要的数据
      */

     @Select("SELECT \n" +
             "count(DISTINCT o.username) placeNum, \n" +
             "sum(o.total_num) orderNum, \n" +
             "sum(oi.num) quantityNum, \n" +
             "sum(o.total_money) belowMoney, \n" +
             "sum(o.pay_money) payMoney, \n" +
             "(o.total_money/COUNT(DISTINCT o.username)) perMoney\n" +
             "FROM `tb_order` o,tb_order_item oi \n" +
             "where \n" +
             "o.id = oi.order_id\n"
             )
     DataReport findDate();


     @Select("SELECT \n" +
             "sum(o.total_num) validNum \n" +
             "FROM `tb_order` o,tb_order_item oi \n" +
             "where \n" +
             "o.id = oi.order_id and o.is_delete='0' \n" )
     DataReport findDate2();


     @Select("SELECT \n" +
             "sum(oi.price) retreatMoney \n" +
             "FROM `tb_order` o,tb_order_item oi \n" +
             "where \n" +
             "o.id = oi.order_id and o.pay_status='2'\n" )
     DataReport findDate3();


     @Select("SELECT \n" +
             "count(o.id) payPerson, -- 付款人数\n" +
             "sum(o.total_num) payOrder, -- 付款订单数\n" +
             "sum(oi.num) payNum -- 付款件数\n" +
             "FROM tb_order o, tb_order_item oi \n" +
             "where \n" +
             "o.id = oi.order_id and o.pay_status=1 ")
     DataReport findDate4();
}
