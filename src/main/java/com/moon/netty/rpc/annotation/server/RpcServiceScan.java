package com.moon.netty.rpc.annotation.server;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于包扫描用来启动类上
 * 默认是扫描启动类上一级包中所有的类
 *
 * @author chenlei
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcServiceScan {

    public String value() default "";
}
