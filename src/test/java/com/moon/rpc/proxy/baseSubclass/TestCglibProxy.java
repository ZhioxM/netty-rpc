package com.moon.rpc.proxy.baseSubclass;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Author: Mzx
 * @Date: 2022/6/10 23:27
 */
public class TestCglibProxy {
    public static void main(String[] args) {
        RealSubject realSubject = new RealSubject();

        RealSubject cglibProxySubject = (RealSubject) Enhancer.create(realSubject.getClass(), new MethodInterceptor() {
            /**
             *
             * @param o 被代理对象
             * @param method 调用的方法
             * @param objects 方法参数
             * @param methodProxy 当前执行方法的代理对象
             * @return 方法返回值，可以在真实方法返回值的基础上进行增强
             * @throws Throwable
             */
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                // 调用前增强
                System.out.println("调用前增强");
                Object returnVal = method.invoke(realSubject, objects);
                // 调用后增强
                System.out.println("调用后增强");
                return returnVal;
            }
        });

        cglibProxySubject.hello("zhangsan");
    }
}
