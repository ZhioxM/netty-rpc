package com.moon.netty.rpc;

import com.moon.netty.rpc.annotation.server.RpcServiceScan;
import com.moon.netty.rpc.transport.server.RpcServer;

@RpcServiceScan
public class RpcProvider {
    public static void main(String[] args) {
        // 启动一个服务器
        new RpcServer("127.0.0.1", 8082).start();
    }
}
