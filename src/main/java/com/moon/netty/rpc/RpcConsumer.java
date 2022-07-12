package com.moon.netty.rpc;

import com.moon.netty.rpc.service.HelloService;
import com.moon.netty.rpc.service.OrderService;
import com.moon.netty.rpc.transport.client.RpcClient;
import com.moon.netty.rpc.transport.client.RpcClientProxy;

// TODO 服务器不终止，启动客户端后，然后关闭，之后又开启，提示c.m.n.r.t.c.h.HeartBeatClientHandler - channelUnregistered，但是如果把服务端的HEART_BEAT_SERVER_HANDLER删掉的话，就没有这个问题
// 但是如果去掉了HEART_BEAT_SERVER_HANDLER，客户端发送心跳包的还有什么意思呢
// UNREGISTERED是什么意思？
// 即使HEART_BEAT_SERVER_HANDLER内一行代码都没有，还是会有这个问题，所以我觉得问题可能出现在ChannelDuplexHandler上
// 解决方案，给HEART_BEAT_SERVER_HANDLER加上sharable注解，或者添加的时候new一个
public class RpcConsumer {
    public static void main(String[] args) {
        // 通过代理的方式封装了netty通信的过程，使得用户只关注于框架的使用
        RpcClient rpcClient = new RpcClient();
        // 封装通信过程（其实获取的不是rpcClient对象的代理对象，而是获取服务的代理对象）
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient); // 客户端代理对象，通过动态代理封装了远程通信的过程
        // 获取服务的代理对象
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        // TODO : 上面那个远程代用出错之后，下面这个远程调用居然没有成功调用: 解决了，原因是因为Promise异步结果是调用出错时会抛出一个运行时异常，在RpcClientProxy的60行
        //OrderService orderService = rpcClientProxy.getProxy(OrderService.class);
        // 通过代理对象发起RPC请求
        System.out.println(helloService.sayHello("zhangsan"));
        // try {
        //     System.out.println(helloService.sayHello("zhangsan"));
        // } catch (Exception e) {
        //     System.out.println(1);
        // }
        //System.out.println("张三下单成功，订单号是" + orderService.sumbit("张三"));
        // HelloService helloService = RpcClientProxy.getProxy(HelloService.class);
        // String msg = helloService.sayHello("张三");
        // System.out.println(msg);
        // System.out.println(NoBalanceRpcClient.getProxyService(HelloService.class).sayHello("zahngsan"));
    }
}
