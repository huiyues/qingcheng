package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.pojo.log.SpuLog;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpecService;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.service.provider.IndexProviderService;
import com.qingcheng.service.provider.PageProviderService;
import com.qingcheng.utils.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.Table;
import java.util.*;

@Service(interfaceClass = SpuService.class)
@Transactional
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;


    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private IdWorker idWorker;


    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;


    @Autowired
    private SpuLogMapper logMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private SkuService skuService;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(), spus.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(), spus.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     *
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(String id) {
        //获取到当前被删除的spuid查询到对应的sku
        Map ma = new HashMap();
        ma.put("spuId", id);
        List<Sku> skuList = skuService.findList(ma);
        for (Sku sku : skuList) {
            skuService.delPriceToRedis(sku.getId());
        }

        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 保存商品
     */
    @Override
    @Transactional
    public void saveSpu(Goods goods) {
        //获取spu
        Spu spu = goods.getSpu();
        //保存+修改(判断spuId是否为空)
        if (spu.getId() == null) {
            //设置spuId
            spu.setId(idWorker.nextId() + "");
            spuMapper.insert(spu);
        } else {
            //修改
            Example example = new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId", spu.getId());

            skuMapper.deleteByExample(example);
            spuMapper.updateByPrimaryKeySelective(spu);
        }

        Date date = new Date();
        //获取分类名称
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());
        //获取多个sku
        List<Sku> skuList = goods.getSkuList();
        for (Sku sku : skuList) {
            if (sku.getId() == null) { //新增
                //设置skuId和spuId
                sku.setId(idWorker.nextId() + "");
                sku.setCreateTime(date);//创建时间
            }
            sku.setSpuId(spu.getId());
            //设置sku名称 name = spu名称 + 规格值
            String name = spu.getName();

            //如果没有设置规格则默认设置为空的json格式字符串
            if (sku.getSpec() == null || "".equals(sku.getSpec())) {
                sku.setSpec("{}");
            }
            //将前端传过来的规格json数据转换成map集合
            Map<String, String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            for (String value : specMap.values()) {
                name += "" + value;
            }

            sku.setName(name);
            sku.setCategoryId(spu.getCategory3Id());//分类id
            sku.setCategoryName(category.getName());//分类名称
            sku.setCommentNum(0);//评论数
            sku.setUpdateTime(date);//更新时间
            sku.setSaleNum(0);//销售数量

            skuMapper.insert(sku);

            //将更新后的价格更新到缓存中
            skuService.savePriceToRedisById(sku.getId(), sku.getPrice());
        }

        //建立分类与品牌的关系
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setBrandId(spu.getBrandId());
        categoryBrand.setCategoryId(spu.getCategory3Id());
        int count = categoryBrandMapper.selectCount(categoryBrand);

        //如果分类数据中不存在相同的数据则添加
        if (count == 0) {
            categoryBrandMapper.insert(categoryBrand);
        }
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据id查询spu详细信息
     */
    @Override
    public Goods findGoodsById(String id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //查询sku
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId", id);
        List<Sku> skuList = skuMapper.selectByExample(example);

        //封装Goods
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);

        return goods;
    }


    @Reference
    private IndexProviderService indexProviderService;

    @Reference
    private PageProviderService pageProviderService;


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 修改商品审核状态，以及自动上架，审核记录和商品日志
     */
    @Override
    @Transactional
    public void audit(String id, String status, String message) {
        //获取实例对象直接对商品进行修改这样更节省内存
        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setStatus(status);
        //判断审核状态
        if ("1".equals(status)) {
            //设置自动上架
            spu.setIsMarketable("1");
        }
        spuMapper.updateByPrimaryKeySelective(spu);


        //记录商品审核记录
        //获取审核状态
        Record record = new Record();
        if ("1".equals(spu.getStatus())) {
            record.setAuditStatus("审核通过");
        } else {
            record.setAuditStatus("审核未通过");
        }

        //设置商品id方便后期查看
        record.setId(Integer.parseInt(spu.getId()));
        //获取审核商品信息
        String name = spu.getName();
        record.setSpuName(name);
        //获取审核人(尚未登陆无法实现先给假数据)
        record.setUsername("黑夜");
        //执行审核记录添加
        record.setRecord(message);

        recordMapper.insert(record);

        //记录商品日志(通过aop切面实现日志记录然后调用该日志添加方法)
    }


    //根据id修改下架
    @Override
    public void pull(String id) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setIsMarketable("0");

        spuMapper.updateByPrimaryKeySelective(spu);

        //下架发送队列信息
        indexProviderService.deleteIndex(id);
        pageProviderService.deletePage(id);

        //记录商品日志通过aop实现
    }

    //修改上架(需要验证商品是否通过审核)
    @Override
    public void put(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //判断商品是否通过审核
        if (!"1".equals(spu.getStatus())) {
            throw new RuntimeException("此商品未通过审核");
        }

        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);
        //发送队列消息
        pageProviderService.createPage(id);
        indexProviderService.createIndex(id);

        //记录商品日志通过aop实现
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 批量上架
     */
    @Override
    public int putMany(String[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("1");

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //判断是否通过审核(通过才上架)
        criteria.andEqualTo("IsMarketable", "1");
        //判断当前状态已经下架的商品不用再上架
        criteria.andEqualTo("status", "0");
        //所有需要上架的id值
        criteria.andIn("id", Arrays.asList(ids));

        int count = spuMapper.updateByExampleSelective(spu, example);
        return count;
    }


    //删除商品
    @Override
    @Transactional
    public void del(String id) {
        //通过id查询spu的删除状态
        Spu spu = spuMapper.selectByPrimaryKey(id);

        //逻辑删除
        if ("0".equals(spu.getIsDelete())) {
            spu.setIsDelete("1");
        }
        //还原操作
        else if ("1".equals(spu.getIsDelete())) {
            spu.setIsDelete("0");
        }
        spuMapper.updateByPrimaryKey(spu);
    }

    //物理删除
    @Override
    public void deleteId(String id) {
        spuMapper.deleteByPrimaryKey(id);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 日志记录
     */
    @Override
    public void saveLog(SpuLog spuLog) {
        logMapper.insert(spuLog);
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 主键
            if (searchMap.get("id") != null && !"".equals(searchMap.get("id"))) {
                criteria.andLike("id", "%" + searchMap.get("id") + "%");
            }
            // 货号
            if (searchMap.get("sn") != null && !"".equals(searchMap.get("sn"))) {
                criteria.andLike("sn", "%" + searchMap.get("sn") + "%");
            }
            // SPU名
            if (searchMap.get("name") != null && !"".equals(searchMap.get("name"))) {
                criteria.andLike("name", "%" + searchMap.get("name") + "%");
            }
            // 副标题
            if (searchMap.get("caption") != null && !"".equals(searchMap.get("caption"))) {
                criteria.andLike("caption", "%" + searchMap.get("caption") + "%");
            }
            // 图片
            if (searchMap.get("image") != null && !"".equals(searchMap.get("image"))) {
                criteria.andLike("image", "%" + searchMap.get("image") + "%");
            }
            // 图片列表
            if (searchMap.get("images") != null && !"".equals(searchMap.get("images"))) {
                criteria.andLike("images", "%" + searchMap.get("images") + "%");
            }
            // 售后服务
            if (searchMap.get("saleService") != null && !"".equals(searchMap.get("saleService"))) {
                criteria.andLike("saleService", "%" + searchMap.get("saleService") + "%");
            }
            // 介绍
            if (searchMap.get("introduction") != null && !"".equals(searchMap.get("introduction"))) {
                criteria.andLike("introduction", "%" + searchMap.get("introduction") + "%");
            }
            // 规格列表
            if (searchMap.get("specItems") != null && !"".equals(searchMap.get("specItems"))) {
                criteria.andLike("specItems", "%" + searchMap.get("specItems") + "%");
            }
            // 参数列表
            if (searchMap.get("paraItems") != null && !"".equals(searchMap.get("paraItems"))) {
                criteria.andLike("paraItems", "%" + searchMap.get("paraItems") + "%");
            }
            // 是否上架
            if (searchMap.get("isMarketable") != null && !"".equals(searchMap.get("isMarketable"))) {
                criteria.andLike("isMarketable", "%" + searchMap.get("isMarketable") + "%");
            }
            // 是否启用规格
            if (searchMap.get("isEnableSpec") != null && !"".equals(searchMap.get("isEnableSpec"))) {
                criteria.andLike("isEnableSpec", "%" + searchMap.get("isEnableSpec") + "%");
            }
            // 是否删除
            if (searchMap.get("isDelete") != null && !"".equals(searchMap.get("isDelete"))) {
                criteria.andLike("isDelete", "%" + searchMap.get("isDelete") + "%");
            }
            // 审核状态
            if (searchMap.get("status") != null && !"".equals(searchMap.get("status"))) {
                criteria.andLike("status", "%" + searchMap.get("status") + "%");
            }

            // 品牌ID
            if (searchMap.get("brandId") != null) {
                criteria.andEqualTo("brandId", searchMap.get("brandId"));
            }
            // 一级分类
            if (searchMap.get("category1Id") != null) {
                criteria.andEqualTo("category1Id", searchMap.get("category1Id"));
            }
            // 二级分类
            if (searchMap.get("category2Id") != null) {
                criteria.andEqualTo("category2Id", searchMap.get("category2Id"));
            }
            // 三级分类
            if (searchMap.get("category3Id") != null) {
                criteria.andEqualTo("category3Id", searchMap.get("category3Id"));
            }
            // 模板ID
            if (searchMap.get("templateId") != null) {
                criteria.andEqualTo("templateId", searchMap.get("templateId"));
            }
            // 运费模板id
            if (searchMap.get("freightId") != null) {
                criteria.andEqualTo("freightId", searchMap.get("freightId"));
            }
            // 销量
            if (searchMap.get("saleNum") != null) {
                criteria.andEqualTo("saleNum", searchMap.get("saleNum"));
            }
            // 评论数
            if (searchMap.get("commentNum") != null) {
                criteria.andEqualTo("commentNum", searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
