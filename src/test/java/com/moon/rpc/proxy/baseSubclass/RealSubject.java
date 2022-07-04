package com.moon.rpc.proxy.baseSubclass;

/**
 * @Author: Mzx
 * @Date: 2022/6/10 22:39
 */
public class RealSubject {
    public void hello(String str) {
        System.out.println("hello, " + str);
    }

    public String bye() {
        System.out.println("bye");
        return "over";
    }
}
