package com.qingcheng.dao;

import com.qingcheng.pojo.system.Resource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface ResourceMapper extends Mapper<Resource> {


    @Select("SELECT id,res_key FROM `tb_resource` where id in(\n" +
            "\tselect resource_id from tb_role_resource where role_id in(\n" +
            "\t\tselect role_id from tb_admin_role where admin_id in(\n" +
            "\t\t\tselect admin_id from tb_admin where login_name = #{loginName} \n" +
            "\t\t)\n" +
            "\t)\n" +
            ");")
    List findByNameResource(String loginName);


    //根据资源id查询所有的资源路径
    @Select(" select res_key FROM `tb_resource` where id in(#{resourceId})")
    String findByResourceIds(@Param("resourceId") Integer resourceId);
}
