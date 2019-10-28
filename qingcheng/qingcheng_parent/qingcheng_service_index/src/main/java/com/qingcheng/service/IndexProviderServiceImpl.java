package com.qingcheng.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.service.provider.IndexProviderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Service(interfaceClass = IndexProviderService.class)
public class IndexProviderServiceImpl implements IndexProviderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送生成详情页消息
     * @param spuId
     */
    @Override
    public void createIndex(String spuId) {
        rabbitTemplate.convertAndSend("exchange.Racking", "", spuId);
        System.out.println("------> exchange.Racking send index success......");
    }

    /**
     * 发送删除详情页消息
     * @param spuId
     */
    @Override
    public void deleteIndex(String spuId) {
        rabbitTemplate.convertAndSend("exchange.UnShelve", "", spuId);
    }
}
