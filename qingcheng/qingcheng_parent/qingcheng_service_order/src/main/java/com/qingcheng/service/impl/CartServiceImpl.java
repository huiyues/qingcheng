package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.utils.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service(interfaceClass = CartService.class)
@SuppressWarnings("all")
public class CartServiceImpl implements CartService {


    @Autowired
    private RedisTemplate redisTemplate;

    @Reference
    private SkuService skuService;

    @Reference
    private CategoryService categoryService;


    /**
     * 通过登录用户从redis获取购物车信息
     *
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findCartList(String username) {
        List<Map<String, Object>> cartMapList = (List<Map<String, Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        //如果取出来的数据为空则重新实例化对象
        if (cartMapList == null) {
            cartMapList = new ArrayList<>();
        }
        return cartMapList;
    }


    /**
     * 添加购物车项到redis
     *
     * @param username
     * @param skuId
     * @param num
     */
    @Override
    public void addItem(String username, String skuId, Integer num) {
        //判断购物车中是否存在该商品，如果存在则累加商品数量，如果不存在则添加商品
        List<Map<String, Object>> cartMapList = findCartList(username);
        //定义一个布尔变量判断购物车中是否存在商品
        boolean flag = false;
        //遍历得到每一个商品
        for (Map map : cartMapList) {
            OrderItem item = (OrderItem) map.get("item");
            //存在该商品
            if (item.getSkuId().equals(skuId)) {
                if (item.getNum() <= 0) {
                    cartMapList.remove(map);
                    break;
                }
                //获取商品原有的单个重量
                int weight = item.getWeight() / item.getNum();

                item.setNum(item.getNum() + num);//商品数量
                item.setMoney(item.getNum() * item.getPrice());//商品价格
                item.setWeight(weight * item.getNum()); //商品重量

                //当前端传入负数则数量为0执行删除操作
                if (item.getNum() <= 0) {
                    cartMapList.remove(map);
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

            cartMapList.add(map);
        }

        //最后重新将数据设置到redis中
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartMapList);

        //设置过期时间
        Random random = new Random();
        int cartCache = random.nextInt(30)+30;
        redisTemplate.boundHashOps(CacheKey.CART_LIST).expire(cartCache, TimeUnit.DAYS);
    }

    /**
     * 修改购物车商品的选中状态
     *
     * @param username
     * @param skuId
     * @param checked
     */
    @Override
    public void updateChecked(String username, String skuId, Boolean checked) {
        List<Map<String, Object>> cartList = findCartList(username);

        //遍历商品
        boolean isOk = false;
        for (Map<String, Object> map : cartList) {
            //判断当前的商品
            OrderItem orderItem = (OrderItem) map.get("item");
            if (orderItem.getSkuId().equals(skuId)) {
                //修改状态
                map.put("checked", checked);

                isOk = true;
                break;
            }
        }

        //修改缓存信息
        if (isOk) {
            redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
        }
    }

    /**
     * 删除选中(使用流方法筛选出没有选中的进行覆盖)
     *
     * @param username
     */
    @Override
    public void deleteCheckedCart(String username) {
        List<Map<String, Object>> cartList = findCartList(username).
                stream().filter(cart -> (boolean) cart.get("checked") == false).collect(Collectors.toList());
        //覆盖缓存
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
    }

    @Autowired
    private PreferentialService preferentialService;

    /**
     * 根据用户获取当前选中的购物车优惠金额
     *
     * @param username
     * @return
     */
    @Override
    public int preferential(String username) {

        //获取选中的订单明细
        List<OrderItem> orderItemList = findCartList(username)
                .stream().filter(cart -> (boolean) cart.get("checked") == true)
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());

        //根据分类id聚合计算消费金额
        Map<Integer, IntSummaryStatistics> carMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OrderItem::getCategoryId3 //根据分类id
                        , Collectors.summarizingInt(OrderItem::getMoney)));//统计消费金额

        int allPreMoney = 0;//总的优惠金额
        for (Integer categoryId : carMap.keySet()) {
            int money = (int) carMap.get(categoryId).getSum(); //获取消费金额
            int preMoney = preferentialService.findByPreMoneyByCategoryId((long)categoryId, money); //获取优惠金额
            allPreMoney += preMoney;
        }
        return allPreMoney;
    }


    /**
     * 更新最新的购物车列表
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findNewOrderItemList(String username) {

        //获取现在的购物车
        List<Map<String, Object>> cartList = findCartList(username);

        //遍历当前购物车
        for (Map<String, Object> cart : cartList) {
            OrderItem orderItem = (OrderItem) cart.get("item");
            //查询最新价格
            Sku sku = skuService.findById(orderItem.getSkuId());
            orderItem.setPrice(sku.getPrice());
            orderItem.setMoney(sku.getPrice() * orderItem.getNum());

            cart.put("item", orderItem);
        }
        //返回最新的购物车列表
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList );
        return cartList;
    }
}
