package com.fps.rpc;

import com.codeg33ks.processing.TimeResponse;
import io.grpc.stub.StreamObserver;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by fperez on 6/22/16.
 */
public class TimeProtocol {

    public enum Command {
        GET_TIME, TIME_DONE
    }

    private final Command command;
    private final int level;

    public TimeProtocol(Command command, int level) {

        this.command = command;
        this.level = level;

    }

    public Command getCommand() {

        return command;

    }

    public int getLevel() {

        return level;

    }

    public static class NettyTimeProtocol extends TimeProtocol {

        private final ChannelHandlerContext ctx;

        public NettyTimeProtocol(Command command, int level, ChannelHandlerContext ctx) {

            super(command, level);
            this.ctx = ctx;

        }

        public ChannelHandlerContext getCtx() {

            return ctx;

        }

    }

    public static class GRPCTimeProtocol extends TimeProtocol {

        private final StreamObserver<TimeResponse> responseObserver;

        public GRPCTimeProtocol(Command command, int level, StreamObserver<TimeResponse> responseObserver) {

            super(command, level);
            this.responseObserver = responseObserver;

        }

        public StreamObserver<TimeResponse> getStreamObserver() {

            return responseObserver;

        }

    }

}
