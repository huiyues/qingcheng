package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.*;

import com.qingcheng.service.system.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.Id;
import java.util.*;

@Service(interfaceClass = AdminService.class)
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminAndRoleMapper adminAndRoleMapper;


    @Autowired
    private RoleMapper roleMapper;


    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private MenuMapper menuMapper;


    @Autowired
    private ResourceAndMenuMapper resourceAndMenuMapper;

    @Autowired
    private RoleAndResourceMapper roleAndResourceMapper;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Admin> findAll() {
        return adminMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Admin> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Admin> admins = (Page<Admin>) adminMapper.selectAll();
        return new PageResult<Admin>(admins.getTotal(), admins.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Admin> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return adminMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Admin> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Admin> admins = (Page<Admin>) adminMapper.selectByExample(example);
        return new PageResult<Admin>(admins.getTotal(), admins.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Admins findById(Integer id) {
        Admin admin = adminMapper.selectByPrimaryKey(id);

        if (admin != null) {
            Example example = new Example(AdminAndRole.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("adminId", admin.getId());

            Example example1 = new Example(Role.class);

            List<AdminAndRole> adminAndRoles = adminAndRoleMapper.selectByExample(example);
            for (AdminAndRole adminAndRole : adminAndRoles) {

                Example.Criteria criteria1 = example1.createCriteria();
                criteria1.andEqualTo("id", adminAndRole.getRoleId());

            }
            List<Role> roleList = roleMapper.selectByExample(example1);

            Admins admins = new Admins();
            admins.setAdmin(admin);
            admins.setRoleList(roleList);

            return admins;
        } else {
            throw new RuntimeException("管理员id不能为空!");
        }
    }

    /**
     * 新增
     *
     * @param admin
     */
    public Integer add(Admin admin) {

        adminMapper.insert(admin);
        return admin.getId();
    }

    /**
     * 修改
     *
     * @param admins
     */
    @PreAuthorize("hasAnyAuthority('brand')")
    public void update(Admin admin,String[] roleIds) {
        adminMapper.updateByPrimaryKeySelective(admin);

        AdminAndRole adminAndRole = new AdminAndRole();

        for (String role : roleIds) {
            int roleId = Integer.parseInt(role);
            adminAndRole.setAdminId(admin.getId());
            adminAndRole.setRoleId(roleId);
            adminAndRoleMapper.insert(adminAndRole);
        }
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(Integer id) {
        adminMapper.deleteByPrimaryKey(id);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据用户获取密码
     */
    @Override
    public String findByPassword(String username) {
        Example example = new Example(Admin.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("loginName", username);

        Admin admin = adminMapper.selectOneByExample(example);
        return admin.getPassword();
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据旧密码修改新密码
     */
    @Override
    public void updatePassword(String username, String newPassword) {
        Admin admin = new Admin();
        admin.setPassword(newPassword);

        Example example = new Example(Admin.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("loginName", username);

        adminMapper.updateByExampleSelective(admin, example);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 添加管理员对应的角色到中间表
     */
    @Override
    public void adminIdAndRoleId(int id, int roleId) {
        AdminAndRole adminAndRole = new AdminAndRole();
        adminAndRole.setAdminId(id);
        adminAndRole.setRoleId(roleId);

        adminAndRoleMapper.insert(adminAndRole);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据管理名称查询所拥有的权限菜单
     */
    @Override
    public List<Menu> fndByNameAndMenu(String name) {
        //用于封装查询出来的菜单级数
        List<Menu> menuList = new ArrayList<>();

        //通过名称获取对应的所有资源id
        Admin admin = adminMapper.findByName(name);
        //根据管理员id获取roleId
        Integer[] roleIds = adminAndRoleMapper.findByAdminId(admin.getId());
        //根据角色id查询资源id
        Integer[] resourceIds = null;
        for (Integer roleId : roleIds) {
            resourceIds = roleAndResourceMapper.findByRoleId(roleId);
        }

        //根据资源id查询对应的菜单id
        List<String> menuIds = new ArrayList<>();//将菜单id封装方便查询菜单
        for (Integer resourceId : resourceIds) {
            String menuId = resourceAndMenuMapper.findByResourceId(resourceId);
            menuIds.add(menuId);
        }

        //根据菜单id查询菜单parentId
        List<String> parentIds = new ArrayList<>();
        for (String menuId : menuIds) {
            String parentIdi = menuMapper.findByParentId(menuId);
            parentIds.add(parentIdi);
        }

        //查询一级菜单id
        List<String> parentIds2 = new ArrayList<>();
        for (String parentId : parentIds) {
            String parentIds2i = menuMapper.findByParentId(parentId);
            parentIds2.add(parentIds2i);
        }


        //查询一级菜单结果
        Menu menu = menuMapper.findByParentId2(parentIds2.get(0));
        //封装一级菜单
        menuList.add(menu);


        //查询二级菜单
        Example example1 = new Example(Menu.class);
        Example.Criteria criteria1 = example1.createCriteria();
        criteria1.andIn("id", parentIds);
        List<Menu> menu1 = menuMapper.selectByExample(example1);
        //封装二级菜单
        for (Menu menu2 : menu1) {
            menuList.add(menu2);
        }

        //查询三级菜单
        Example example2 = new Example(Menu.class);
        Example.Criteria criteria2 = example2.createCriteria();
        criteria2.andIn("id", menuIds);
        List<Menu> menu2 = menuMapper.selectByExample(example2);
        //封装三级菜单
        for (Menu menu3 : menu2) {
            menuList.add(menu3);
        }

        return menuList;
    }

   /* public List<Map> findByMenu(List<Menu> menuList,String menuId){
        List<Map> mapList = new ArrayList<>();

        for (Menu menu : menuList) {
            if (menuId.equals(menu.getId())){
                Map map = new HashMap();
                map.put("parentId",findByMenu(menuList,menu.getParentId()));
                mapList.add(map);
            }
        }
        return mapList;
    }*/


    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Admin.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 用户名
            if (searchMap.get("loginName") != null && !"".equals(searchMap.get("loginName"))) {
                criteria.andEqualTo("loginName", searchMap.get("loginName"));
            }
            // 密码
            if (searchMap.get("password") != null && !"".equals(searchMap.get("password"))) {
                criteria.andLike("password", "%" + searchMap.get("password") + "%");
            }
            // 状态
            if (searchMap.get("status") != null && !"".equals(searchMap.get("status"))) {
                criteria.andEqualTo("status", searchMap.get("status"));
            }

            // id
            if (searchMap.get("id") != null) {
                criteria.andEqualTo("id", searchMap.get("id"));
            }

        }
        return example;
    }

}
