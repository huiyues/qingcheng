package com.qingcheng.dao;

import com.qingcheng.pojo.system.Menu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

public interface MenuMapper extends Mapper<Menu> {

    //根据菜单id查询parentId
    @Select("select parent_id from tb_menu where id in(#{parentId})")
    String findByParentId(@Param("parentId") String parentIds);


    //根据parentId查询一级菜单
    @Select("select * from tb_menu where id in(#{parentId})")
    Menu findByParentId2(@Param("parentId") String parentIds2i);
}
