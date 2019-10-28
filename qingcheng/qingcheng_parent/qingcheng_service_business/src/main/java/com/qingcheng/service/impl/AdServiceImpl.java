package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AdMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.service.business.AdService;
import com.qingcheng.utils.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
public class AdServiceImpl implements AdService {

    @Autowired
    private AdMapper adMapper;


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Ad> findAll() {
        return adMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Ad> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Ad> ads = (Page<Ad>) adMapper.selectAll();
        return new PageResult<Ad>(ads.getTotal(), ads.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Ad> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return adMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Ad> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Ad> ads = (Page<Ad>) adMapper.selectByExample(example);
        return new PageResult<Ad>(ads.getTotal(), ads.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Ad findById(Integer id) {
        return adMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param ad
     */
    public void add(Ad ad) {
        adMapper.insert(ad);
        saveAdToByRedisPosition(ad.getPosition());//如果数据发生变化则重新预热
    }

    /**
     * 修改
     *
     * @param ad
     */
    public void update(Ad ad) {
        //获取修改之前的位置进行预热
        String position = adMapper.selectByPrimaryKey(ad.getId()).getPosition();

        adMapper.updateByPrimaryKeySelective(ad);

        saveAdToByRedisPosition(position);
        if (!position.equals(ad.getPosition())) {
            saveAdToByRedisPosition(ad.getPosition());//如果位置发生变化则重新预热
        }
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(Integer id) {
        String position = adMapper.selectByPrimaryKey(id).getPosition();
        adMapper.deleteByPrimaryKey(id);
        saveAdToByRedisPosition(position);//如果数据发生变化则重新预热
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据地址查询所有的广告信息
     */
    @Override
    public List<Ad> findByList(String position) {
        //从缓存中拿到指定位置的缓存信息
        List<Ad> adList = (List<Ad>) redisTemplate.boundHashOps(CacheKey.AD).get(position);
        return adList;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 将某个位置的广告存入到缓存中
     */
    @Override
    public void saveAdToByRedisPosition(String position) {
        Example example = new Example(Ad.class);
        Example.Criteria criteria = example.createCriteria();

        //根据地址进行查询
        criteria.andEqualTo("position", position);
        //开始时间
        criteria.andLessThanOrEqualTo("startTime", new Date());
        /*结束时间*/
        criteria.andGreaterThanOrEqualTo("endTime", new Date());
        /*判读状态*/
        criteria.andEqualTo("status", "1");

        List<Ad> ads = adMapper.selectByExample(example);

        //将数据存入到缓存中
        if (ads != null) {
            redisTemplate.boundHashOps(CacheKey.AD).put(position, ads);
        }
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 查询所有的广告存入到缓存中
     */
    @Override
    public void saveAllToRedis() {
        //获取到所有的位置
        List<String> position = getPosition();
        for (String posi : position) {
            //调用查询位置的方法
            saveAdToByRedisPosition(posi);
        }
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 得到所有的广告地址
     */

    public List<String> getPosition() {
        List<String> positionList = new ArrayList<>();
        positionList.add("web_index_lb");
        positionList.add("web_index_lb");
        positionList.add("web_index_lb");

        return positionList;
    }


    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Ad.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 广告名称
            if (searchMap.get("name") != null && !"".equals(searchMap.get("name"))) {
                criteria.andLike("name", "%" + searchMap.get("name") + "%");
            }
            // 广告位置
            if (searchMap.get("position") != null && !"".equals(searchMap.get("position"))) {
                criteria.andLike("position", "%" + searchMap.get("position") + "%");
            }
            // 状态
            if (searchMap.get("status") != null && !"".equals(searchMap.get("status"))) {
                criteria.andLike("status", "%" + searchMap.get("status") + "%");
            }
            // 图片地址
            if (searchMap.get("image") != null && !"".equals(searchMap.get("image"))) {
                criteria.andLike("image", "%" + searchMap.get("image") + "%");
            }
            // URL
            if (searchMap.get("url") != null && !"".equals(searchMap.get("url"))) {
                criteria.andLike("url", "%" + searchMap.get("url") + "%");
            }
            // 备注
            if (searchMap.get("remarks") != null && !"".equals(searchMap.get("remarks"))) {
                criteria.andLike("remarks", "%" + searchMap.get("remarks") + "%");
            }

            // ID
            if (searchMap.get("id") != null) {
                criteria.andEqualTo("id", searchMap.get("id"));
            }

        }
        return example;
    }

}
