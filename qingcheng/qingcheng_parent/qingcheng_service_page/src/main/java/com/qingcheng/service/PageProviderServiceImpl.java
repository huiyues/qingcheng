package com.qingcheng.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.service.provider.PageProviderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Service(interfaceClass = PageProviderService.class)
public class PageProviderServiceImpl implements PageProviderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送创建删除消息
     * @param spuId
     */
    @Override
    public void createPage(String spuId) {
        rabbitTemplate.convertAndSend("exchange.Racking", "", spuId);
        System.out.println("------> exchange.Racking send page success......");
    }


    /**
     * 发送页面删除消息
     * @param spuId
     */
    @Override
    public void deletePage(String spuId) {
        rabbitTemplate.convertAndSend("exchange.UnShelve", "", spuId);
        System.out.println("------> exchange.UnShelve send success......");
    }
}
