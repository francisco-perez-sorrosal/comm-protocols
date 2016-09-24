package com.fps.rpc.akka; /**
 * Created by fperez on 6/22/16.
 */

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.codeg33ks.processing.TimeRequest;
import com.codeg33ks.processing.TimeResponse;
import com.fps.rpc.TimeProtocol;
import com.fps.rpc.UnixTime;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.time.Instant;

public class RequestForwarderActor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestForwarderActor.class);

    final ActorRef localTimeOracle;
    final ActorSelection remoteTimeOracle;

    RequestForwarderActor() {

        this.localTimeOracle = getContext().actorOf(Props.create(LocalTimeOracleActor.class), "oracle");
        this.remoteTimeOracle = getContext().actorSelection("akka.tcp://backActorSystem1@127.0.0.1:2552/user/timeOracle");
    }

    @Override
    public void preStart() {
        LOG.info("Actor {} prestarted", this);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof TimeProtocol.NettyTimeProtocol) {
            TimeProtocol.NettyTimeProtocol protocolMsg = (TimeProtocol.NettyTimeProtocol) msg;
            switch (protocolMsg.getCommand()) {
                case GET_TIME:
                    if (protocolMsg.getLevel() == 0) {
                        localTimeOracle.tell(protocolMsg, getSelf());
                    } else {
                        Future<Object> future = Patterns.ask(remoteTimeOracle, TimeRequest.newBuilder().setNodeLevel(protocolMsg.getLevel()).build(), new Timeout(Duration.create(5, "seconds")));
                        future.onSuccess(new OnSuccess<Object>() {

                            @Override
                            public void onSuccess(Object result) throws Throwable {
                                TimeResponse timeResponse = (TimeResponse) result;
                                final ChannelFuture f = protocolMsg.getCtx().writeAndFlush(new UnixTime(timeResponse.getTime(), timeResponse.getSignature()));
                                f.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) {
                                        LOG.info("Remote Unix time written to Netty channel {}", f.channel());
                                    }
                                });
                            }

                        }, getContext().dispatcher());
                    }
                    break;
                case TIME_DONE:
                    protocolMsg.getCtx().close();
                    LOG.info("Actor {} closed channel {}", getSender(), protocolMsg.getCtx().channel());
                    break;
            }
        } else if (msg instanceof TimeProtocol.GRPCTimeProtocol) {
            TimeProtocol.GRPCTimeProtocol request = (TimeProtocol.GRPCTimeProtocol) msg;
            if (request.getLevel() == 0) {
                LOG.info("Getting time from local");
                TimeResponse time = TimeResponse.newBuilder().setTime(Instant.now().getEpochSecond()).setSignature("local").build();
                request.getStreamObserver().onNext(time);
                request.getStreamObserver().onCompleted();
            } else {
                LOG.info("Getting time from remote");
                Future<Object> future = Patterns.ask(remoteTimeOracle, TimeRequest.newBuilder().setNodeLevel(request.getLevel()).build(), new Timeout(Duration.create(5, "seconds")));
                future.onSuccess(new OnSuccess<Object>() {

                    @Override
                    public void onSuccess(Object result) throws Throwable {
                        TimeResponse timeResponse = (TimeResponse) result;
                        request.getStreamObserver().onNext(TimeResponse.newBuilder().setTime(timeResponse.getTime()).setSignature(timeResponse.getSignature()).build());
                        request.getStreamObserver().onCompleted();
                    }

                }, getContext().dispatcher());

            }
        } else if (msg instanceof TimeResponse) {
            LOG.info("We can do also things with the response received! {}", (TimeResponse) msg);
        } else {
            unhandled(msg);
        }
    }

    @Override
    public void postStop() {
        LOG.info("Actor {} postStop", this);
    }

}