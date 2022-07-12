package com.moon.netty.rpc.service.impl;

import com.moon.netty.rpc.annotation.server.RpcService;
import com.moon.netty.rpc.service.HelloService;

@RpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String msg) {
        // 验证RPC调用异常
        // int i = 1 / 0;
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "你好, " + msg;
    }
}