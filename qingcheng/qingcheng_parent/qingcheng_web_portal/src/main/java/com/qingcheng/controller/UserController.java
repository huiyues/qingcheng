package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import com.qingcheng.utils.BCryptPasswordEncoderUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@EnableDubboConfig
public class UserController {


    @Reference
    private UserService userService;


    /**
     * 发送验证码
     * @param phone
     * @return
     */
    @GetMapping("/codeSms")
    public Result codeSms(String phone){
        userService.codeSms(phone);
        return new Result();
    }

    @PostMapping("/save")
    public Result save(@RequestBody User user, String code){
        //密码加密
        String password = BCryptPasswordEncoderUtils.encodePassword(user.getPassword());
        user.setPassword(password);

        userService.save(user,code );
        return new Result();
    }
}
