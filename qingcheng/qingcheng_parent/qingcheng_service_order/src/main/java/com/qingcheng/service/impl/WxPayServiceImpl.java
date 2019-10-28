package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.Config;
import com.github.wxpay.sdk.WXPayRequest;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.service.seckill.SeckillOrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;


@Service(interfaceClass = WxPayService.class)
public class WxPayServiceImpl implements WxPayService {


    @Autowired
    private Config config;


    @Override
    public Map wxpayUrl(String orderId, Integer money, String notifyUrl, String... attach) {

        try {
            Config config = new Config();
            //配置请求参数
            Map<String, String> map = new HashMap();
            map.put("appid", config.getAppID()); // 公众号ID
            map.put("mch_id", config.getMchID()); //商户号
            map.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            map.put("body", "青橙"); //商品描述
            map.put("out_trade_no", orderId); //商户订单号
            map.put("total_fee", money + ""); //标价金额
            map.put("spbill_create_ip", "127.0.0.1"); //终端Ip
            map.put("notify_url", notifyUrl); //通知地址
            map.put("trade_type", "NATIVE"); //交易类型
            if (attach != null && attach.length > 0) {
                map.put("attach", attach[0]);
            }

            //转换xml文件形式
            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());

            //发送请求
            WXPayRequest request = new WXPayRequest(config);
            String xmlResult = request.requestWithCert("/pay/unifiedorder", null, xmlParam, false);

            System.out.println("createPay ------>"+xmlResult );

            //将结果数据转换成map
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);

            //返回数据
            Map m = new HashMap();
            m.put("code_url", resultMap.get("code_url"));
            m.put("out_trade_no", orderId);
            m.put("total_fee", money+"");

            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Reference
    private SeckillOrderService seckillOrderService;


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * 微信支付回调
     *
     * @param xml
     */
    @Override
    public void notifyLogic(String xml) {

        try {
            //将xml 的数据转换成map集合
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //验证签名
            boolean b = WXPayUtil.isSignatureValid(resultMap, config.getKey());
            System.out.println("签名验证：" + b);

            //修改订单状态
            if (b) {
                if ("SUCCESS".equals(resultMap.get("result_code"))) {
                    if (resultMap.get("attach") != null || seckillOrderService != null) {
                        seckillOrderService.updatePayStatus(resultMap.get("out_trade_no"), resultMap.get("attach"), resultMap.get("transaction_id"));
                    } else {
                        //修改订单支付状态
                        orderService.updateByPayStatus(resultMap.get("out_trade_no"), resultMap.get("transaction_id"));
                    }

                    //将订单id发送到中间消息
                    rabbitTemplate.convertAndSend("paynotify", "", resultMap.get("out_trade_no"));
                } else {
                    //记录日志
                    System.out.println("-----payment failure-----");
                }
            } else {
                //记录日志
                System.out.println("-----Not this order-----");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 关闭订单支付
     *
     * @param orderId
     * @return
     */
    @Override
    public Map closePay(String orderId) {
        try {
            Config config = new Config();
            //配置请求参数
            Map<String, String> map = new HashMap();
            map.put("appid", config.getAppID()); // 公众号ID
            map.put("mch_id", config.getMchID()); //商户号
            map.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            map.put("out_trade_no", orderId); //商户订单号

            //将Map数据转成XML字符
            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());

            //确定url
            String url = "/pay/closeorder";

            //发送请求
            WXPayRequest request = new WXPayRequest(config);
            String xmlResult = request.requestWithCert(url, null, xmlParam, false);

            System.out.println("closePay ---->"+xmlResult);
            //获取返回数据
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);

            //返回数据
            Map m = new HashMap();
            m.put("return_code", resultMap.get("return_code"));
            m.put("result_code", resultMap.get("result_code"));
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 查询订单支付
     *
     * @param orderId
     * @return
     */
    @Override
    public Map queryPay(String orderId) {
        try {
            Config config = new Config();
            //配置请求参数
            Map<String, String> map = new HashMap();
            map.put("appid", config.getAppID()); // 公众号ID
            map.put("mch_id", config.getMchID()); //商户号
            map.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            map.put("out_trade_no", orderId); //商户订单号
            map.put("sign", "青橙"); //签名

            //将Map数据转成XML字符
            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());

            //确定url
            String url = "/pay/orderquery";

            //发送请求
            WXPayRequest request = new WXPayRequest(config);
            String xmlResult = request.requestWithCert(url, null, xmlParam, false);
            System.out.println("queryPay ----->"+xmlResult);

            //获取返回数据
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);

            //返回数据
            Map m = new HashMap();
            m.put("return_code", resultMap.get("return_code"));
            m.put("result_code", resultMap.get("result_code"));
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
