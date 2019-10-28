package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.utils.CacheKey;
import com.qingcheng.utils.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;


    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private IdWorker idWorker;


    @Autowired
    private OrderConfigMapper orderConfigMapper;

    @Autowired
    private CartService cartService;

    @Reference
    private SkuService skuService;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(), orders.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(), orders.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 新增订单（购物车操作）
     */
    @SuppressWarnings("all")
    public Map<String, Object> add(Order order) {
        //获取当前最新的购物车
        List<Map<String, Object>> cartList = cartService.findNewOrderItemList(order.getUsername());
        //过滤选中的信息
        List<OrderItem> orderItemList = cartList.stream().filter(cart -> (boolean) cart.get("checked") == true)
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());

        //扣减库存(销量增加)
        if (!skuService.deductionStock(orderItemList)) {
            throw new RuntimeException("扣减库存失败！");
        }
        //防止订单id为null
        order.setId(idWorker.nextId() + "");
        try {
            //保存订单主表
            int totalNum = orderItemList.stream().mapToInt(OrderItem::getNum).sum(); //商品数量
            int totalMoney = orderItemList.stream().mapToInt(OrderItem::getMoney).sum();//总金额
            int preferential = cartService.preferential(order.getUsername()); //满减金额
            order.setPreMoney(preferential);
            order.setOrderStatus("0");
            order.setTotalMoney(totalMoney);
            order.setTotalNum(totalNum);
            order.setConsignStatus("0");
            order.setCreateTime(new Date());
            order.setIsDelete("0");
            order.setPayMoney(totalMoney - preferential);
            order.setPayType("0");
            order.setPayStatus("0");
            order.setUpdateTime(new Date());

            //添加订单
            orderMapper.insert(order);

            //添加订单明细数据
            //优惠比列
            double proportion = order.getPayMoney() / totalMoney;
            for (OrderItem orderItem : orderItemList) {
                orderItem.setId(idWorker.nextId() + "");
                orderItem.setOrderId(order.getId());
                orderItem.setPayMoney((int) (orderItem.getMoney() * proportion));

                //添加订单明细
                orderItemMapper.insert(orderItem);

                //添加订单成功发送消息检查是否支付
                rabbitTemplate.convertAndSend("exchange.order", "", order.getId());
                //int x = 1/0;
            }
        } catch (Exception e) {
            rabbitTemplate.convertAndSend("", "queue.back", JSON.toJSONString(orderItemList));
            throw new RuntimeException("订单提交失败！"); //抛出异常让其回滚
        }
        //清除购物车
        cartService.deleteCheckedCart(order.getUsername()); //让未选中的购物车覆盖选中的

        //返回订单编号和实付金额
        Map map = new HashMap();
        map.put("orderNo", order.getId());
        map.put("money", order.getPayMoney());

        return map;
    }

    /**
     * 修改
     *
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据订单id查询详细信息
     */
    @Override
    public Orders findOrders(String id) {
        Order order = orderMapper.selectByPrimaryKey(id);

        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId", order.getId());

        List<OrderItem> orderItem = orderItemMapper.selectByExample(example);

        Orders orders = new Orders();
        orders.setOrder(order);
        orders.setOrderItemList(orderItem);

        return orders;
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据订单id查询所有需要发货的订单
     */
    @Override
    public List<Order> findOrderIds(Map map) {
        if (map.get("ids") != null) {
            Example example = new Example(Order.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andIn("id", Arrays.asList(map.get("ids")));
            //多个订单对象
            List<Order> orderList = orderMapper.selectByExample(example);
            for (Order order : orderList) {
                if ("0".equals(order.getConsignStatus())) {
                    return orderList;
                }
            }
        }
        return null;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据所有订单信息进行修改发货状态
     */
    @Override
    @Transactional
    public void updateOrders(List<Order> orderList) {
        //判断运单号和物流公司是否为空
        for (Order order : orderList) {
            if (order.getShippingCode() == null || order.getShippingName() == null) {
                throw new RuntimeException("请选择快递公司和填写快递单号");
            }
        }

        for (Order order : orderList) {
            //根据所有符合条件的订单进行修改发货状态
            order.setOrderStatus("2");
            order.setConsignStatus("1");
            Example example = new Example(Order.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("id", order.getId());
            orderMapper.updateByExample(order, example);


            //记录订单日志 TODO
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId() + "");//主键
            orderLog.setOperater("操作员1");//暂时写死 TODO
            orderLog.setOperateTime(new Date());//操作时间
            orderLog.setOrderId(order.getId());//订单id
            orderLog.setOrderStatus(order.getOrderStatus());//订单状态
            orderLog.setPayStatus("1");//付款状态，已付款
            orderLog.setConsignStatus(order.getConsignStatus());//发货状态

            int count = orderLogMapper.insertSelective(orderLog);
            System.out.println("新增日志记录:" + count);

        }
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 订单超时处理
     */
    @Override
    @Transactional
    public void orderTimeLogIc() {
        //获取超时时间
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        Integer orderTimeout = orderConfig.getOrderTimeout();

        //获取超时时间点
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);

        //通过超时时间点查询超时的所有订单
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime", localDateTime);//获取创建时间小于超时时间的订单
        criteria.andEqualTo("payStatus", "0");//判断未支付的订单
        criteria.andEqualTo("isDelete", "0");//判断已删除的订单
        //criteria.andEqualTo("payStatus", "2");//判断已经退款的订单

        //查询这些条件的所有订单对象
        List<Order> orderList = orderMapper.selectByExample(example);
        //遍历对象设置订单关闭以及日志记录
        for (Order order : orderList) {
            //记录变更日志信息
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId() + "");
            orderLog.setRemarks("订单超时,系统自动关闭");
            orderLog.setPayStatus("4");
            orderLog.setOrderId(order.getId());
            orderLog.setOperateTime(new Date());
            orderLog.setOperater("黑夜");
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLogMapper.insert(orderLog);

            //更改订单变更
            order.setOrderStatus("4");//关闭订单
            order.setCloseTime(new Date());//关闭时间
            orderMapper.updateByPrimaryKey(order);
        }
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 订单合并
     */
    @Override
    @Transactional
    public void orderMerge(String orderId1, String orderId2) {
        //通过id查询两个订单信息
        Order order = orderMapper.selectByPrimaryKey(orderId1);//主订单
        Order order2 = orderMapper.selectByPrimaryKey(orderId2);//从订单
        order.setPostFee(0);
        order2.setPostFee(0);
        order.setPreMoney(0);
        order2.setPreMoney(0);

        //将从订单的信息合并到主订单
        order.setTotalNum(order.getTotalNum() + order2.getTotalNum());//数量
        order.setTotalMoney(order.getTotalMoney() + order2.getTotalMoney());//金额
        order.setPreMoney(order.getPreMoney() + order2.getPreMoney());//优惠金额
        order.setPostFee(order.getPostFee() + order2.getPostFee());//邮费金额
        order.setPayMoney(order.getPayMoney() + order2.getPayMoney());//实付金额
        order.setUpdateTime(new Date());

        //将从订单逻辑删除
        order2.setIsDelete("1");
        //设置从订单的订单明细为主订单
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId", order2.getId());
        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());//设置所有从订单明细归属于主订单
            orderItemMapper.updateByPrimaryKey(orderItem);
        }

        orderMapper.updateByPrimaryKey(order);
        orderMapper.updateByPrimaryKey(order2);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 订单拆分
     */
    @Override
    public String orderSplit(List<Map> mapList) {
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id", Arrays.asList(mapList.get(0).get("id")));

        //新拆分出来的订单对象
        Order order = new Order();

        OrderItem orderItem1 = orderItemMapper.selectOneByExample(example);
        order = orderMapper.selectByPrimaryKey(orderItem1.getOrderId());

        order.setId(idWorker.nextId() + "");
        order.setTotalNum(order.getTotalNum());
        order.setTotalMoney(order.getTotalMoney() / 2);
        order.setPayStatus("0");


        //获取要拆分的明细对象
        for (Map map : mapList) {
            Object id = map.get("id");
            //通过id获取到明细对象
            //得到每一个需要拆分的订单明细对象
            OrderItem orderItems = orderItemMapper.selectByPrimaryKey(id);

            //首先判断拆分数量是否大于订单明细数量
            //如果拆分数量大于明细数量则拆分失败
            if (orderItems.getNum() < (Integer) map.get("num")) {
                throw new RuntimeException("拆分的数量超出了可拆分范围");
            } else {
                //遍历拆分订单，拆分订单数量应该等于传递的数量
                //遍历进行订单赋值
                OrderItem orderItem = new OrderItem();
                orderItem.setId(idWorker.nextId() + "");
                orderItem.setNum(orderItems.getNum() - (Integer) map.get("num"));
                orderItem.setPayMoney(orderItems.getMoney() / orderItems.getNum() + orderItems.getPostFee());
                orderItem.setPrice(orderItems.getMoney() / orderItems.getNum());

                //将每一个对应的拆分明细与订单对应
                orderItem.setOrderId(order.getId());

                //拆分执行
                orderItemMapper.insert(orderItem);
                orderMapper.insert(order);

            }
        }
        return order.getId();
    }


    /**
     * 根据订单id修改订单状态
     *
     * @param orderId
     * @param transactionId
     */
    @Override
    @Transactional
    public void updateByPayStatus(String orderId, String transactionId) {
        Order order = findById(orderId);
        if (order != null && "0".equals(order.getOrderStatus())) {
            order.setUpdateTime(new Date()); //更新时间
            order.setPayStatus("1"); //支付状态
            order.setOrderStatus("1");  //订单状态
            order.setPayTime(new Date()); //更新支付时间
            order.setTransactionId(transactionId); //交易流水号

            orderMapper.updateByPrimaryKeySelective(order);

            //记录订单变动日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId() + "");
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLog.setOperater("heiye");
            orderLog.setOperateTime(new Date());
            orderLog.setOrderId(orderId);
            orderLog.setOrderStatus("1");
            orderLog.setPayStatus("1");
            orderLog.setRemarks("支付流水号:" + transactionId);
            orderLogMapper.insert(orderLog);
        }
    }

    /**
     * \
     * 修改订单的发货状态
     *
     * @param id
     */
    @Override
    public void consignStatus(String id) {
        Order order = findById(id);
        if (order != null) {
            order.setOrderStatus("2");
            order.setConsignStatus("1");
        }
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 订单id
            if (searchMap.get("id") != null && !"".equals(searchMap.get("id"))) {
                criteria.andLike("id", "%" + searchMap.get("id") + "%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if (searchMap.get("payType") != null && !"".equals(searchMap.get("payType"))) {
                criteria.andLike("payType", "%" + searchMap.get("payType") + "%");
            }
            // 物流名称
            if (searchMap.get("shippingName") != null && !"".equals(searchMap.get("shippingName"))) {
                criteria.andLike("shippingName", "%" + searchMap.get("shippingName") + "%");
            }
            // 物流单号
            if (searchMap.get("shippingCode") != null && !"".equals(searchMap.get("shippingCode"))) {
                criteria.andLike("shippingCode", "%" + searchMap.get("shippingCode") + "%");
            }
            // 用户名称
            if (searchMap.get("username") != null && !"".equals(searchMap.get("username"))) {
                criteria.andLike("username", "%" + searchMap.get("username") + "%");
            }
            // 买家留言
            if (searchMap.get("buyerMessage") != null && !"".equals(searchMap.get("buyerMessage"))) {
                criteria.andLike("buyerMessage", "%" + searchMap.get("buyerMessage") + "%");
            }
            // 是否评价
            if (searchMap.get("buyerRate") != null && !"".equals(searchMap.get("buyerRate"))) {
                criteria.andLike("buyerRate", "%" + searchMap.get("buyerRate") + "%");
            }
            // 收货人
            if (searchMap.get("receiverContact") != null && !"".equals(searchMap.get("receiverContact"))) {
                criteria.andLike("receiverContact", "%" + searchMap.get("receiverContact") + "%");
            }
            // 收货人手机
            if (searchMap.get("receiverMobile") != null && !"".equals(searchMap.get("receiverMobile"))) {
                criteria.andLike("receiverMobile", "%" + searchMap.get("receiverMobile") + "%");
            }
            // 收货人地址
            if (searchMap.get("receiverAddress") != null && !"".equals(searchMap.get("receiverAddress"))) {
                criteria.andLike("receiverAddress", "%" + searchMap.get("receiverAddress") + "%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if (searchMap.get("sourceType") != null && !"".equals(searchMap.get("sourceType"))) {
                criteria.andLike("sourceType", "%" + searchMap.get("sourceType") + "%");
            }
            // 交易流水号
            if (searchMap.get("transactionId") != null && !"".equals(searchMap.get("transactionId"))) {
                criteria.andLike("transactionId", "%" + searchMap.get("transactionId") + "%");
            }
            // 订单状态
            if (searchMap.get("orderStatus") != null && !"".equals(searchMap.get("orderStatus"))) {
                criteria.andLike("orderStatus", "%" + searchMap.get("orderStatus") + "%");
            }
            // 支付状态
            if (searchMap.get("payStatus") != null && !"".equals(searchMap.get("payStatus"))) {
                criteria.andLike("payStatus", "%" + searchMap.get("payStatus") + "%");
            }
            // 发货状态
            if (searchMap.get("consignStatus") != null && !"".equals(searchMap.get("consignStatus"))) {
                criteria.andLike("consignStatus", "%" + searchMap.get("consignStatus") + "%");
            }
            // 是否删除
            if (searchMap.get("isDelete") != null && !"".equals(searchMap.get("isDelete"))) {
                criteria.andLike("isDelete", "%" + searchMap.get("isDelete") + "%");
            }

            // 数量合计
            if (searchMap.get("totalNum") != null) {
                criteria.andEqualTo("totalNum", searchMap.get("totalNum"));
            }
            // 金额合计
            if (searchMap.get("totalMoney") != null) {
                criteria.andEqualTo("totalMoney", searchMap.get("totalMoney"));
            }
            // 优惠金额
            if (searchMap.get("preMoney") != null) {
                criteria.andEqualTo("preMoney", searchMap.get("preMoney"));
            }
            // 邮费
            if (searchMap.get("postFee") != null) {
                criteria.andEqualTo("postFee", searchMap.get("postFee"));
            }
            // 实付金额
            if (searchMap.get("payMoney") != null) {
                criteria.andEqualTo("payMoney", searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
