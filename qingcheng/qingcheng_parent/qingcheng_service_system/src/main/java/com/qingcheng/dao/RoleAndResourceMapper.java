package com.qingcheng.dao;

import com.qingcheng.pojo.system.RoleAndResource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface RoleAndResourceMapper extends Mapper<RoleAndResource> {

    //根据角色id查询资源id
    @Select("select resource_id from tb_role_resource where role_id in (#{roleId})")
    Integer[] findByRoleId(@Param("roleId") Integer id);

}
