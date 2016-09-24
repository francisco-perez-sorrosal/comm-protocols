package com.fps.rpc.grpc;

import akka.actor.ActorRef;
import com.codeg33ks.processing.TimeRequest;
import com.codeg33ks.processing.TimeResponse;
import com.codeg33ks.processing.TimeServiceGrpc;
import com.fps.rpc.FrontServer;
import com.fps.rpc.TimeProtocol;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Created by fperez on 6/23/16.
 */
public class TimerService extends TimeServiceGrpc.TimeServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(FrontServer.class);

    private final ActorRef requestForwarderActor;

    @Inject
    public TimerService(ActorRef requestForwarderActor) {

        this.requestForwarderActor = requestForwarderActor;

    }

    @Override
    public void getTime(TimeRequest request, StreamObserver<TimeResponse> responseObserver) {

        requestForwarderActor.tell(new TimeProtocol.GRPCTimeProtocol(TimeProtocol.Command.GET_TIME, request.getNodeLevel(), responseObserver), ActorRef.noSender());

    }

}
