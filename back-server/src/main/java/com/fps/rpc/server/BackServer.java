package com.fps.rpc.server;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.beust.jcommander.Parameter;
import com.fps.rpc.server.akka.TimeOracleActor;
import com.fps.rpc.utils.ConfigUtils;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.fps.rpc.utils.ConfigUtils.*;

@Singleton
public class BackServer extends AbstractIdleService {

    public static final String DASH_SEPARATOR_80_CHARS =
            "--------------------------------------------------------------------------------";

    private static final Logger LOG = LoggerFactory.getLogger(BackServer.class);

    @Inject
    BackServerConfig serverConfig;

    @Inject
    private ActorSystem actorSystem;
    private ActorRef timerActor;

    // ----------------------------------------------------------------------------------------------------------------

    static BackServer getInitializedTsoServer(BackServerConfig commands) throws IOException {
        LOG.info("Configuring Server...");
        Injector injector = Guice.createInjector(buildModuleList(commands));
        LOG.info("Server configured. Creating instance...");
        return injector.getInstance(BackServer.class);
    }

    private static List<Module> buildModuleList(final BackServerConfig commands) throws IOException {

        List<Module> guiceModules = new ArrayList<>();
        guiceModules.add(new BackServerModule(commands));
        return guiceModules;

    }

    // ----------------------------------------------------------------------------------------------------------------
    // AbstractIdleService implementation
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * This is where all starts on the server side
     */
    public static void main(String[] args) {

        BackServerConfig akkaConfCommand = new BackServerConfig();
        parseCommandArgs(args, akkaConfCommand);

        try {
            BackServer server = getInitializedTsoServer(akkaConfCommand);
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
        LOG.info("Starting Remote Server");

        if ("none".equals(serverConfig.remoteActor)) {
            timerActor = actorSystem.actorOf(Props.create(TimeOracleActor.class,
                                                          Optional.empty(),
                                                          serverConfig.nodeLevel), "timeOracle");
        } else {
            ActorSelection remoteTimeActor = actorSystem.actorSelection(serverConfig.remoteActor);
            timerActor = actorSystem.actorOf(Props.create(TimeOracleActor.class,
                                                          Optional.of(remoteTimeActor),
                                                          serverConfig.nodeLevel), "timeOracle");
        }
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
    }

    // ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void shutDown() throws Exception {
        LOG.info("{}", DASH_SEPARATOR_80_CHARS);
        LOG.info("Shutting Down Server");
        LOG.info("\tTerminating Actor {}", timerActor);
        timerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
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

    public static class BackServerConfig {

        @Parameter(names = "-actorSystemName")
        public String actorSystemName = "remoteActorSystem";

        @Parameter(names = "-conf")
        public String akkaConf = "defaultConf";

        @Parameter(names = "-remoteActor")
        public String remoteActor = "none";

        @Parameter(names = "-nodeLevel")
        public int nodeLevel = 0;

    }

}
