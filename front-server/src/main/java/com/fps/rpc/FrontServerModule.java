package com.fps.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fps.rpc.akka.RequestForwarderActor;
import com.fps.rpc.grpc.TimerService;
import com.fps.rpc.netty.ControlCommandManager;
import com.fps.rpc.netty.NettyChannelManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.inject.Named;
import javax.inject.Singleton;

class FrontServerModule extends AbstractModule {

    private final FrontServer.FrontServerConfig serverConf;

    FrontServerModule(final FrontServer.FrontServerConfig serverConf) {
        this.serverConf = serverConf;
    }

    @Override
    protected void configure() {

        bind(ControlCommandManager.class).in(Singleton.class);
        bind(NettyChannelManager.class).in(Singleton.class);

    }

    @Provides
    FrontServer.FrontServerConfig provideServerConfig() {
        return serverConf;
    }

    @Provides
    @Singleton
    ActorSystem provideAkkaActorSystem() {
        return ActorSystem.create(serverConf.actorSystemName);
    }

    @Provides
    @Singleton
    @Named("requestForwarderActor")
    ActorRef provideRequestForwarderActor(ActorSystem actorSystem) {
        return actorSystem.actorOf(Props.create(RequestForwarderActor.class), "requestForwarder");
    }

    @Provides
    @Singleton
    Server provideGRPCServer(@Named("requestForwarderActor") ActorRef actor) {
        return ServerBuilder.forPort(serverConf.grpcPort).addService(new TimerService(actor)).build();
    }

}
