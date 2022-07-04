package com.moon.rpc.proxy.baseInterface;

/**
 * @Author: Mzx
 * @Date: 2022/6/10 22:39
 */
public class RealSubject implements Subject {
    @Override
    public void hello(String str) {
        System.out.println("hello, " + str);
    }

    @Override
    public String bye() {
        System.out.println("bye");
        return "over";
    }
}
