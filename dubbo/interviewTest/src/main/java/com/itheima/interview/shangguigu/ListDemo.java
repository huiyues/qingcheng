package com.itheima.interview.shangguigu;

import org.elasticsearch.common.collect.CopyOnWriteHashMap;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author heiye
 * @date 2019-08-09 12:19
 * <p>
 * 集合类出现的安全问题
 */
@SuppressWarnings("all")
public class ListDemo {

    public static void main(String[] args) {
        //List<String> list = new ArrayList<>();
        List<String> list = new CopyOnWriteArrayList<>();
        //List<String> list = Collections.synchronizedList(new ArrayList<>());//new Vector<>();new ArrayList<>();


        //此方法遍历有三种方式可以解决ConcurrentModificationException问题
        /*for (int i = 0; i < 30; i++) {

            new Thread(() -> {
                list.add(UUID.randomUUID().toString().substring(0,8));
                System.out.println(list);  //会产生线程安全问题
                //list.stream().forEach(System.out::println);   //为什么不会产生线程安全问题？
            }, String.valueOf(i)).start();
        }*/

        //此方法遍历只有两种解决方式
        /*list.add("AA");
        Iterator<String> it = list.iterator();
        while (it.hasNext()){
            list.add(UUID.randomUUID().toString().substring(0,8));
            System.out.println(it.next());
        }
*/
        //此方法根本不会产生问题
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                list.add(UUID.randomUUID().toString().substring(0, 8));
                //list.stream().forEach(System.out::println); //println底层加了synchronized关键字  很多线程下也没用了
                System.out.println(list);
            }, "AA").start();
        }

        /**
         * 1、故障现象
         *    java.util.ConcurrentModificationException
         *
         * 2、故障原因
         *      并发争抢修改导致，一个线程正在写，而另一个线程正在读，所以导致线程安全问题
         *      由于list集合的add方法没有加锁所以在多线程下读写会产生线程安全问题
         * 3、解决方案
         *
         *      通过Vector可以防止线程安全问题但是效率低 new Vector<>()
         *      或者通过Collections工具类中的安全集合封装 Collections.synchronizedList(new ArrayList<>())
         *      写时复制CopyOnWrite，线程操作的时候不会直接操作原来的数组，而是先复制一份，再进行修改，让原有的数组
         *      指向复制后的数组即可
         * 4、优化建议
         */
    }

    public static void SetTest(String[] args) {
        Set<String> set = new CopyOnWriteArraySet<>();
        //Collections.synchronizedList(new ArrayList<>());//new Vector<>();new ArrayList<>();

        for (int i = 0; i < 30; i++) {

            new Thread(() -> {
                set.add(UUID.randomUUID().toString().substring(0, 8));
                System.out.println(set);  //会产生线程安全问题
                //list.stream().forEach(System.out::println);   //为什么不会产生线程安全问题？
            }, String.valueOf(i)).start();
        }

        /**
         * new HashSet()  为什么不安全？
         * 因为hashSet底层是hashMap
         * 至于为什么hashSet为什么add方法只需要添加一个值，
         * 是因为hashSet的源代码只关心key不关心value值value值是一个object的常量
         *
         * 解决方案和list集合的一样
         */
    }

    public static void main2(String[] args) {
        Map<String, String> map = new ConcurrentHashMap<>();
//        Map = new CopyOnWriteHashMap<>();
        for (int i = 0; i < 30; i++) {

            new Thread(() -> {
                map.put(Thread.currentThread().getName(), UUID.randomUUID().toString().substring(0, 8));
                System.out.println(map);  //会产生线程安全问题
                //list.stream().forEach(System.out::println);   //为什么不会产生线程安全问题？
            }, String.valueOf(i)).start();
        }

        /**
         * HashMap 的安全问题解决方案
         * ConcurrentHashMap  底层通过segment分段锁技术，
         * 一个segment类似于一个HashTable，其默认值是16相当于hashTable效率的16倍
         *  new CopyOnWriteHashMap<>()
         */
    }

    @Test
    public void test(){
        int i = 123456789;
        System.out.println(i % 100);
    }
}
