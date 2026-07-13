package ru.practicum.stat.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import stats.service.collector.ActionTypeProto;
import stats.service.collector.UserActionControllerGrpc;
import stats.service.collector.UserActionProto;
import stats.service.dashboard.RecommendationControllerGrpc;

import java.time.Instant;


@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
public class StatServerController {

    @GrpcClient("analyzer")
    private final RecommendationControllerGrpc.RecommendationControllerBlockingStub client;
    @GrpcClient("collector")
    private final UserActionControllerGrpc.UserActionControllerBlockingStub userActionControl;


    public void saveStat(Long userId, Long eventId, ActionTypeProto action) {
        UserActionProto userAction = UserActionProto.newBuilder()
                .setUserId(Math.toIntExact(userId))
                .setEventId(Math.toIntExact(eventId))
                .setActionType(action)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();
        userActionControl.collectUserAction(userAction);
    }


}
