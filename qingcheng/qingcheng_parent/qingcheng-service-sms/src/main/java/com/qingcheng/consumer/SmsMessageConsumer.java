package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.utls.SendUtls;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public class SmsMessageConsumer implements MessageListener {

    @Autowired
    private SendUtls sendUtls;

    @Value("${smsCode}")
    private String smsCode;

    @Value("${param}")
    private String param;

    @Override
    public void onMessage(Message message) {
        //获取存储的手机号的验证码
        String jsonString = new String(message.getBody());

        Map<String,String> map = JSON.parseObject(jsonString, Map.class);
        String phone = map.get("phone");
        String code = map.get("code");

        //调用阿里云通信
        sendUtls.code(phone,smsCode , param.replace("[value]", "520"));
        System.out.println("phone："+phone+"--"+"code："+code);
    }
}
