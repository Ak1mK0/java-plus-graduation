package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;
import stats.service.dashboard.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationControllerGrpc.RecommendationControllerImplBase {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    @PostConstruct
    public void init() {
        log.info("RecommendationsController инициализирован и зарегистрирован как gRPC сервис");
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос на получение рекомендаций для пользователя ID: {}", request.getUserId());
        try {
            Long userId = (long) request.getUserId();
            int maxResults = request.getMaxResults();

            List<UserAction> userActions = userActionRepository
                    .findAllByUserIdOrderByTsDesc(userId);

            List<Long> recentInteractedEvents = userActions.stream()
                    .map(UserAction::getEventId)
                    .limit(maxResults)
                    .toList();

            if (recentInteractedEvents.isEmpty()) {
                log.info("У пользователя ID: {} нет взаимодействий с событиями", userId);
                responseObserver.onCompleted();
                return;
            }

            List<EventSimilarity> similarEvents = eventSimilarityRepository
                    .findByEvent1InOrEvent2InOrderBySimilarityDesc(recentInteractedEvents);

            Map<Long, Float> recomendedEvents = new LinkedHashMap<>();

            for (EventSimilarity event : similarEvents) {
                Long event1 = event.getEvent1();
                Long event2 = event.getEvent2();
                Float score = event.getSimilarity();

                boolean hasEvent1 = recentInteractedEvents.contains(event1);
                boolean hasEvent2 = recentInteractedEvents.contains(event2);

                if (hasEvent1 != hasEvent2) {
                    Long candidateEventId = hasEvent1 ? event2 : event1;
                    if (!recentInteractedEvents.contains(candidateEventId)) {
                        recomendedEvents.putIfAbsent(candidateEventId, score);
                    }
                }
            }

            List<RecommendedEventProto> result = new ArrayList<>();

            for (Map.Entry<Long, Float> entry : recomendedEvents.entrySet()) {
                Long candidateEventId = entry.getKey();

                List<EventSimilarity> nearEvent = eventSimilarityRepository
                        .findByEvent1OrEvent2OrderBySimilarityDesc(candidateEventId)
                        .stream()
                        .filter(sim -> {
                            Long neighborId = sim.getEvent1().equals(candidateEventId)
                                    ? sim.getEvent2()
                                    : sim.getEvent1();
                            return recentInteractedEvents.contains(neighborId);
                        })
                        .limit(20)
                        .toList();

                if (nearEvent.isEmpty()) {
                    continue;
                }

                double weightedSum = 0.0;
                double similaritySum = 0.0;

                for (EventSimilarity neighbor : nearEvent) {
                    Long neighborId = neighbor.getEvent1().equals(candidateEventId)
                            ? neighbor.getEvent2()
                            : neighbor.getEvent1();

                    double rating = userActions.stream()
                            .filter(ua -> ua.getEventId().equals(neighborId))
                            .mapToDouble(UserAction::getRating)
                            .findFirst()
                            .orElse(0.0);

                    weightedSum += neighbor.getSimilarity() * rating;
                    similaritySum += neighbor.getSimilarity();
                }

                double predictedScore = similaritySum > 0 ? weightedSum / similaritySum : 0.0;

                result.add(RecommendedEventProto.newBuilder()
                        .setEventId(Math.toIntExact(candidateEventId))
                        .setScore(predictedScore)
                        .build());
            }

            result.stream()
                    .sorted((p1, p2) -> Double.compare(p2.getScore(), p1.getScore()))
                    .limit(maxResults)
                    .forEach(responseObserver::onNext);

            log.info("Успешно отправлено {} рекомендаций для пользователя ID: {}",
                    Math.min(result.size(), maxResults), userId);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.info("Ошибка при формировании рекомендаций для пользователя ID: {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос на поиск похожих событий: userId={}, eventId={}",
                request.getUserId(), request.getEventId());
        try {
            Long userId = (long) request.getUserId();
            Long eventId = (long) request.getEventId();
            int maxEventNumber = request.getMaxResults();

            List<UserAction> userActions = userActionRepository.findAllByUserId(userId);
            List<Long> interactedEvents = userActions.stream()
                    .map(UserAction::getEventId)
                    .toList();

            List<EventSimilarity> similarEvents = eventSimilarityRepository
                    .findByEvent1OrEvent2OrderBySimilarityDesc(eventId);

            List<RecommendedEventProto> result = new ArrayList<>();
            for (EventSimilarity event : similarEvents) {
                Long similarEventId = event.getEvent1().equals(eventId)
                        ? event.getEvent2()
                        : event.getEvent1();

                if (!interactedEvents.contains(similarEventId)) {
                    result.add(RecommendedEventProto.newBuilder()
                            .setEventId(Math.toIntExact(similarEventId))
                            .setScore(event.getSimilarity())
                            .build());

                    if (result.size() >= maxEventNumber) {
                        break;
                    }
                }
            }

            log.info("Найдено {} похожих событий для пользователя ID: {}", result.size(), userId);
            result.forEach(responseObserver::onNext);
            log.info("Успешно отправлены похожие события для пользователя ID: {}", userId);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.info("Ошибка при поиске похожих событий для eventId: {}", request.getEventId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос на получение количества взаимодействий для {} событий",
                request.getEventIdList().size());
        try {
            List<Long> eventIds = request.getEventIdList().stream()
                    .map(Integer::longValue)
                    .toList();

            List<UserAction> usersActions = userActionRepository.findAllByEventIdIn(eventIds);

            Map<Integer, Float> eventScore = usersActions.stream()
                    .collect(Collectors.toMap(
                            userAction -> Math.toIntExact(userAction.getEventId()),
                            UserAction::getRating,
                            Float::sum
                    ));

            eventScore.forEach((eventId, score) -> {
                RecommendedEventProto response = RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(score)
                        .build();
                responseObserver.onNext(response);
            });

            log.info("Успешно отправлены оценки для {} событий", eventScore.size());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.info("Ошибка при расчете количества взаимодействий для событий", e);
            responseObserver.onError(e);
        }
    }
}