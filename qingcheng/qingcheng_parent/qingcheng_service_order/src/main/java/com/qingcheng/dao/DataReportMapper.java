package com.qingcheng.dao;


import com.qingcheng.pojo.order.DataReport;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

public interface DataReportMapper extends Mapper<DataReport>{

    @Update("update tb_data_report set\n" +
            " browse_num=#{browseNum},place_num=#{placeNum},\n" +
            " order_num=#{orderNum},quantity_num=#{quantityNum},\n" +
            " below_money=#{belowMoney},retreat_money=#{retreatMoney},\n" +
            " pay_person=#{payPerson},pay_order=#{payOrder},\n" +
            " pay_num=#{payNum},pay_money=#{payMoney},per_money=#{perMoney},\n" +
            " valid_num=#{validNum} where id = #{id}")
    void update(DataReport dataReport);
}
