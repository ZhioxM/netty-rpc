package com.moon.netty.rpc.transport.client;

import com.moon.netty.rpc.protocol.codec.MessageCodecSharable;
import com.moon.netty.rpc.protocol.codec.ProcotolFrameDecoder;
import com.moon.netty.rpc.transport.client.handler.HeartBeatClientHandler;
import com.moon.netty.rpc.transport.client.handler.RpcResponseMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 提供客户端连接
 * 网络连接的部分在此实现
 *
 * @author ziyang
 */
@Slf4j
public class ChannelProvider {
    public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private static final Bootstrap bootstrap = initializeBootstrap();
    private static final Map<String, Channel> channels = new ConcurrentHashMap<>(); // 客户端通道合集

    /**
     * 根据地址获取Channel，如果这个channel不存在或者已经断开，则重新连接
     *
     * @param inetSocketAddress
     * @return
     * @throws InterruptedException
     */
    public static Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if (channels.containsKey(key)) {
            // 连接存在
            Channel channel = channels.get(key);
            // 连接有效
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                // 连接失效了，则从channel集合中删除
                channels.remove(key);
            }
        }

        // 有必要为每次连接的通道设置不同的handle吗
        // bootstrap.handler(new ChannelInitializer<SocketChannel>() {
        //     @Override
        //     protected void initChannel(SocketChannel ch) {
        //         /*自定义序列化编解码器*/
        //         // RpcResponse -> ByteBuf
        //         ch.pipeline().addLast(new CommonEncoder(serializer))
        //                 .addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
        //                 .addLast(new CommonDecoder())
        //                 .addLast(new NettyClientHandler());
        //     }
        // });

        // 连接不存在或者失效，则重新建立连接
        Channel channel = null;
        try {
            log.debug("重新连接");
            channel = connect(bootstrap, inetSocketAddress);
        } catch (ExecutionException e) {
            log.error("连接客户端时有错误发生", e);
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channels.put(key, channel);
        return channel;
    }

    /**
     * 建立连接
     *
     * @param bootstrap
     * @param inetSocketAddress
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private static Channel connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("客户端连接成功!");
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get(); // 阻塞，直到连接建立成功
    }

    /**
     * 初始化bootstrap
     *
     * @return
     */
    private static Bootstrap initializeBootstrap() {
        System.out.println("init bootstrap");
        Bootstrap bootstrap = new Bootstrap();
        //日志handler
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        //消息处理handler
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        //处理相应handler
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        //心跳处理器
        HeartBeatClientHandler HEARTBEAT_CLIENT = new HeartBeatClientHandler();
        bootstrap.group(eventLoopGroup)
                 .channel(NioSocketChannel.class)
                 //连接的超时时间，超过这个时间还是建立不上的话则代表连接失败
                 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                 //是否开启 TCP 底层心跳机制
                 // .option(ChannelOption.SO_KEEPALIVE, true)
                 //TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                 // .option(ChannelOption.TCP_NODELAY, true)
                 .handler(new ChannelInitializer<SocketChannel>() {
                     @Override
                     protected void initChannel(SocketChannel ch) throws Exception {
                         // 空闲检测处理器
                         // 参数1为读空闲时间，即在这个时间段内没有收到服务端发来的消息，就会触发READER_IDLE事件
                         // 参数2为写空闲时间，即在这个时间段内没有向服务端发送消息消息，就会触发WRITER_IDLE
                         // 参数3为读和写加起来的空闲时间
                         ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                         //定长解码器
                         ch.pipeline().addLast(new ProcotolFrameDecoder());
                         ch.pipeline().addLast(MESSAGE_CODEC);
                         ch.pipeline().addLast(LOGGING_HANDLER);
                         // 发送心跳包的处理器，维持与与服务端的连接
                         // ch.pipeline().addLast(HEARTBEAT_CLIENT);
                         ch.pipeline().addLast(RPC_HANDLER);
                     }
                 });
        return bootstrap;
    }

}
