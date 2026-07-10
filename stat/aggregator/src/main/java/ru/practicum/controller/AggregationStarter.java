package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.client.ClientConfiguration;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final ClientConfiguration client;
    private final Map<Integer, Map<Integer, Double>> userActionMatrix = new HashMap<>();
    private final Map<Integer, Map<Integer, Double>> eventSimilarityMatrix = new HashMap<>();

    public void start() {
        try {
            client.getConsumer().subscribe(List.of("stats.user-actions.v1"));
            while (true) {
                ConsumerRecords<String, UserActionAvro> records =
                        client.getConsumer().poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    UserActionAvro data = record.value();
                    log.info("------------------------------");
                    log.info("Получены данные: {}", data);

                    Double oldValue = userActionMatrix.getOrDefault(data.getEventId(), new HashMap<>())
                            .getOrDefault(data.getUserId(), 0.0);
                    log.info("Старое значение: {}", oldValue);
                    Double newValue = computeWeightActionType(data.getActionType());
                    log.info("Новое значение: {}", newValue);
                    if (oldValue < newValue) {
                        log.info("Требуется обновление в матрице действий пользователей");
                        putUserActionInMatrix(data);

                        Map<Integer, Double> eventA = userActionMatrix.get(data.getEventId());
                        for (Integer eventBKey : userActionMatrix.keySet()) {
                            if (data.getEventId() == eventBKey) {
                                continue;
                            }
                            Map<Integer, Double> eventB = userActionMatrix.get(eventBKey);
                            double weightEventBForUpdatedEventA = eventB.getOrDefault(data.getUserId(), 0.0);
                            int first = Math.min(data.getEventId(), eventBKey);
                            int second = Math.max(data.getEventId(), eventBKey);

                            double similarity = similarityCount(eventA, eventB);
                            EventSimilarityAvro eventSimilarityAvro = createSimilarity(first, second, similarity);
                            log.info("Значение схожести для ивента {} с ивентом {} составляет: {}", first, second, similarity);
                            client.getProducer().send(new ProducerRecord<>("stats.events-similarity.v1",
                                    eventSimilarityAvro));


                        }
                    } else {
                        log.info("Обновление в матрице действий пользователей не требуется");
                    }
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                client.getProducer().flush();
                client.getConsumer().commitSync();
            } finally {
                log.info("Закрываем консьюмер и продюсер");
                client.stop();
            }
        }
    }

    private void putUserActionInMatrix(UserActionAvro data) {
        userActionMatrix.computeIfAbsent(data.getEventId(), k -> new HashMap<>())
                .put(data.getUserId(), computeWeightActionType(data.getActionType()));
        log.info("Новая матрица действий пользователя: {}", userActionMatrix);
    }

    private double computeWeightActionType(ActionTypeAvro data) {
        return switch (data) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1;
        };
    }

    private double similarityCount(Map<Integer, Double> eventA, Map<Integer, Double> eventB) {
        double numerator = 0.0;
        double denominatorPartA = denominatorCount(eventA);
        log.info("Значение первой части знаменателя: {}", denominatorPartA);
        double denominatorPartB = denominatorCount(eventB);
        log.info("Значение второй части знаменателя: {}", denominatorPartB);
        for (Integer eventAKeys : eventA.keySet()) {
            if (eventB.containsKey(eventAKeys)) {
                numerator = numerator + Math.min(
                        eventA.get(eventAKeys),
                        eventB.get(eventAKeys));
            }
        }
        log.info("Значение числителя: {}", numerator);
        return numerator / (denominatorPartA * denominatorPartB);
    }

    private double denominatorCount(Map<Integer, Double> usersActionsMap) {
        double denominator = 0;
        for (double value : usersActionsMap.values()) {
            denominator = denominator + Math.pow(value, 2);
        }
        return Math.sqrt(denominator);
    }

    private EventSimilarityAvro createSimilarity(int first, int second, double similarity) {
        eventSimilarityMatrix.computeIfAbsent(first, k -> new HashMap<>())
                .put(second, similarity);
        log.info("Новая матрица подобия: {}", eventSimilarityMatrix);
        return EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(similarity)
                .setTimestamp(Instant.now())
                .build();
    }

    private boolean chekExistingNote(int first, int second) {
        if (eventSimilarityMatrix.containsKey(first)) {
            return eventSimilarityMatrix.get(first).containsKey(second);
        } else {
            return false;
        }
    }

}
