package com.fps.rpc.client;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fps.rpc.client.grpc.GRPCClient;
import com.fps.rpc.client.netty.NettyClient;
import com.fps.rpc.utils.ConfigUtils;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.fps.rpc.utils.ConfigUtils.*;
import static com.fps.rpc.utils.ConfigUtils.RequestType.*;

/**
 * Created by fperez on 6/23/16.
 */
public class MainClient {

    private static final Logger LOG = LoggerFactory.getLogger(MainClient.class);

    public static final String GRPC_COMMAND = "grpc";
    public static final String NETTY_COMMAND = "netty";

    public static void main(String[] args) throws Exception {

        NettyClientParams nettyCommand = new NettyClientParams();
        GRPCClientParams grpcCommand = new GRPCClientParams();
        Map<String, Object> commands = new HashMap<>();
        commands.put(NETTY_COMMAND, nettyCommand);
        commands.put(GRPC_COMMAND, grpcCommand);
        String parsedCommand = parseCommandArgs(args, commands);

        CountDownLatch asyncResponsesReceived = new CountDownLatch(1);
        switch(parsedCommand.toLowerCase()) {
            case GRPC_COMMAND:
                try (GRPCClient grpcClient = new GRPCClient(grpcCommand, asyncResponsesReceived)) {

                    switch (grpcCommand.type) {
                        case SYNC:
                            grpcClient.checkTime();
                            break;
                        case ASYNC:
                            grpcClient.checkTimeAsync();
                            asyncResponsesReceived.await();
                            break;
                    }
                }
                break;
            case NETTY_COMMAND:
                try (TimeChecker nettyClient = new NettyClient(nettyCommand, asyncResponsesReceived)) {
                    nettyClient.checkTimeAsync();
                    asyncResponsesReceived.await();
                }
                break;
        }

    }

    @Parameters(commandDescription = "Client configuration for Netty")
    public static class NettyClientParams {

        @Parameter(names = "-serverHostPort", converter = HostAndPortConverter.class)
        public HostAndPort serverHostPort = HostAndPort.fromString("localhost:64444");

        @Parameter(names = "-nodeLevel")
        public int nodeLevel = 0;

    }

    @Parameters(commandDescription = "Client configuration for GRPC")
    public static class GRPCClientParams extends NettyClientParams {

        @Parameter(names = "-requestType", converter = RequestTypeConverter.class)
        public RequestType type = SYNC;

    }

}
