package com.qingcheng.service.order;
import java.util.Map;

public interface WxPayService {


    /**
     * 获取微信二维码url
     * @param orderId  订单号
     * @param money 实际金额
     * @param notifyUrl 回调地址
     * @return
     */
    Map wxpayUrl(String orderId,Integer money,String notifyUrl ,String... attach);

    void notifyLogic(String xml);

    Map closePay(String orderId);


    Map queryPay(String orderId);
}
