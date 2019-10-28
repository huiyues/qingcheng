package com.qingcheng.utils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//数据类型转换类
public class DateUtils {


    //将日期类型转换成字符串
    public static String getData(Date date, String patten){
        SimpleDateFormat simp = new SimpleDateFormat(patten);
        String dateFormat = simp.format(date);
        return dateFormat;
    }

    //字符串转换成日期格式
    public static Date getString(String date, String patten) throws ParseException {
        SimpleDateFormat smp = new SimpleDateFormat(patten);
        Date parseDate = smp.parse(date);
        return parseDate;
    }
}
