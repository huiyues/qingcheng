package com.qingcheng.controller.system;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.system.*;
import com.qingcheng.service.system.AdminService;
import com.qingcheng.utils.BCryptPasswordEncoderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/admin")
@EnableDubboConfig
public class AdminController {

    @Reference
    private AdminService adminService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @GetMapping("/findAll")
    public List<Admin> findAll() {
        return adminService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Admin> findPage(int page, int size) {
        return adminService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Admin> findList(@RequestBody Map<String, Object> searchMap) {
        return adminService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Admin> findPage(@RequestBody Map<String, Object> searchMap, int page, int size) {
        return adminService.findPage(searchMap, page, size);
    }

    @GetMapping("/findById")
    public Admins findById(Integer id) {
        return adminService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Admin admin, @RequestParam(value = "roleIds") String[] roleIds) {
        admin.setPassword(BCryptPasswordEncoderUtils.encodePassword(admin.getPassword()));//密码加密
        //添加管理员同时将角色中间表进行添加
        int id = adminService.add(admin);

        for (String roleId : roleIds) {
            int rid = Integer.parseInt(roleId);
            adminService.adminIdAndRoleId(id, rid);
        }
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Admin admin, @RequestParam(value = "roleIds") String[] roleIds) {
        admin.setPassword(BCryptPasswordEncoderUtils.encodePassword(admin.getPassword()));
        adminService.update(admin, roleIds);

        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(Integer id) {
        adminService.delete(id);
        return new Result();
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 修改密码
     */

    //修改密码
    @GetMapping("/update")
    public void update(@RequestParam(value = "oldPassword") String oldPassword,
                       @RequestParam(value = "newPassword") String newPassword) throws IOException, ServletException {

        //获取用户信息
        SecurityContext context = SecurityContextHolder.getContext();
        User user = (User) context.getAuthentication().getPrincipal();
        String username = user.getUsername();

        //获取到用户密码
        String password = adminService.findByPassword(username);

        //让用户密码与用户输入的密码进行比对
        boolean matches = BCryptPasswordEncoderUtils.matches(oldPassword, password);
        if (matches) {
            //对新密码进行加密
            newPassword = BCryptPasswordEncoderUtils.encodePassword(newPassword);
            adminService.updatePassword(username, newPassword);
            //成功则重新登录
            request.getRequestDispatcher("/login.html").forward(request, response);
        } else {
            //修改失败页面
            response.sendRedirect("/system/updatePassword.html");
        }
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据管理员名称查询所有能访问的权限菜单
     */

    @GetMapping("/findByNameAndMenu")
    public List<Map> fndByNameAndMenu() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Menu> menuList = adminService.fndByNameAndMenu(name);
        List<Map> mapList = findByMenuList(menuList, "0");
        return mapList;
    }

    public List<Map> findByMenuList(List<Menu> menuList, String parentId) {
        List<Map> mapList = new ArrayList<>();
        //因为前端数据接收不同所以封装成map数据比较方便
        for (Menu menu : menuList) {
            if (parentId.equals(menu.getParentId())) {
                Map map = new HashMap();
                map.put("path", menu.getId());
                map.put("title", menu.getName());
                map.put("icon", menu.getIcon());
                map.put("linkUrl", menu.getUrl());
                map.put("children", findByMenuList(menuList, menu.getId()));
                mapList.add(map);
            }
        }
        return mapList;
    }
}
