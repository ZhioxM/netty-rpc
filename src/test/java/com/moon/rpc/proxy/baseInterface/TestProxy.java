package com.moon.rpc.proxy.baseInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author: Mzx
 * @Date: 2022/6/10 22:46
 */
public class TestProxy {
    public static void main(String[] args) {
        // 被代理对象
        Subject realSubject = new RealSubject();

        /*
         * 通过Proxy的newProxyInstance方法来创建代理对象，我们来看看其三个参数
         * 第一个参数realSubject.getClass().getClassLoader() ，我们这里使用realSubject这个类的ClassLoader对象来加载我们的代理对象
         * 第二个参数realSubject.getClass().getInterfaces()，我们这里为代理对象提供的接口是被代理对象所实现的接口，表示我要代理的是该被代理对象，这样我就能调用这组接口中的方法了
         * 第三个参数handler， 我们这里将这个代理对象关联到了上方的 InvocationHandler 这个对象上
         */
        // 生成代理对象
        Subject proxySubject = (Subject) Proxy.newProxyInstance(
                realSubject.getClass().getClassLoader(),
                realSubject.getClass().getInterfaces(),
                // 实现InvocationHandler, 定义代理增强的代码逻辑
                new InvocationHandler() {
                    /**
                     * @param proxy 被代理的对象
                     * @param method 被调用的方法
                     * @param args 方法参数
                     * @return
                     * @throws Throwable
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 在代理真实对象前我们可以添加一些自己的操作
                        // 同时可以判断method的名字，随不同的方法进行不同的增强
                        System.out.println("Before method");
                        System.out.println("Call Method: " + method);

                        // 当代理对象调用真实对象的方法时，其会自动的跳转到代理对象关联的handler对象的invoke方法来进行调用
                        Object obj = method.invoke(realSubject, args); // obj是方法的返回值，如果方法返回值为void，则obj == null

                        // 在代理真实对象后我们也可以添加一些自己的操作
                        System.out.println("After method");
                        return obj;
                    }
                });

        System.out.println(proxySubject.getClass().getName()); // 看看代理对象的所属的类：com.sun.proxy.$Proxy0

        proxySubject.hello("World"); // 会调用invoke方法
        String result = proxySubject.bye();
        System.out.println("Result is: " + result);

    }
}
