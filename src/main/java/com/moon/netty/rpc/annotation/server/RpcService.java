package com.moon.netty.rpc.annotation.server;


import java.lang.annotation.*;

/**
 * 暴露服务的注解
 * 将使用了这个注解的服务注册到注册中心中
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcService {

    /**
     *  暴露服务接口类型
     * @return
     */
    // Class<?> interfaceType() default Object.class;

    /**
     * 暴露的服务名字
     *
     * @return
     */
    String name() default "";

    /**
     * 服务版本
     *
     * @return
     */
    String version() default "1.0";
}