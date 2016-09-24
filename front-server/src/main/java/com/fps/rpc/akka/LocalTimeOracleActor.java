package com.fps.rpc.akka;

import akka.actor.UntypedActor;
import com.fps.rpc.TimeProtocol;
import com.fps.rpc.UnixTime;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fperez on 6/22/16.
 */
public class LocalTimeOracleActor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTimeOracleActor.class);

    public LocalTimeOracleActor() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof TimeProtocol.NettyTimeProtocol) {
            TimeProtocol.NettyTimeProtocol protocolMsg = (TimeProtocol.NettyTimeProtocol) msg;
            switch (protocolMsg.getCommand()) {
                case GET_TIME:
                    final ChannelFuture f = protocolMsg.getCtx().writeAndFlush(new UnixTime(getSelf().path().toString()));
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            LOG.info("Unix time written to channel {}", f.channel());
                        }
                    });
                    break;
                case TIME_DONE:
                    LOG.info("Sender {}", getSender());
                    getSender().tell(new TimeProtocol.NettyTimeProtocol(TimeProtocol.Command.TIME_DONE, 0, protocolMsg.getCtx()), getSelf());
                    break;
            }
        } else if (msg instanceof TimeProtocol.NettyTimeProtocol) {

        } else {
            unhandled(msg);
        }
    }

    @Override
    public void postStop() {
        LOG.info("Actor {} postStop", this);
    }

}
