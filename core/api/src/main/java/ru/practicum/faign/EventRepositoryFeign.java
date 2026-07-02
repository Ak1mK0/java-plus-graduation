package ru.practicum.faign;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.eventDto.EventFullDto;


@FeignClient(name = "event-service")
public interface EventRepositoryFeign {

    @GetMapping("/events/{id}")
    EventFullDto getEventById(@PathVariable @Positive Long id, HttpServletRequest request);
}
