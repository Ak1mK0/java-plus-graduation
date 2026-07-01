package ru.practicum.main.service.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.userDto.UserDto;
import ru.practicum.dto.userDto.UserShortDto;
import ru.practicum.faign.UserServiceFeign;
import ru.practicum.main.service.event.dto.EventFullDto;
import ru.practicum.main.service.event.dto.EventShortDto;
import ru.practicum.main.service.event.dto.NewEventDto;
import ru.practicum.main.service.event.dto.UpdateEventUserRequest;
import ru.practicum.main.service.event.mapper.EventMapper;
import ru.practicum.main.service.event.model.Event;
import ru.practicum.main.service.event.service.PrivateEventService;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.service.request.dto.ParticipationRequestDto;
import ru.practicum.main.service.request.mapper.RequestMapper;
import ru.practicum.main.service.request.service.RequestService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateEventController {

    private final PrivateEventService privateEventService;
    private final RequestService requestService;
    private final UserServiceFeign userServiceFeign;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @Valid @RequestBody NewEventDto dto) {
        log.info("POST /users/{}/events - создание события: {}", userId, dto);

        UserDto user = userServiceFeign.getUser(userId);
        Event event = privateEventService.createEvent(userId, dto);

        return EventMapper.toFullDto(event, 0L, 0L,
                new UserShortDto(user.getId(), user.getName()));
    }

    @GetMapping
    public List<EventShortDto> getUserEvents(@PathVariable @Positive Long userId,
                                             @RequestParam(defaultValue = "0") int from,
                                             @RequestParam(defaultValue = "10") int size) {
        log.info("GET /users/{}/events - получение событий пользователя", userId);

        UserDto user = userServiceFeign.getUser(userId);
        List<Event> events = privateEventService.getUserEvents(userId, from, size);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> confirmedMap = privateEventService.getConfirmedRequestsCounts(
                events.stream().map(Event::getId).toList()
        );

        Map<Long, Long> viewsMap = privateEventService.getViewsForEvents(events);

        UserShortDto initiator = new UserShortDto(user.getId(), user.getName());

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toShortDto(event, confirmedRequests, views, initiator);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{eventId}")
    public EventFullDto getUserEventById(@PathVariable @Positive Long userId,
                                         @PathVariable @Positive Long eventId) {
        log.info("GET /users/{}/events/{} - получение события", userId, eventId);

        UserDto user = userServiceFeign.getUser(userId);
        Event event = privateEventService.getUserEventById(userId, eventId);

        Long confirmedRequests = privateEventService.getConfirmedRequestsCount(eventId);
        Long views = privateEventService.getViewsForEvent(event);

        return EventMapper.toFullDto(event, confirmedRequests, views,
                new UserShortDto(user.getId(), user.getName()));
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(@PathVariable @Positive Long userId,
                                        @PathVariable @Positive Long eventId,
                                        @Valid @RequestBody UpdateEventUserRequest dto) {
        log.info("PATCH /users/{}/events/{} - обновление события: {}", userId, eventId, dto);

        UserDto user = userServiceFeign.getUser(userId);
        Event event = privateEventService.updateUserEvent(userId, eventId, dto);

        Long confirmedRequests = privateEventService.getConfirmedRequestsCount(eventId);
        Long views = privateEventService.getViewsForEvent(event);

        return EventMapper.toFullDto(event, confirmedRequests, views,
                new UserShortDto(user.getId(), user.getName()));
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
        log.info("PATCH /users/{}/events/{}/requests: {}", userId, eventId, updateRequest);
        return requestService.updateEventRequestsStatus(userId, eventId, updateRequest);
    }
}