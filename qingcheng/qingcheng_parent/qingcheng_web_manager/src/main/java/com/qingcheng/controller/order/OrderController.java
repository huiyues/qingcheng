package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.Orders;
import com.qingcheng.service.order.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private OrderService orderService;

    @GetMapping("/findAll")
    public List<Order> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Order> findPage(int page, int size) {
        return orderService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Order> findList(@RequestBody Map<String, Object> searchMap) {
        return orderService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Order> findPage(@RequestBody Map<String, Object> searchMap, int page, int size) {
        return orderService.findPage(searchMap, page, size);
    }

    @GetMapping("/findById")
    public Order findById(String id) {
        return orderService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Order order) {
        orderService.add(order);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Order order) {
        orderService.update(order);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(String id) {
        orderService.delete(id);
        return new Result();
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据id查询详细信息
     */
    @GetMapping("/findOrder")
    public Orders findOrders( String id) {
        Orders orders = orderService.findOrders(id);

        return orders;
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 获取需要发货的所有id数组
     */

    @PostMapping("/findOrderIds")
    public List<Order> findOrderIds(@RequestBody Map map) {
        List<Order> orders = orderService.findOrderIds(map);
        return orders;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据物流单号和名称进行发货
     */

    @PostMapping("/updateOrder")
    public Result updateOrders(@RequestBody List<Order> orderList) {
        orderService.updateOrders(orderList);
        return new Result();
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 订单合并
     */
    @GetMapping("/merge")
    public void orderMerge(String orderId1, String orderId2) {
        orderService.orderMerge(orderId1, orderId2);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 订单拆分
     */
    @PostMapping("/split")
    public String orderSplit(@RequestBody List<Map> list) {
        String id = orderService.orderSplit(list);
        return id;
    }
}
