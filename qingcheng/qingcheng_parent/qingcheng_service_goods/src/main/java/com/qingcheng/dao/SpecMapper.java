package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Spec;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SpecMapper extends Mapper<Spec> {


    @Select("SELECT `name`,`options` FROM `tb_spec` where template_id in(\n" +
            "\t\tselect template_id from tb_category where name = #{name}\n" +
            ")ORDER BY seq;")
    List<Map> findByCategoryName(@Param("name")String categoryName);
}
