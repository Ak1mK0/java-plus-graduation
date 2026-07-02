package ru.practicum.faign;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.requestDto.RequestStatus;

@FeignClient(name = "request-service")
public interface RequestRepositoryFeign {

    @GetMapping("/users/{userId}/requests/{eventId}")
    public boolean confirmUserRegisterOnEvent(@PathVariable @Positive Long userId,
                                              @PathVariable @Positive Long eventId,
                                              @RequestParam RequestStatus requestStatus);

}
