package com.fps.rpc.client.grpc;

import com.codeg33ks.processing.TimeRequest;
import com.codeg33ks.processing.TimeResponse;
import com.codeg33ks.processing.TimeServiceGrpc;
import com.fps.rpc.UnixTime;
import com.fps.rpc.client.MainClient;
import com.fps.rpc.client.TimeChecker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by fperez on 6/23/16.
 */
public class GRPCClient implements TimeChecker {

    private static final Logger LOG = LoggerFactory.getLogger(GRPCClient.class);

    private final ManagedChannel channel;
    private final int nodeLevel;

    private CountDownLatch asyncResponsesReceived;

    public GRPCClient(MainClient.GRPCClientParams grpcCommand, CountDownLatch asyncResponsesReceived) {

        this.asyncResponsesReceived = asyncResponsesReceived;
        this.nodeLevel = grpcCommand.nodeLevel;
        this.channel = ManagedChannelBuilder.forAddress(grpcCommand.serverHostPort.getHostText(),
                                                        grpcCommand.serverHostPort.getPort())
                                            .usePlaintext(true)
                                            .build();
        LOG.info("GRPC channel created. Target host {}", grpcCommand.serverHostPort);

    }

    public void checkTime() {

        LOG.info("Checking time synchronously");

        TimeServiceGrpc.TimeServiceBlockingStub blockingStub = TimeServiceGrpc.newBlockingStub(channel);
        TimeRequest request = TimeRequest.newBuilder().setNodeLevel(nodeLevel).build();

        TimeResponse response;
        try {
            response = blockingStub.getTime(request);
        } catch (StatusRuntimeException e) {
            LOG.error("RPC failed: {}", e.getStatus());
            return;
        }
        String timeAsString = UnixTime.getFormattedTime(response.getSignature(), response.getTime());

        LOG.info("Time checked synchronously: " + timeAsString);

    }

    public void checkTimeAsync() throws Exception {

        LOG.info("Checking time asynchronously");

        TimeRequest request = TimeRequest.newBuilder().setNodeLevel(nodeLevel).build();
        AsyncTimeObserver asyncTimeObserver = new AsyncTimeObserver();

        TimeServiceGrpc.TimeServiceStub asyncStub = TimeServiceGrpc.newStub(channel);
        asyncStub.getTime(request, asyncTimeObserver);

    }

    @Override
    public void close() {

        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private class AsyncTimeObserver implements StreamObserver<TimeResponse> {

        @Override
        public void onNext(TimeResponse time) {

            LOG.info("Time checked asynchronously: " + UnixTime.getFormattedTime(time.getSignature(), time.getTime()));
            asyncResponsesReceived.countDown();

        }

        @Override
        public void onError(Throwable throwable) {

            LOG.error("Error occurred: {}", throwable);
            asyncResponsesReceived.countDown();

        }

        @Override
        public void onCompleted() {

            LOG.info("Request Completed");

        }

    }

}
