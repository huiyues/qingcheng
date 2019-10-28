package com.qingcheng.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordEncoderUtils extends BCryptPasswordEncoder {

    private static BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    //编写密码加密工具
    public static String encodePassword(String password) {
        String encodePsm = bCryptPasswordEncoder.encode(password);
        return encodePsm;
    }


    //密码比对方法
    public static boolean matches(String oldPassword, String newPassword){
        boolean falg = bCryptPasswordEncoder.matches(oldPassword, newPassword);
        return falg;
    }

    public static void main(String[] args) {
        String str = "heiye+huiyue-zuzhang=Perfect....";
        String encode = bCryptPasswordEncoder.encode(str);
        System.out.println(encode);


        boolean f = bCryptPasswordEncoder.matches("1223", encode);
        System.out.println(f);
    }
}
