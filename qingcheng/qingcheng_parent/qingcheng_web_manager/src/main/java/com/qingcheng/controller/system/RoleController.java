package com.qingcheng.controller.system;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.system.Admins;
import com.qingcheng.pojo.system.Resource;
import com.qingcheng.pojo.system.Role;
import com.qingcheng.pojo.system.Roles;
import com.qingcheng.service.system.RoleService;
import com.qingcheng.utils.BCryptPasswordEncoderUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/role")
public class RoleController {

    @Reference
    private RoleService roleService;

    @GetMapping("/findAll")
    public List<Role> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Role> findPage(int page, int size) {
        return roleService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Role> findList(@RequestBody Map<String, Object> searchMap) {
        return roleService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Role> findPage(@RequestBody Map<String, Object> searchMap, int page, int size) {
        return roleService.findPage(searchMap, page, size);
    }

    @GetMapping("/findById")
    public Role findById(Integer id) {
        return roleService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Role role) {
        roleService.add(role);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Role role) {
        roleService.update(role);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(Integer id) {
        roleService.delete(id);
        return new Result();
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 添加角色以及权限中间表数据
     */
    @GetMapping("/addRoleId")
    public Result add(@RequestParam(value = "roleId") String roleId, @RequestParam(value = "resIds") String[] resIds) {
        //角色中间表进行添加
        for (String resource : resIds) {
            int resId = Integer.parseInt(resource);
            int roId = Integer.parseInt(roleId);
            roleService.roleAndResource(roId, resId);
        }
        return new Result();
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据角色id查询所有的资源详细信息
     */

    @GetMapping("/findByRoleId")
    public Roles findByRoleId(String id) {
        Roles roles = roleService.findByRoleId(id);
        return roles;
    }
}
