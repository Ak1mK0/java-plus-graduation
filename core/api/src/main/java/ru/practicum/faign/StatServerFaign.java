package ru.practicum.faign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.statServerDto.RecommendedEventDto;
import stats.service.collector.ActionTypeProto;
import stats.service.dashboard.RecommendedEventProto;

import java.util.List;

@FeignClient(name = "stats-server", configuration = FeignConfig.class)
public interface StatServerFaign {

    @PostMapping("/api/save")
    void saveStat(@RequestParam Long userId,
                  @RequestParam Long eventId,
                  @RequestParam ActionTypeProto action);

    @GetMapping("/api/recommendations")
    List<RecommendedEventDto> getRecommendationsForUser(@RequestParam long userId,
                                                        @RequestParam int maxResults);

    @GetMapping("/api/similar")
    List<RecommendedEventDto> getSimilarEvents(@RequestParam long eventId,
                                                 @RequestParam long userId,
                                                 @RequestParam int maxResults);

    @PostMapping("/api/interactions")
    List<RecommendedEventDto> getInteractionsCount(@RequestBody List<Long> eventIds);
}
