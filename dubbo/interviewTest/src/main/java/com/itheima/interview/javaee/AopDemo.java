package com.itheima.interview.javaee;

import java.net.URL;
import java.net.URLConnection;

/**
 * @author heiye
 * @date 2019-08-10 13:04
 * <p>
 * 什么是Aop ？
 * aop ：面向切面编程是oop的延续，所以从某些方面来讲aop比oop更强，aop主要用来处理业务中各个
 * 模块中的代码耦合，在内存中临时生成一个aop对象，其中包含了目标对象的所有方法
 * <p>
 * Aop的核心原理？
 * Aop主要是基于动态代理实现的，而aop的实现方式主要分为：jdk动态代理和CJLib代理技术，jdk动态代理是基于接口来实现的
 * 目标类必须实现业务接口，如果没有实现接口则采用CJLib动态代理技术基于子类的方式来实现，且目标类一定是子类对象，如果
 * 类对象被final修饰则不能采用CJLib代理技术
 * <p>
 * Aop能做什么？
 * 降低各个模块的耦合
 * 使系统容易扩展
 * 不需要修改源代码，对业务功能进行增强，且代码可重用
 * <p>
 * Aop的应用场景？
 * 在项目中Aop一般用来处理日志和事务管理
 */
public class AopDemo {

    public static void main1(String[] args) {
        try {
            String urlStr = "http://www.whiterabbit.top";
            URL url = new URL(urlStr);
            for (int i = 0; i < 10000; i++) {
                URLConnection connection = url.openConnection();
                connection.connect();
                System.out.println("执行次数-->" + i);
            }
        } catch (Exception e) {
            System.out.println("出错了!!!-->" + e.getMessage());
        }
    }

    public static void main(String[] args) {
//        String str = "heiye.txt";
//        System.out.println(str.substring(str.lastIndexOf(".") + 1));
        String str = "";
        System.out.println(str.length());
    }
}
