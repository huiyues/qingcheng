package com.qingcheng.dao;

import com.qingcheng.pojo.system.Admin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

public interface AdminMapper extends Mapper<Admin> {


    //根据用户名查询用户id
    @Select("select id from tb_admin where login_name = #{name}")
    Admin findByName(@Param("name") String name);
}
