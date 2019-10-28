package com.qingcheng.controller.log;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.controller.goods.SpuLogController;
import com.qingcheng.pojo.log.SpuLog;
import com.qingcheng.service.goods.SpuService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;

@Aspect
@Component
public class SpuLogAspect {

    @Reference
    private SpuService spuService;

    @Autowired
    private HttpServletRequest request;


    //定义成员变量用于封装日志类的参数
    private Date visitTime; //开始的访问时间
    private String username;  //访问的用户
    private String ip;  //访问的IP地址(如果对方使用了反向代理则目前获取不到)
    private String url;     //获取用户访问的请求路径
    private Long executionTime;  //用户开始访问到结束的访问时长
    private Method visitMethod;   //用户访问的方法
    private Class visitClass;  //用户访问的类(方便全局使用没有特殊的意义)


    //前置通知获取用户访问的类，方法，以及开始时间
    @Before("execution(* com.qingcheng.controller.goods.spu*(..))")
    public void before(JoinPoint joinPoint) throws NoSuchMethodException {
        //获取开始时间
        visitTime = new Date();
        //获取访问的类
        visitClass = joinPoint.getTarget().getClass();

        //获取访问的方法名
        String visitMethodName = joinPoint.getSignature().getName();

        //获取方法参数
        Object[] args = joinPoint.getArgs();
        //判断该是有参方法还是无参
        if (args == null && args.length == 0) {  //无参方法
            visitMethod = visitClass.getMethod(visitMethodName);
        } else {
            Class[] classAges = new Class[args.length];  //有参方法
            for (int i = 0; i < args.length; i++) {
                classAges[i] = args[i].getClass();  //获取每一个参数类对象
            }
            //再通过类对象获取有参的方法对象
            visitMethod = visitClass.getMethod(visitMethodName, classAges);
        }
    }


    //后置通知用于获取用户的 姓名,访问时长，ip，以及URL地址
    @AfterReturning("execution(* com.qingcheng.controller.goods.spu*(..))")
    public void afterReturning() throws Exception {
        //获取用户的访问路径,排除掉日志的查询类
        //首先获取requestMapping注解的value值获得类路径
        if (visitClass != SpuLogController.class) {
            RequestMapping classAnnotation = (RequestMapping) visitClass.getAnnotation(RequestMapping.class);

            if (classAnnotation != null) {
                String classUrl = classAnnotation.value()[0]; //类路径

                //然后获取方法路径
                String methodUrl;
                //获取到该方法上的注解
                RequestMapping methodAnnotation = visitMethod.getAnnotation(RequestMapping.class);
                GetMapping getMappingAnnotation = visitMethod.getAnnotation(GetMapping.class);
                PostMapping postMappingAnnotation = visitMethod.getAnnotation(PostMapping.class);

                methodUrl = methodAnnotation.value()[0];
                methodUrl = postMappingAnnotation.value()[0];
                methodUrl = getMappingAnnotation.value()[0];

                //用户访问的路径为
                url = classUrl + methodUrl;

                //获取用户访问的ip地址(需要在web.xml文件中配合请求监听)
                ip = request.getRemoteAddr();

                //获取用户的姓名
                //可以从security中的authentication获取到当前的用户信息
                SecurityContext context = SecurityContextHolder.getContext();  //获取security的上下文对象

                //通过上下文对象获取到当前操作的用户信息，强转成user对象
                User user = (User) context.getAuthentication().getPrincipal();
                if (user != null) {
                    username = user.getUsername();
                }
                username = "黑夜";

                //最后获取访问时长
                executionTime = new Date().getTime() - visitTime.getTime();

                //将所有数据设置到SysLog对象中
                SpuLog spuLog = new SpuLog();
                spuLog.setMethod("类名：" + visitClass.getName() + " 方法名：" + visitMethod.getName());  //访问的类名和方法名
                spuLog.setVisitTime(visitTime);  //访问的开始时间
                spuLog.setUsername(username);    //访问的用户名
                spuLog.setIp(ip);               //访问的ip
                spuLog.setUrl(url);             //访问的路径
                spuLog.setExecutionTime(executionTime);   //访问的时长

                //最后调用service层将数据添加到数据库
                spuService.saveLog(spuLog);
            }
        }
    }
}
