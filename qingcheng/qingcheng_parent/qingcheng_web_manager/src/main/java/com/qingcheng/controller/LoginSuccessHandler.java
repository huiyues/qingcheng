package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import com.qingcheng.utils.WebUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
  * @UpdateUser:     heiye
  * @UpdateRemark:   登陆成功日志记录
  */

public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Reference
    private LoginLogService loginLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        System.out.println("登陆成功了，欢迎来到日志记录....");
        //获取登录日志
        LoginLog loginLog = new LoginLog();
        //获取登陆名
        String name = authentication.getName();
        //获取ip地址
        String remoteIp = httpServletRequest.getRemoteAddr();
        //获取请求头
        String header = httpServletRequest.getHeader("user-agent");
        //调用工具类获取浏览器类型
        String browserName = WebUtil.getBrowserName(header);
        //获取ip城市
        String cityByName= WebUtil.getCityByIP(remoteIp);

        loginLog.setLoginName(name);
        loginLog.setBrowserName(browserName);
        loginLog.setIp(remoteIp);
        loginLog.setLocation(cityByName);
        loginLog.setLoginTime(new Date());

        //添加到数据库
        loginLogService.add(loginLog);

        //登陆成功转发页面
        httpServletRequest.getRequestDispatcher("/main.html").forward(httpServletRequest,httpServletResponse );
    }
}
