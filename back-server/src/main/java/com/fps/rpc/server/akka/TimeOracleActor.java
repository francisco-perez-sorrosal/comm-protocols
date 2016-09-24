package com.fps.rpc.server.akka;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import com.codeg33ks.processing.TimeRequest;
import com.codeg33ks.processing.TimeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * Created by fperez on 6/22/16.
 */
public class TimeOracleActor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(TimeOracleActor.class);

    private final Optional<ActorSelection> remoteActor;
    private int myNodeLevel;

    public TimeOracleActor(Optional<ActorSelection> remoteActor, int nodeLevel) {
        this.remoteActor = remoteActor;
        this.myNodeLevel = nodeLevel;
        LOG.info("Creating actor {} of node level {}", getSelf().path(), nodeLevel);
        if (remoteActor.isPresent()) {
            LOG.info("\twith remote actor: {}", remoteActor.get());
        }
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof TimeRequest) {
            TimeRequest timeRequest = (TimeRequest) msg;
            LOG.info("Remote time Request received in level {}! Sender: {}", myNodeLevel, getSender());
            if(myNodeLevel == timeRequest.getNodeLevel()) {
                LOG.info("\tSending back result to client");
                getSender().tell(buildTimeResponse(), getSelf());
            } else {
                if (remoteActor.isPresent()) {
                    LOG.info("\tForwarding to remote actor in next level");
                    remoteActor.get().forward(timeRequest, getContext());
                } else {
                    LOG.info("\tThere are no more node levels to redirect. Sending back result to client");
                    getSender().tell(buildTimeResponse(), getSelf());
                }
            }
        } else {
            LOG.info("Unhandled message {}", msg);
            unhandled(msg);
        }
    }

    @Override
    public void postStop() {
        LOG.info("Actor {} postStop", this);
    }

    private TimeResponse buildTimeResponse() {
        return TimeResponse.newBuilder().setTime(Instant.now().getEpochSecond()).setSignature(getSelf().path().toString()).build();
    }

}
