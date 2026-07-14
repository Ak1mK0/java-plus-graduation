package ru.practicum.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.statServerDto.RecommendedEventDto;
import ru.practicum.faign.StatServerFaign;
import stats.service.collector.ActionTypeProto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class StatServerFaignFallbackFactory implements FallbackFactory<StatServerFaign> {

    @Override
    public StatServerFaign create(Throwable cause) {

        return new StatServerFaign() {
            @Override
            public void saveStat(Long userId, Long eventId, ActionTypeProto action) {
                System.err.println("Fallback: saveStat - сервис статистики недоступен. " +
                        "userId: " + userId + ", eventId: " + eventId + ", action: " + action);
            }

            @Override
            public List<RecommendedEventDto> getRecommendationsForUser(long userId, int maxResults) {
                int count = Math.min(maxResults, 5);
                RecommendedEventDto[] events = new RecommendedEventDto[count];

                for (int i = 0; i < count; i++) {
                    events[i] = createFallbackEvent(
                            (long) (1000 + i),
                            "Рекомендация #" + (i + 1) + " для пользователя " + userId,
                            0.9 - (i * 0.1)
                    );
                }

                return Arrays.asList(events);
            }

            @Override
            public List<RecommendedEventDto> getSimilarEvents(long eventId, long userId, int maxResults) {
                int count = Math.min(maxResults, 3);
                RecommendedEventDto[] events = new RecommendedEventDto[count];

                for (int i = 0; i < count; i++) {
                    events[i] = createFallbackEvent(
                            eventId + 100 + i,
                            "Похожее на событие " + eventId + " #" + (i + 1),
                            0.85 - (i * 0.15)
                    );
                }

                return Arrays.asList(events);
            }

            @Override
            public List<RecommendedEventDto> getInteractionsCount(List<Long> eventIds) {
                if (eventIds == null || eventIds.isEmpty()) {
                    return Collections.emptyList();
                }

                return eventIds.stream()
                        .map(id -> {
                            RecommendedEventDto dto = new RecommendedEventDto();
                            dto.setEventId(Math.toIntExact(id));
                            dto.setScore(0.0);
                            return dto;
                        })
                        .toList();
            }

            private RecommendedEventDto createFallbackEvent(Long eventId, String name, Double score) {
                RecommendedEventDto dto = new RecommendedEventDto();
                dto.setEventId(Math.toIntExact(eventId));
                dto.setScore(score);
                return dto;
            }
        };
    }
}