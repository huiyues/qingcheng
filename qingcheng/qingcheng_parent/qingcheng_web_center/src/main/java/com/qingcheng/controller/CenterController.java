package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderItemService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.service.user.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/center")
@CrossOrigin
public class CenterController {

    @Reference
    private UserService userService;

    @Reference
    private OrderService orderService;

    @Reference
    private OrderItemService orderItemService;

    @Reference
    private WxPayService wxPayService;
    /**
     * 根据登陆用户查询该用户的订单列表
     *
     * @return
     */

    @GetMapping("/findByOrderList")
    public List findByOrderList() {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List orderAndItemList = userService.findByOrderList(username);
        return orderAndItemList;
    }

    /**
     * 查询所有的收藏商品
     *
     * @return
     */
    @GetMapping("/findCollectList")
    public List<Map<String, Object>> findCartList() {
        //获取登陆用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Map<String, Object>> collectList = userService.findCollectList(username);
        return collectList;
    }

    /**
     * 收藏商品
     *
     * @param response
     * @param skuId
     * @param num
     * @throws IOException
     */
    @GetMapping("/buy")
    public void buy(HttpServletResponse response, String skuId, Integer num) throws IOException {
        //获取登陆用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.addItem(username, skuId, num);

        response.sendRedirect("/center-collect.html");
    }


    /**
     * 根据id修改订单的发货的状态
     * @param id
     * @return
     */
    @GetMapping("/updateStatus")
    public Result updateStatus(String id){
        orderService.consignStatus(id);
        return new Result();
    }


    /**
     * 生成该订单的微信二维码
     *
     * @param orderId
     * @return
     */
    @GetMapping("/createNative")
    public Map createNative(String orderId) {
        Order order = orderService.findById(orderId);
        //获取登陆用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        //判断需要生成二维码的订单
        if (order != null) {
            if ("0".equals(order.getPayStatus()) && username.equals(order.getUsername())) {
                //如果是该订单则生成二维码
                Map map = wxPayService.wxpayUrl(orderId, order.getPayMoney(), "http://heiye.easy.echosite.cn/center/notifyPay.do");
                return map;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * 支付成功后的回调方法
     */
    @RequestMapping("/notifyPay")
    public void notifyLogs(HttpServletRequest request) {
        System.out.println("payment success...Will return automatically after three seconds...");

        //获取请求流
        ServletInputStream inputStream;
        try {
            inputStream = request.getInputStream();
            //创建输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            //定义指针
            int len = 0;
            byte[] bytes = new byte[1024];
            while ((len = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
            }

            String xml = new String(outputStream.toByteArray(), "utf-8");
            System.out.println(xml);
            wxPayService.notifyLogic(xml);
            outputStream.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
