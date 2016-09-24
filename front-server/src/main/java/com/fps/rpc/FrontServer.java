package com.fps.rpc;

import akka.actor.ActorSystem;
import com.beust.jcommander.Parameter;
import com.fps.rpc.netty.NettyChannelManager;
import com.fps.rpc.utils.ConfigUtils;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FrontServer extends AbstractIdleService {

    public static final String DASH_SEPARATOR_80_CHARS =
            "--------------------------------------------------------------------------------";
    private static final Logger LOG = LoggerFactory.getLogger(FrontServer.class);
    @Inject
    private ActorSystem actorSystem;

    @Inject
    private NettyChannelManager channelManager;

    @Inject
    private Server server;

    // ----------------------------------------------------------------------------------------------------------------

    static FrontServer getInitializedServer(FrontServerConfig commands) throws IOException {
        LOG.info("Configuring Server...");
        Injector injector = Guice.createInjector(buildModuleList(commands));
        LOG.info("Server configured. Creating instance...");
        return injector.getInstance(FrontServer.class);
    }

    private static List<Module> buildModuleList(final FrontServerConfig commands) throws IOException {

        List<Module> guiceModules = new ArrayList<>();
        guiceModules.add(new FrontServerModule(commands));
        return guiceModules;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // AbstractIdleService implementation
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * This is where all starts on the server side
     */
    public static void main(String[] args) {

        FrontServerConfig frontServerConfig = new FrontServerConfig();

        ConfigUtils.parseCommandArgs(args, frontServerConfig);

        try {
            FrontServer server = getInitializedServer(frontServerConfig);
            server.attachShutDownHook();
            server.startUp();
            server.awaitTerminated();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }

    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("Starting Server");
        channelManager.startUp();
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("Starting GRPC Server");
        server.start();
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
    }

    // ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void shutDown() throws Exception {
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("Shutting Down Server");
        LOG.info("\tTerminating Channel Manager");
        channelManager.shutDown();
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("\tStoping GRPC Server");
        server.shutdown();
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("\tTerminating Actor System {}", actorSystem.name());
        actorSystem.terminate();
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("Server stopped");
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);

    }

    private void attachShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    shutDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        LOG.info("Shutdown Hook Attached");
    }

    public static class FrontServerConfig {

        @Parameter(names = "-nettyPort")
        public Integer nettyPort = 64444;

        @Parameter(names = "-grpcPort")
        public Integer grpcPort = 65444;

        @Parameter(names = "-actorSystemName")
        public String actorSystemName = "frontServerActorSystem";

    }

}
