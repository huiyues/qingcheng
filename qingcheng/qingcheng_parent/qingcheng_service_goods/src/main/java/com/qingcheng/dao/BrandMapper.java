package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface BrandMapper extends Mapper<Brand> {

    @Select("SELECT name,image FROM `tb_brand` where id in(\n" +
            "\tselect brand_id from tb_category_brand where category_id in(\n" +
            "\t\tselect id from tb_category where name = #{name}\n" +
            "\t)\n" +
            ") order by seq;")
    List<Map> findByMap(@Param("name")String categoryName);
}
