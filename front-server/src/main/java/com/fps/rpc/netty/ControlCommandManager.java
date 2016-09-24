package com.fps.rpc.netty;

import akka.actor.*;
import com.fps.rpc.TimeProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

import static com.fps.rpc.netty.ControlCommandManager.ControlProtocol.UNKNOWN;
import static com.fps.rpc.netty.ControlCommandManager.ControlProtocol.fromString;

/**
 * Created by fperez on 7/7/16.
 */
public class ControlCommandManager {

    private static final Logger LOG = LoggerFactory.getLogger(ControlCommandManager.class);

    enum ControlProtocol {

        TIME, DONE, UNKNOWN;

        private static final Logger LOG = LoggerFactory.getLogger(ControlProtocol.class);

        public static Optional<ControlProtocol> fromString(String text) {
            text = text.replaceAll("[\n\r]", "");
            if (text != null) {
                for (ControlProtocol b : ControlProtocol.values()) {
                    if (text.equalsIgnoreCase(b.name())) {
                        return Optional.of(b);
                    }
                }
            }
            return Optional.empty();
        }

    }

    private final ActorRef requestForwarderActor;

    @Inject
    public ControlCommandManager(@Named("requestForwarderActor")ActorRef requestForwarderActor) {

        this.requestForwarderActor = requestForwarderActor;

    }

    public void parseAndExecute(ChannelHandlerContext ctx, String controlCommandAsString) {

        controlCommandAsString = controlCommandAsString.replaceAll("(?:\\n|\\r)", "");
        String[] tokens = controlCommandAsString.split(" ");
        if (tokens.length <= 2) {
            ControlProtocol controlCommand = fromString(tokens[0]).orElse(UNKNOWN);
            switch (controlCommand) {
                case TIME:
                    int level = 0;
                    try {
                        level = Integer.parseInt(tokens[1]);
                    } catch(Exception e) {
                        LOG.error("Error parsing level for TIME command. The default value [0] will be used");
                    }
                    requestForwarderActor.tell(new TimeProtocol.NettyTimeProtocol(TimeProtocol.Command.GET_TIME, level, ctx), ActorRef.noSender());
                    break;
                case DONE:
                    ctx.writeAndFlush("Channel with server closed. Come back soon!");
                    ctx.close();
                    break;
                default:
                    LOG.error("Unknown control command received {}", controlCommandAsString);
                    ctx.writeAndFlush("Unknown command: " + controlCommandAsString + "Options: TIME [level] | CLOSE");
            }
        } else {
            ctx.writeAndFlush("Wrong number of arguments. Options: TIME [level] | CLOSE");
        }

    }
}
