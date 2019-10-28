package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.utils.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = CategoryService.class)
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Category> findAll() {
        return categoryMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Category> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Category> categorys = (Page<Category>) categoryMapper.selectAll();
        return new PageResult<Category>(categorys.getTotal(), categorys.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Category> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return categoryMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Category> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Category> categorys = (Page<Category>) categoryMapper.selectByExample(example);
        return new PageResult<Category>(categorys.getTotal(), categorys.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Category findById(Integer id) {
        return categoryMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param category
     */
    public void add(Category category) {
        categoryMapper.insert(category);
        saveCategoryTreeCache();//当数据更新之后重新缓存预热
    }

    /**
     * 修改
     *
     * @param category
     */
    public void update(Category category) {
        categoryMapper.updateByPrimaryKeySelective(category);
        saveCategoryTreeCache();//当数据更新之后重新缓存预热
    }

    /**
     * 删除
     *
     * @param id
     */
    @Transactional
    public void delete(Integer id) {
        //根据id查询是否存在下级分类
        Example example = new Example(Category.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("parentId", id);
        int count = categoryMapper.selectCountByExample(example);

        //如果有则抛出异常
        if (count > 0) {
            throw new RuntimeException("不能删除含有分类的选项");
        }
        categoryMapper.deleteByPrimaryKey(id);
        saveCategoryTreeCache();//当数据更新之后重新缓存预热
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 树形结构的菜单查询
     */
    @Override
    public List<Map> categoryTree() {
        //从缓存中获取分类的树形菜单
        List<Map> mapList = (List<Map>) redisTemplate.boundValueOps(CacheKey.CATEGORY_TREE).get();
        return mapList;
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 将树形菜单存入到redis缓存中
     */

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void saveCategoryTreeCache() {
        //查询所有的菜单
        Example example = new Example(Category.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("isShow", "1");
        example.setOrderByClause("seq");

        List<Category> categories = categoryMapper.selectByExample(example);
        List<Map> categoryTree = findByCategoryTree(categories, 0);

        //将树形存入到缓存中
        if (categoryTree != null) {
            redisTemplate.boundValueOps(CacheKey.CATEGORY_TREE).set(categoryTree);
        }
    }


    public List<Map> findByCategoryTree(List<Category> adList, Integer parentId) {
        List<Map> mapList = new ArrayList<>();
        for (Category ad : adList) {
            if (parentId.equals(ad.getParentId())) {
                Map map = new HashMap();
                map.put("name", ad.getName());
                map.put("menus", findByCategoryTree(adList, ad.getId()));

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
        Example example = new Example(Category.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 分类名称
            if (searchMap.get("name") != null && !"".equals(searchMap.get("name"))) {
                criteria.andLike("name", "%" + searchMap.get("name") + "%");
            }
            // 是否显示
            if (searchMap.get("isShow") != null && !"".equals(searchMap.get("isShow"))) {
                criteria.andLike("isShow", "%" + searchMap.get("isShow") + "%");
            }
            // 是否导航
            if (searchMap.get("isMenu") != null && !"".equals(searchMap.get("isMenu"))) {
                criteria.andLike("isMenu", "%" + searchMap.get("isMenu") + "%");
            }

            // 分类ID
            if (searchMap.get("id") != null) {
                criteria.andEqualTo("id", searchMap.get("id"));
            }
            // 商品数量
            if (searchMap.get("goodsNum") != null) {
                criteria.andEqualTo("goodsNum", searchMap.get("goodsNum"));
            }
            // 排序
            if (searchMap.get("seq") != null) {
                criteria.andEqualTo("seq", searchMap.get("seq"));
            }
            // 上级ID
            if (searchMap.get("parentId") != null) {
                criteria.andEqualTo("parentId", searchMap.get("parentId"));
            }
            // 模板ID
            if (searchMap.get("templateId") != null) {
                criteria.andEqualTo("templateId", searchMap.get("templateId"));
            }

        }
        return example;
    }

}
