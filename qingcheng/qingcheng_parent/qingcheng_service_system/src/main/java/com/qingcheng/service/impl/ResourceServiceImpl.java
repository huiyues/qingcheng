package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AdminAndRoleMapper;
import com.qingcheng.dao.AdminMapper;
import com.qingcheng.dao.ResourceMapper;
import com.qingcheng.dao.RoleAndResourceMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.Menu;
import com.qingcheng.pojo.system.Resource;
import com.qingcheng.service.system.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;


    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminAndRoleMapper adminAndRoleMapper;

    @Autowired
    private RoleAndResourceMapper roleAndResourceMapper;
    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Resource> findAll() {
        return resourceMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Resource> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Resource> resources = (Page<Resource>) resourceMapper.selectAll();
        return new PageResult<Resource>(resources.getTotal(), resources.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Resource> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return resourceMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Resource> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Resource> resources = (Page<Resource>) resourceMapper.selectByExample(example);
        return new PageResult<Resource>(resources.getTotal(), resources.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Resource findById(Integer id) {
        return resourceMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param resource
     */
    public void add(Resource resource) {
        resourceMapper.insert(resource);
    }

    /**
     * 修改
     *
     * @param resource
     */
    public void update(Resource resource) {
        resourceMapper.updateByPrimaryKeySelective(resource);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(Integer id) {
        resourceMapper.deleteByPrimaryKey(id);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据用户名查询所有 资源sky信息
     */

    public List<Resource> findByNameResource(String username) {
        //用于封装查询出来的资源路径
        List<Resource> resourceList = new ArrayList<>();

        //通过名称获取对应的管理员id
        Admin admin = adminMapper.findByName(username);
        //根据管理员id获取角色roleId
        Integer[] roleIds = adminAndRoleMapper.findByAdminId(admin.getId());
        //根据角色id查询资源id
        Integer[] resourceIds = null;
        for (Integer roleId : roleIds) {
            resourceIds = roleAndResourceMapper.findByRoleId(roleId);
        }

        //根据所有资源id查询所有资源路径
        for (Integer resourceId : resourceIds) {
            Resource resource = new Resource();
            String reskey = resourceMapper.findByResourceIds(resourceId);
            resource.setResKey(reskey);
            resourceList.add(resource);
        }

        return resourceList;
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 树形资源表查询
     */
    @Override
    public List<Map> findByResource() {
        List<Resource> resources = resourceMapper.selectAll();
        List<Map> mapList = findByResourceParentId(resources, 0);
        return mapList;
    }

    public List<Map> findByResourceParentId(List<Resource> resources, Integer parentId) {
        List<Map> mapList = new ArrayList<>();

        for (Resource resource : resources) {
            if (resource.getParentId().equals(parentId)) {
                Map map = new HashMap();
                map.put("id", resource.getId());
                map.put("resKey", resource.getResKey());
                map.put("resName", resource.getResName());

                map.put("parentId", findByResourceParentId(resources, resource.getId()));
                mapList.add(map);
            }
        }
        return mapList;
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Resource.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // res_key
            if (searchMap.get("resKey") != null && !"".equals(searchMap.get("resKey"))) {
                criteria.andLike("resKey", "%" + searchMap.get("resKey") + "%");
            }
            // res_name
            if (searchMap.get("resName") != null && !"".equals(searchMap.get("resName"))) {
                criteria.andLike("resName", "%" + searchMap.get("resName") + "%");
            }

            // id
            if (searchMap.get("id") != null) {
                criteria.andEqualTo("id", searchMap.get("id"));
            }
            // parent_id
            if (searchMap.get("parentId") != null) {
                criteria.andEqualTo("parentId", searchMap.get("parentId"));
            }

        }
        return example;
    }

}
