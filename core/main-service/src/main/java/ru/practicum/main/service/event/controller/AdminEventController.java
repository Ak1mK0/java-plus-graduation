package ru.practicum.main.service.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.userDto.UserDto;
import ru.practicum.dto.userDto.UserShortDto;
import ru.practicum.faign.UserServiceFeign;
import ru.practicum.dto.eventDto.EventFullDto;
import ru.practicum.dto.eventDto.UpdateEventAdminRequest;
import ru.practicum.main.service.event.mapper.EventMapper;
import ru.practicum.main.service.event.model.Event;
import ru.practicum.dto.eventDto.EventState;
import ru.practicum.main.service.event.service.AdminEventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminEventController {

    private final AdminEventService adminEventService;
    private final UserServiceFeign userServiceFeign;

    @GetMapping
    public List<EventFullDto> getEvents(@RequestParam(required = false) List<Long> users,
                                        @RequestParam(required = false) List<EventState> states,
                                        @RequestParam(required = false) List<Long> categories,
                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        log.info("GET /admin/events - админский поиск событий");

        List<Event> events = adminEventService.getAdminEvents(users, states, categories, rangeStart, rangeEnd, from, size);

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .toList();

        List<UserDto> userDtos = userServiceFeign.getAllUsersById(userIds);

        Map<Long, UserDto> userMap = userDtos.stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));

        Map<Long, UserShortDto> userShortMap = userDtos.stream()
                .collect(Collectors.toMap(
                        UserDto::getId,
                        user -> new UserShortDto(user.getId(), user.getName())
                ));

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = adminEventService.getConfirmedRequestsCount(event.getId());
                    Long views = adminEventService.getViewsForEvent(event);
                    UserShortDto initiator = userShortMap.get(event.getInitiatorId());
                    return EventMapper.toFullDto(event, confirmedRequests, views, initiator);
                })
                .collect(Collectors.toList());
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventAdminRequest dto) {
        log.info("PATCH /admin/events/{} - админское обновление события: {}", eventId, dto);

        Event updatedEvent = adminEventService.updateAdminEvent(eventId, dto);
        UserDto user = userServiceFeign.getUser(updatedEvent.getInitiatorId());
        UserShortDto userShortDto = new UserShortDto(user.getId(), user.getName());
        Long confirmedRequests = adminEventService.getConfirmedRequestsCount(eventId);
        Long views = adminEventService.getViewsForEvent(updatedEvent);

        return EventMapper.toFullDto(updatedEvent, confirmedRequests, views, userShortDto);
    }
}