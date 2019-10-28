package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.user.Address;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.user.AddressService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@EnableDubboConfig
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private AddressService addressService;

    @GetMapping("/findCartList")
    public List<Map<String, Object>> findCartList() {
        //获取登陆用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Map<String, Object>> cartList = cartService.findCartList(username);
        return cartList;
    }

    @GetMapping("/addItem")
    public Result addItem(String skuId, Integer num) {
        //获取登陆用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username, skuId, num);

        return new Result();
    }

    @GetMapping("/buy")
    public void buy(HttpServletResponse response, String skuId, Integer num) throws IOException {
        //获取登陆用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username, skuId, num);

        response.sendRedirect("/cart.html");
    }

    /**
     * 修改选中状态
     *
     * @param skuId
     * @param checked
     * @return
     */
    @GetMapping("/updateChecked")
    public Result updateChecked(String skuId, Boolean checked) {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.updateChecked(username, skuId, checked);
        return new Result();
    }


    /**
     * 删除选中
     *
     * @return
     */
    @GetMapping("/deleteCheckedCart")
    public Result deleteCheckedCart() {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.deleteCheckedCart(username);
        return new Result();
    }


    /**
     * 获取优惠金额
     *
     * @return
     */
    @GetMapping("/findByPreferential")
    public Map findByPreferential() {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        int preferential = cartService.preferential(username);
        Map map = new HashMap();
        map.put("preferential", preferential);
        return map;
    }


    /**
     * 获取更新价格后的最新购物车列表
     *
     * @return
     */
    @GetMapping("/findNewOrderItemList")
    public List<Map<String, Object>> findNewOrderItemList() {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Map<String, Object>> orderItemList = cartService.findNewOrderItemList(username);
        return orderItemList;
    }


    /**
     * 根据用户名查询该用户拥有的地址信息
     *
     * @return
     */
    @GetMapping("/findByUserName")
    public List<Address> findByUserName() {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Address> addressList = addressService.findByUserName(username);
        return addressList;
    }


    @Reference
    private OrderService orderService;


    /**
     * 添加购物车订单
     *
     * @return
     */
    @PostMapping("/saveOrder")
    public Map<String, Object> saveOrder(@RequestBody Order order) {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        order.setUsername(username);
        Map<String, Object> map = orderService.add(order);
        return map;
    }
}
