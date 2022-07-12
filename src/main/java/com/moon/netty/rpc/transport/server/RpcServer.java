package com.moon.netty.rpc.transport.server;

import com.moon.netty.rpc.annotation.server.RpcService;
import com.moon.netty.rpc.annotation.server.RpcServiceScan;
import com.moon.netty.rpc.protocol.codec.MessageCodecSharable;
import com.moon.netty.rpc.protocol.codec.ProcotolFrameDecoder;
import com.moon.netty.rpc.provider.ServiceProvider;
import com.moon.netty.rpc.registery.ServiceRegistry;
import com.moon.netty.rpc.registery.impl.NacosServiceRegistry;
import com.moon.netty.rpc.transport.server.handler.HeartBeatServerHandler;
import com.moon.netty.rpc.transport.server.handler.RpcRequestMessageHandler;
import com.moon.netty.rpc.utils.PackageScanUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Rpc 服务器实例
 */
@Slf4j
public class RpcServer {
    private final String host; // 对于本机，只能是localhost
    private final int port;

    private final ServiceRegistry serviceRegistry; // 服务注册表
    private final ServiceProvider serviceProvider; // 服务工厂

    NioEventLoopGroup boss = new NioEventLoopGroup();
    NioEventLoopGroup work = new NioEventLoopGroup();
    ServerBootstrap serverBootstrap = new ServerBootstrap();

    public RpcServer(String host, int port) {
        this.host = host;
        this.port = port;
        serviceRegistry = new NacosServiceRegistry();
        serviceProvider = new ServiceProvider();
        // 扫描注解，自动注册服务
        scanServices();
    }

    /**
     * 启动服务器
     */
    public void start() {
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcRequestMessageHandler RPC_REQUEST_HANDLER = new RpcRequestMessageHandler();
        HeartBeatServerHandler HEART_BEAT_SERVER_HANDLER = new HeartBeatServerHandler();
        try {
            serverBootstrap.group(boss, work)
                           .channel(NioServerSocketChannel.class)
                           .option(ChannelOption.SO_BACKLOG, 256)
                           .option(ChannelOption.SO_KEEPALIVE, true)
                           .option(ChannelOption.TCP_NODELAY, true)
                           .childHandler(new ChannelInitializer<NioSocketChannel>() {
                               @Override
                               protected void initChannel(NioSocketChannel ch) throws Exception {
                                   ch.pipeline()
                                     // 服务端的readerIDleTime和客户端的writerIdleTime一般是两倍的关系
                                     .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS)) // 这个时间是什么时间？
                                     .addLast(new ProcotolFrameDecoder())
                                     .addLast(MESSAGE_CODEC)
                                     .addLast(LOGGING_HANDLER) // 这个和上面一个的顺序对功能没有影响，只是控制台打印效果不同而已
                                     .addLast(HEART_BEAT_SERVER_HANDLER) // 长时间没有接收到客户端的数据，说明网络有可能真的断开了，所以释放掉这条通道。而且不会出现连接假死的问题，因为客户端一直在发送数心跳包，如果连心跳包都没收到，说明连接大概率断开了
                                     // .addLast(new HeartBeatServerHandler())
                                     .addLast(RPC_REQUEST_HANDLER);
                               }
                           });
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(this.host, this.port)).sync();
            log.info("服务器{}:{}成功启动...", this.host, this.port);
            Channel channel = channelFuture.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("启动服务器时有错误发生: ", e);
        } finally {
            Future<?> bossCloseFuture = boss.shutdownGracefully();
            Future<?> workerCloseFuture = work.shutdownGracefully();
            try {
                bossCloseFuture.sync();
                workerCloseFuture.sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("服务端优雅关闭");
        }
    }


    /**
     * 自动扫描 @RpcService注解  注册服务
     */
    private void scanServices() {
        String mainClassName = PackageScanUtils.getStackTrace();
        Class<?> startClass;
        try {
            startClass = Class.forName(mainClassName);
            if (!startClass.isAnnotationPresent(RpcServiceScan.class)) {
                throw new RuntimeException("启动类缺少@RpcServer 注解");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("启动类为找到");
        }
        String basePackage = startClass.getAnnotation(RpcServiceScan.class).value();
        if ("".equals(basePackage)) {
            basePackage = mainClassName.substring(0, mainClassName.lastIndexOf("."));
        }
        Set<Class<?>> classSet = PackageScanUtils.getClasses(basePackage);
        for (Class<?> clazz : classSet) {
            if (clazz.isAnnotationPresent(RpcService.class)) {
                // 在使用注解时，通过name属性传入服务的名字
                String serviceName = clazz.getAnnotation(RpcService.class).name();
                Object obj;
                try {
                    // 实例化一个服务类的实例对象，存入ServiceProvider（服务端本地工厂），之后通过反射调用本地方法
                    obj = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("创建 " + clazz + " 时有错误发生");
                    continue;
                }
                if ("".equals(serviceName)) {
                    // 如果使用注解时没有传入name属性，则它的名字使用它的父接口的名字
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> oneInterface : interfaces) {
                        publishService(obj, oneInterface.getCanonicalName());
                    }
                    // 我觉得使用类的全类名更好
                    // publishService(obj, clazz.getName());
                } else {
                    publishService(obj, serviceName);
                }
            }
        }
    }

    private <T> void publishService(T service, String serviceName) {
        // 服务对象添加进本地服务工厂
        serviceProvider.addServiceProvider(service, serviceName);
        // 添加到服务注册中心
        serviceRegistry.register(serviceName, new InetSocketAddress(host, port));
    }
}
