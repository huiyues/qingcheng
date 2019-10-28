package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.UserMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.OrderItemService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.user.UserService;
import com.qingcheng.utils.CacheKey;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<User> findAll() {
        return userMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<User> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<User> users = (Page<User>) userMapper.selectAll();
        return new PageResult<User>(users.getTotal(), users.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<User> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return userMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<User> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<User> users = (Page<User>) userMapper.selectByExample(example);
        return new PageResult<User>(users.getTotal(), users.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param username
     * @return
     */
    public User findById(String username) {
        return userMapper.selectByPrimaryKey(username);
    }

    /**
     * 新增
     *
     * @param user
     */
    public void add(User user) {
        userMapper.insert(user);
    }

    /**
     * 修改
     *
     * @param user
     */
    public void update(User user) {
        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     * 删除
     *
     * @param username
     */
    public void delete(String username) {
        userMapper.deleteByPrimaryKey(username);
    }


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送短信验证码
     *
     * @param phone
     */
    @Override
    public void codeSms(String phone) {
        //获取验证码
        Random random = new Random();
        int code = random.nextInt(899999) + 100000;

        //存储到redis
        redisTemplate.boundValueOps("code_" + phone).set(code + "");
        //设置过期时间
        redisTemplate.boundValueOps("code" + phone).expire(5, TimeUnit.MINUTES);

        //发送到rabbit
        //将数据封装成一个map
        Map map = new HashMap();
        map.put("phone", phone);
        map.put("code", code + "");

        rabbitTemplate.convertAndSend("", "queue.sms", JSON.toJSONString(map));
    }

    /**
     * 用户注册。以及验证码效验
     *
     * @param user
     * @param smsCode
     */
    @Override
    public void save(User user, String smsCode) {
        //效验验证码
        if (smsCode == null) {
            throw new RuntimeException("验证码未发送或已过期");
        }

        //获取redis中的验证码
        String code = (String) redisTemplate.boundValueOps("code_" + user.getPhone()).get();
        if (!smsCode.equals(code)) {
            throw new RuntimeException("验证码错误！");
        }

        //添加用户名
        if (user.getUsername() == null) {
            user.setUsername(user.getPhone());//设置用户名为手机号
        }

        //效验用户名是否存在
        User searchUser = new User();
        searchUser.setUsername(user.getUsername());

        int count = userMapper.selectCount(searchUser);
        if (count > 0) {
            throw new RuntimeException("该手机号已注册！");
        }
        //添加用户数据
        user.setIsEmailCheck("0");//邮箱是否验证
        user.setIsMobileCheck("1"); //手机是否验证
        user.setPoints(0); //积分
        user.setStatus("1");//状态
        user.setUpdated(new Date()); //更新时间
        user.setCreated(new Date()); //创建时间

        userMapper.insert(user);//添加
    }

    /**
     * 用户收藏中心查询
     *
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findCollectList(String username) {
        List<Map<String, Object>> mapList = (List<Map<String, Object>>) redisTemplate.boundHashOps(CacheKey.CENTER_COLLECT).get(username);
        if (mapList == null) {
            mapList = new ArrayList<>();
        }
        return mapList;
    }

    @Reference
    private CategoryService categoryService;

    @Reference
    private SkuService skuService;

    //添加用户收藏
    @Override
    public void addItem(String username, String skuId, Integer num) {
        //判断收藏中是否存在该商品，如果存在则累加商品数量，如果不存在则添加商品
        List<Map<String, Object>> collectList = findCollectList(username);
        //定义一个布尔变量判断收藏中是否存在商品
        boolean flag = false;
        //遍历得到每一个商品
        for (Map map : collectList) {
            OrderItem item = (OrderItem) map.get("item");
            //存在该商品
            if (item.getSkuId().equals(skuId)) {
                if (item.getNum() <= 0) {
                    collectList.remove(map);
                    break;
                }
                //获取商品原有的单个重量
                int weight = item.getWeight() / item.getNum();

                item.setNum(item.getNum() + num);//商品数量
                item.setMoney(item.getNum() * item.getPrice());//商品价格
                item.setWeight(weight * item.getNum()); //商品重量

                //当前端传入负数则数量为0执行删除操作
                if (item.getNum() <= 0) {
                    collectList.remove(map);
                }
                flag = true;
                break;
            }
        }


        //如果商品不存在
        if (flag == false) {

            //根据skuId查询商品信息
            Sku sku = skuService.findById(skuId);
            if (sku == null) {
                throw new RuntimeException("该商品已下架!");
            }
            if (!"1".equals(sku.getStatus())) {
                throw new RuntimeException("商品状态不合法！");
            }
            if (num <= 0) {
                throw new RuntimeException("商品数量不合法！");
            }

            //设置订单明细属性
            OrderItem orderItem = new OrderItem();
            orderItem.setPrice(sku.getPrice());
            orderItem.setNum(num);
            orderItem.setMoney(orderItem.getPrice() * num); //计算金额
            orderItem.setImage(sku.getImage());
            orderItem.setName(sku.getName());
            orderItem.setSkuId(skuId);
            orderItem.setSpuId(sku.getSpuId());
            if (orderItem.getWeight() == null) {
                orderItem.setWeight(0);
            }
            orderItem.setWeight(sku.getWeight() * num); //计算总量

            //设置商品id
            //redis优化
            Category category3 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(sku.getCategoryId());
            if (category3 == null) {
                //从数据库查询
                category3 = categoryService.findById(sku.getCategoryId());
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(sku.getCategoryId(), category3);
            }

            //查询二级分类对象
            Category category2 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(category3.getParentId());
            if (category2 == null) {
                //从数据库查询
                category2 = categoryService.findById(category3.getParentId());
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(category3.getParentId(), category2);
            }
            orderItem.setCategoryId3(sku.getCategoryId());
            orderItem.setCategoryId2(category3.getParentId());//通过三级分类id得到二级分类id
            orderItem.setCategoryId1(category2.getParentId());//通过二级分类id得到一级分类id

            //将订单添加到map中
            Map map = new HashMap();
            map.put("item", orderItem);
            map.put("checked", true);

            collectList.add(map);
        }

        //最后重新将数据设置到redis中
        redisTemplate.boundHashOps(CacheKey.CENTER_COLLECT).put(username, collectList);

        //设置过期时间
        Random random = new Random();
        int cartCache = random.nextInt(30) + 30;
        redisTemplate.boundHashOps(CacheKey.CENTER_COLLECT).expire(cartCache, TimeUnit.DAYS);
    }

    @Reference
    private OrderService orderService;

    @Reference
    private OrderItemService orderItemService;

    //查询用户拥有的订单和明细数据
    @Override
    public List findByOrderList(String username) {
        //Map centerOrderList = (Map) redisTemplate.boundValueOps("centerOrderList").get();
        List resultList = new ArrayList();

        Map map = new HashMap<>();
        map.put("username", username);
        //查询所有订单
        List<Order> orderList = orderService.findList(map);
        //查询所有明细
        for (Order order : orderList) {
            Map itemMap = new HashMap();
            itemMap.put("orderId", order.getId());
            List<OrderItem> orderItems = orderItemService.findList(itemMap);

            Map resultMap = new HashMap();
            resultMap.put("orderList", order);
            resultMap.put("orderItemList", orderItems);

            resultList.add(resultMap);
        }

        //存入缓存
        //redisTemplate.boundValueOps("centerOrderList").set(orderAndItem);
        return resultList;
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(User.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 用户名
            if (searchMap.get("username") != null && !"".equals(searchMap.get("username"))) {
                criteria.andLike("username", "%" + searchMap.get("username") + "%");
            }
            // 密码，加密存储
            if (searchMap.get("password") != null && !"".equals(searchMap.get("password"))) {
                criteria.andLike("password", "%" + searchMap.get("password") + "%");
            }
            // 注册手机号
            if (searchMap.get("phone") != null && !"".equals(searchMap.get("phone"))) {
                criteria.andLike("phone", "%" + searchMap.get("phone") + "%");
            }
            // 注册邮箱
            if (searchMap.get("email") != null && !"".equals(searchMap.get("email"))) {
                criteria.andLike("email", "%" + searchMap.get("email") + "%");
            }
            // 会员来源：1:PC，2：H5，3：Android，4：IOS
            if (searchMap.get("sourceType") != null && !"".equals(searchMap.get("sourceType"))) {
                criteria.andLike("sourceType", "%" + searchMap.get("sourceType") + "%");
            }
            // 昵称
            if (searchMap.get("nickName") != null && !"".equals(searchMap.get("nickName"))) {
                criteria.andLike("nickName", "%" + searchMap.get("nickName") + "%");
            }
            // 真实姓名
            if (searchMap.get("name") != null && !"".equals(searchMap.get("name"))) {
                criteria.andLike("name", "%" + searchMap.get("name") + "%");
            }
            // 使用状态（1正常 0非正常）
            if (searchMap.get("status") != null && !"".equals(searchMap.get("status"))) {
                criteria.andLike("status", "%" + searchMap.get("status") + "%");
            }
            // 头像地址
            if (searchMap.get("headPic") != null && !"".equals(searchMap.get("headPic"))) {
                criteria.andLike("headPic", "%" + searchMap.get("headPic") + "%");
            }
            // QQ号码
            if (searchMap.get("qq") != null && !"".equals(searchMap.get("qq"))) {
                criteria.andLike("qq", "%" + searchMap.get("qq") + "%");
            }
            // 手机是否验证 （0否  1是）
            if (searchMap.get("isMobileCheck") != null && !"".equals(searchMap.get("isMobileCheck"))) {
                criteria.andLike("isMobileCheck", "%" + searchMap.get("isMobileCheck") + "%");
            }
            // 邮箱是否检测（0否  1是）
            if (searchMap.get("isEmailCheck") != null && !"".equals(searchMap.get("isEmailCheck"))) {
                criteria.andLike("isEmailCheck", "%" + searchMap.get("isEmailCheck") + "%");
            }
            // 性别，1男，0女
            if (searchMap.get("sex") != null && !"".equals(searchMap.get("sex"))) {
                criteria.andLike("sex", "%" + searchMap.get("sex") + "%");
            }

            // 会员等级
            if (searchMap.get("userLevel") != null) {
                criteria.andEqualTo("userLevel", searchMap.get("userLevel"));
            }
            // 积分
            if (searchMap.get("points") != null) {
                criteria.andEqualTo("points", searchMap.get("points"));
            }
            // 经验值
            if (searchMap.get("experienceValue") != null) {
                criteria.andEqualTo("experienceValue", searchMap.get("experienceValue"));
            }

        }
        return example;
    }

}
