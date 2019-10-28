package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.Orders;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);


    public Order findById(String id);

    public Map<String,Object> add(Order order);


    public void update(Order order);


    public void delete(String id);

    public Orders findOrders(String id);

    List<Order> findOrderIds(Map map);

    void updateOrders(List<Order> orderList);

    void orderTimeLogIc();

    void orderMerge(String orderId1, String orderId2);

    String orderSplit(List<Map> orderList);

    public void updateByPayStatus(String orderId,String transactionId);

    void consignStatus(String id);
}
