package com.moon.netty.rpc.transport.server;


import com.moon.netty.rpc.protocol.codec.MessageCodecSharable;
import com.moon.netty.rpc.protocol.codec.ProcotolFrameDecoder;
import com.moon.netty.rpc.transport.server.handler.RpcRequestMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Deprecated
public class DeprecatedRpcServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        // 下面这三个Handler是可以被多个人管道锁共享的，所以只需要创建一个对象
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcRequestMessageHandler RPC_REQUEST_HANDLER = new RpcRequestMessageHandler();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProcotolFrameDecoder()); // InboundHandler 长度字段解码器，解决粘包拆包的问题
                    ch.pipeline().addLast(LOGGING_HANDLER); // 日志处理器
                    ch.pipeline().addLast(MESSAGE_CODEC); // In/Out 自定义协议 编码器 解码器
                    ch.pipeline().addLast(RPC_REQUEST_HANDLER); // Inbound 处理RPC请求的Handler
                }
            });
            Channel channel = serverBootstrap.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
