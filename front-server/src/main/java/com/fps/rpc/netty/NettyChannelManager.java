package com.fps.rpc.netty;

import com.fps.rpc.UnixTime;
import com.google.common.util.concurrent.AbstractIdleService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.fps.rpc.FrontServer.FrontServerConfig;

/**
 * Incoming requests are processed in this class
 */
public class NettyChannelManager extends AbstractIdleService implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NettyChannelManager.class);

    private final FrontServerConfig serverConfig;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final ServerBootstrap b;
    private ChannelFuture listeningChannelF;

    @Inject
    public NettyChannelManager(FrontServerConfig serverConfig, ControlCommandManager commandManager)
            throws InterruptedException {

        this.serverConfig = serverConfig;

        // Netty
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.b = new ServerBootstrap();
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class);
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new TimeEncoder());
                ch.pipeline().addLast(new StringEncoder());
                ch.pipeline().addLast(new ProtocolHandler(commandManager));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, 128);
        b.childOption(ChannelOption.SO_KEEPALIVE, true);

    }

    // ----------------------------------------------------------------------------------------------------------------
    // AbstractIdleService implementation
    // ----------------------------------------------------------------------------------------------------------------

    @Override
    public void startUp() throws Exception {
        LOG.info("Starting Netty Channel Manager");
        // Bind and start to accept incoming connections.
        try {
            listeningChannelF = b.bind(serverConfig.nettyPort).sync();
            LOG.info("Listening channel {} created and bind to port {}",
                     listeningChannelF.channel(),
                     serverConfig.nettyPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutDown() throws Exception {
        close();
        LOG.info("Netty Channel Manager stopped");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Closeable implementation
    // ----------------------------------------------------------------------------------------------------------------
    @Override
    public void close() throws IOException {

        try {
            LOG.info("Closing Netty Channel {}", listeningChannelF.channel());
            // We need to close the listening channel first, otherwise the sync() below will not return
            // See http://stackoverflow.com/questions/28032092/shutdown-netty-programmatically
            listeningChannelF.channel().close();
            listeningChannelF.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } finally {
            LOG.info("Gracefully shutdown Netty structures");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

    // ----------------------------------------------------------------------------------------------------------------
    // Helper methods and classes
    // ----------------------------------------------------------------------------------------------------------------

    public static class TimeEncoder extends MessageToByteEncoder<UnixTime> {

        @Override
        protected void encode(ChannelHandlerContext ctx, UnixTime msg, ByteBuf out) {
            LOG.info("Encoding time into string {}", msg.value());
            out.writeCharSequence(msg.toString() + "\n", Charset.defaultCharset());
        }

    }


    public class ProtocolHandler extends SimpleChannelInboundHandler<String> {

        private ControlCommandManager commandManager;


        public ProtocolHandler(ControlCommandManager commandManager) {
            this.commandManager = commandManager;
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {

            LOG.info("Channel active {}", ctx.channel());

        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

            commandManager.parseAndExecute(ctx, msg);

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {

            LOG.info("Channel inactive {}", ctx.channel());

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }

    }

}