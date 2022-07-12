package com.moon.netty.rpc.transport.client;


import com.moon.netty.rpc.message.RpcRequestMessage;
import com.moon.netty.rpc.protocol.SequenceIdGenerator;
import com.moon.netty.rpc.protocol.codec.MessageCodecSharable;
import com.moon.netty.rpc.protocol.codec.ProcotolFrameDecoder;
import com.moon.netty.rpc.transport.client.handler.RpcResponseMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络通信部分，客户端连接管理
 */
@Slf4j
@Deprecated
public class NoBalanceRpcClient {
    // 消费方与服务提供方连接的管道
    private static volatile Channel channel = null; // todo 改成负载均衡之后，客户端可以与服务端建立多个channel

    // 异步接收RPC请求响应结果的Promise集合。
    // key : RPC请求消息的序号
    // value：是用来接收该请求消息结果的 promise 对象
    // 有的线程要向Map添加Promise, 有的线程要向Map里面删除Promise, 所以要使用线程安全的集合
    public static final Map<Integer, Promise<Object>> PROMISES;

    static {
        PROMISES = new ConcurrentHashMap<Integer, Promise<Object>>();
    }

    /**
     * 初始化管道
     * Todo 实现注册中心，可以与多个服务器建立多条管道
     */
    private static void initChannel() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseMessageHandler RPC_RESPONSE_HANDLER = new RpcResponseMessageHandler();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // log.debug("innitChannel 当前线程");
                ch.pipeline().addLast(new ProcotolFrameDecoder());
                ch.pipeline().addLast(LOGGING_HANDLER);
                ch.pipeline().addLast(MESSAGE_CODEC);
                ch.pipeline().addLast(RPC_RESPONSE_HANDLER);
            }
        });
        try {
            channel = bootstrap.connect("localhost", 8080).sync().channel();
            // 必须使用异步回调处理的方式，不能使用sync, 不然线程就一直在这里阻塞（innitChannel方法无法退出），等待管道关闭了
            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
            });
        } catch (Exception e) {
            log.error("client error", e);
        }
    }

    /*
     * 选择一个 channel 与 Server进行通信
     * Todo 实现负载均衡算法
     * 双重检查锁的单例模式
     */
    public static Channel getChannel() {
        if (channel == null) {
            synchronized (NoBalanceRpcClient.class) { //  t2
                if (channel == null) { // t1
                    initChannel();
                }
            }
        }
        return channel;
    }

    /**
     * 创建远程服务对象的代理类
     * 因为RPC框架是给使用者使用的，你总不能是让使用者自己使用netty给服务器发送一个RpcRequest吧
     * 所以应该在本地实现一个远程服务代理类，对方法进行增强
     * 新知识：静态的泛型方法不能通过实例对象去调用，只能通过类名调用
     *
     * @param serviceClass
     * @param <T>
     * @return
     */
    public static <T> T getProxyService(Class<T> serviceClass) {
        ClassLoader loader = serviceClass.getClassLoader();
        Class<?>[] interfaces = new Class[]{serviceClass};
        // sayHello  "张三"
        // 传入interface参数时不能传入serviceClass.getInterfaces, 不然会报异常转换异常（原因未知，按理说，对于本身就是接口类型的class对象，调用interfaces方法返回的应该是该接口本身才对啊），com.sun.proxy.$Proxy0 cannot be cast to com.moon.netty.rpc.transport.server.service.HelloService
        Object proxyInstance = Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
            // 代理方法实现逻辑
            // 1. 将方法调用转换为RpcRequest消息对象
            int sequenceId = SequenceIdGenerator.nextId();
            RpcRequestMessage msg = new RpcRequestMessage(
                    sequenceId,
                    1000,
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            // 2. 将消息对象发送出去
            getChannel().writeAndFlush(msg);

            // log.debug("当前线程"); // 当前线程还是main主线程

            // 3. 准备一个空 Promise 对象，来接收结果 (因为write线程和当前线程不是同一个线程，write方法也不是阻塞的，所以需要一个异步函数来接收rpc请求的响应结果)
            // 指定异步执行Listener的线程，在这里没有用上
            // Promise是一个结果容器，任何一个线程都可以向这个线程填充结果
            DefaultPromise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());
            NoBalanceRpcClient.PROMISES.put(sequenceId, promise); // 向线程安全的Map集合中添加Promise (序号用于标识每一次独立的RPC请求消息， 这样请求和响应的promise就对应起来了)

            // 4. 等待 promise 结果
            // 这一不能使用listener异步处理结果，我们必须使用同步的方式，阻塞主线程，直到RPC的结果返回
            // promise.addListener(future -> {
            //     // 线程
            //     // 执行这些异步代码的线程就是上面的getChannel().eventLoop()线程
            // });
            promise.await(); // 阻塞主线程，等待结果
            if (promise.isSuccess()) {
                // 调用正常
                return promise.getNow();
            } else {
                // 调用失败
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) proxyInstance; // 返回代理对象
    }

}
