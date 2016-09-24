package com.fps.rpc.client.netty;

import com.fps.rpc.client.MainClient;
import com.fps.rpc.client.TimeChecker;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created by fperez on 6/23/16.
 */
public class NettyClient implements TimeChecker {

    private static final Logger LOG = LoggerFactory.getLogger(NettyClient.class);

    private final String host;
    private final int port;
    private final int nodeLevel;

    private final Bootstrap boot;
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private ChannelFuture f;
    private CountDownLatch asyncResponsesReceived;

    public NettyClient(MainClient.NettyClientParams nettyCommand, CountDownLatch asyncResponsesReceived)
            throws InterruptedException {

        this.asyncResponsesReceived = asyncResponsesReceived;

        this.host = nettyCommand.serverHostPort.getHostText();
        this.port = nettyCommand.serverHostPort.getPort();
        this.nodeLevel = nettyCommand.nodeLevel;

        boot = new Bootstrap();
        boot.group(workerGroup);
        boot.channel(NioSocketChannel.class);
        boot.option(ChannelOption.SO_KEEPALIVE, true);
        boot.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new StringEncoder());
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new TimeClientHandler());
            }
        });

    }

    @Override
    public void checkTime() {

        throw new UnsupportedOperationException();

    }

    @Override
    public void checkTimeAsync() throws Exception {

        LOG.info("Checking time asynchronously. Target host: {}:{}", host, port);

        try {
            f = boot.connect(host, port).sync();
            f.channel().writeAndFlush("TIME " + nodeLevel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }

    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }

    public class TimeClientHandler extends SimpleChannelInboundHandler<String> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

            LOG.info("Time checked asynchronously: ", msg);
            ctx.close();
            asyncResponsesReceived.countDown();

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

            cause.printStackTrace();
            ctx.close();
            asyncResponsesReceived.countDown();

        }

    }

}
