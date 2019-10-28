package com.qingcheng.service.system;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.Admins;
import com.qingcheng.pojo.system.Menu;

import java.util.*;

/**
 * admin业务逻辑层
 */
public interface AdminService {


    public List<Admin> findAll();


    public PageResult<Admin> findPage(int page, int size);


    public List<Admin> findList(Map<String,Object> searchMap);


    public PageResult<Admin> findPage(Map<String,Object> searchMap,int page, int size);


    public Admins findById(Integer id);

    public Integer add(Admin admin);


    public void update(Admin admin,String[] roleIds);


    public void delete(Integer id);

    String findByPassword(String username);

    void updatePassword(String username, String newPassword);

    void adminIdAndRoleId(int id, int roleId);

    List<Menu> fndByNameAndMenu(String name);
}
