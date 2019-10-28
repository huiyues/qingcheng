package com.qingcheng.dao;

import com.qingcheng.pojo.system.ResourceAndMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface ResourceAndMenuMapper extends Mapper<ResourceAndMenu>{

    //根据资源id查询菜单id
    @Select("select menu_id from tb_resource_menu where resource_id in (#{resourceId})")
    String findByResourceId(@Param("resourceId") Integer resourceIds);

}
