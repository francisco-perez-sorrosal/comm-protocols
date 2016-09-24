package com.fps.rpc.server;

import akka.actor.ActorSystem;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Singleton;

class BackServerModule extends AbstractModule {

    private final BackServer.BackServerConfig serverConf;

    BackServerModule(final BackServer.BackServerConfig serverConf) {
        this.serverConf = serverConf;
    }

    @Override
    protected void configure() {

    }

    @Provides
    BackServer.BackServerConfig provideServerConfig() {
        return serverConf;
    }

    @Provides
    @Singleton
    ActorSystem provideAkkaActorSystem() {

        Config rootConfig = ConfigFactory.load();

        return ActorSystem.create(serverConf.actorSystemName, rootConfig.getConfig(serverConf.akkaConf));

    }

}
