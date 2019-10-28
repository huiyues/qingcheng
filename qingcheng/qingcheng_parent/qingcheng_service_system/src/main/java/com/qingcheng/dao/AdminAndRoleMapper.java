package com.qingcheng.dao;

import com.qingcheng.pojo.system.AdminAndRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;


public interface AdminAndRoleMapper extends Mapper<AdminAndRole> {


    //根据用户id查询角色id
    @Select("select role_id from tb_admin_role where admin_id in (#{adminId})")
    Integer[] findByAdminId(@Param("adminId") Integer id);


}
