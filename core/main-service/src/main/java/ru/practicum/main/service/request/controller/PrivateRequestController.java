package ru.practicum.main.service.request.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.service.request.dto.ParticipationRequestDto;
import ru.practicum.main.service.request.mapper.RequestMapper;
import ru.practicum.main.service.request.model.ParticipationRequest;
import ru.practicum.main.service.request.service.RequestService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId) {
        log.info("GET /users/{}/requests", userId);

        return requestService.getUserRequests(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable @Positive Long userId,
                                                 @RequestParam @Positive Long eventId) {
        log.info("POST /users/{}/requests?eventId={}", userId, eventId);
        ParticipationRequest pr = requestService.createRequest(userId, eventId);
        return RequestMapper.toDto(pr);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        ParticipationRequest pr = requestService.cancelRequest(userId, requestId);
        return RequestMapper.toDto(pr);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return requestService.getEventRequests(userId, eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateEventRequestsStatus(@PathVariable @Positive Long userId,
                                                                    @PathVariable @Positive Long eventId,
                                                                    @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}/requests с телом: {}", userId, eventId, updateRequest);
        return requestService.updateEventRequestsStatus(userId, eventId, updateRequest);
    }
}