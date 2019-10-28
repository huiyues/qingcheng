package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.ResourceMapper;
import com.qingcheng.dao.RoleAndResourceMapper;
import com.qingcheng.dao.RoleMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.*;
import com.qingcheng.service.system.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;


    @Autowired
    private RoleAndResourceMapper roleAndResourceMapper;


    @Autowired
    private ResourceMapper resourceMapper;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Role> findAll() {
        return roleMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Role> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Role> roles = (Page<Role>) roleMapper.selectAll();
        return new PageResult<Role>(roles.getTotal(), roles.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Role> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return roleMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Role> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Role> roles = (Page<Role>) roleMapper.selectByExample(example);
        return new PageResult<Role>(roles.getTotal(), roles.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Role findById(Integer id) {
        return roleMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param role
     */
    public int add(Role role) {
        roleMapper.insert(role);

        return role.getId();
    }

    /**
     * 修改
     *
     * @param role
     */
    public void update(Role role) {
        roleMapper.updateByPrimaryKeySelective(role);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(Integer id) {
        roleMapper.deleteByPrimaryKey(id);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 中间表添加
     */
    @Override
    public void roleAndResource(int id, Integer id1) {
        RoleAndResource roleAndResource = new RoleAndResource();
        roleAndResource.setRoleId(id);
        roleAndResource.setResourceId(id1);

        roleAndResourceMapper.insert(roleAndResource);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据id查询详细信息
     */
    @Override
    public Roles findByRoleId(String roleId) {
        Role role = roleMapper.selectByPrimaryKey(roleId);

        if (role != null) {
            Example example = new Example(RoleAndResource.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("roleId", role.getId());

            List<RoleAndResource> roleAndResources = roleAndResourceMapper.selectByExample(example);

            Example example1 = new Example(Resource.class);
            for (RoleAndResource roleAndResource : roleAndResources) {
                Example.Criteria criteria1 = example1.createCriteria();
                criteria1.andEqualTo("id", roleAndResource.getResourceId());
            }

            List<Resource> resourceList = resourceMapper.selectByExample(example1);

            Roles roles = new Roles();
            roles.setRole(role);
            roles.setResourceList(resourceList);

            return roles;
        } else {
            throw new RuntimeException("角色id不能为空!");
        }
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Role.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 角色名称
            if (searchMap.get("name") != null && !"".equals(searchMap.get("name"))) {
                criteria.andLike("name", "%" + searchMap.get("name") + "%");
            }

            // ID
            if (searchMap.get("id") != null) {
                criteria.andEqualTo("id", searchMap.get("id"));
            }

        }
        return example;
    }

}
