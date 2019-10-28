package com.itheima.interview.javaee;

import javafx.concurrent.Worker;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author heiye
 * @date 2019-08-10 13:04
 * <p>
 * ʲô��Aop ��
 * aop ��������������oop�����������Դ�ĳЩ��������aop��oop��ǿ��aop��Ҫ��������ҵ���и���
 * ģ���еĴ�����ϣ����ڴ�����ʱ����һ��aop�������а�����Ŀ���������з���
 * <p>
 * Aop�ĺ���ԭ��
 * Aop��Ҫ�ǻ��ڶ�̬����ʵ�ֵģ���aop��ʵ�ַ�ʽ��Ҫ��Ϊ��jdk��̬�����CJLib��������jdk��̬�����ǻ��ڽӿ���ʵ�ֵ�
 * Ŀ�������ʵ��ҵ��ӿڣ����û��ʵ�ֽӿ������CJLib��̬��������������ķ�ʽ��ʵ�֣���Ŀ����һ��������������
 * �����final�������ܲ���CJLib������
 * <p>
 * Aop����ʲô��
 * ���͸���ģ������
 * ʹϵͳ������չ
 * ����Ҫ�޸�Դ���룬��ҵ���ܽ�����ǿ���Ҵ��������
 * <p>
 * Aop��Ӧ�ó�����
 * ����Ŀ��Aopһ������������־���������
 */
@SuppressWarnings("all")
public class AopDemo {

    public static void main1(String[] args) {
        try {
            String urlStr = "http://www.whiterabbit.top";
            URL url = new URL(urlStr);
            for (int i = 0; i < 10000; i++) {
                URLConnection connection = url.openConnection();
                connection.connect();
                System.out.println("ִ�д���-->" + i);
            }
        } catch (Exception e) {
            System.out.println("������!!!-->" + e.getMessage());
        }
    }

    public static void main(String[] args) {
//        String str = "heiye.txt";
//        System.out.println(str.substring(str.lastIndexOf(".") + 1));
        String str = "";
        //System.out.println(str.length());

        //System.out.println(ClassLoader.getSystemClassLoader());
/*

        int k = 1, y = 1, j = 1;
        for (int i = 9; i > 0; i--) {
            k = y;
            for (j = 1; j <= i; j++) {
                int sum = j * k;
                System.out.print(String.format("|%s * %s = %s%s|", j, k, sum, sum > 9 ? "" : " "));
                k++;
            }
            y++;
            System.out.println();
        }
*/


    }

    @Test
    public void test(){
        int num = 20;
        if (((num - 1) & num ) == 0){
            System.out.println("��2�Ľ�˷�");
        }else {
            System.out.println("���ǽٳַ�");
            System.out.println("是否会乱码");
        }
    }
}

