package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.Resource;
import com.qingcheng.service.system.AdminService;
import com.qingcheng.service.system.ResourceService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class UserDetailServiceImp implements UserDetailsService {

    @Reference
    private AdminService adminService;

    @Reference
    private ResourceService resourceService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //构建map集合封装查询条件
        Map<String, Object> map = new HashMap<>();
        map.put("loginName", username);
        map.put("status", "1");
        List<Admin> list = adminService.findList(map);
        if (list == null) {
            return null;
        }
        // 通过用户名和用户状态查询用户信息
        return new User(username, list.get(0).getPassword(), getRoleList(username));
    }


    public List<GrantedAuthority> getRoleList(String username) {
        List<GrantedAuthority> authorityList = new ArrayList<>();

        List<Resource> resourceList = resourceService.findByNameResource(username);
        for (Resource resource : resourceList) {
            authorityList.add(new SimpleGrantedAuthority(resource.getResKey()));
        }
        return authorityList;
    }
}
