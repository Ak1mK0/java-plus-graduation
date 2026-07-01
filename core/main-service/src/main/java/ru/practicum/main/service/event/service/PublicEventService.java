package ru.practicum.main.service.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.userDto.UserShortDto;
import ru.practicum.main.service.event.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PublicEventService {

    List<Event> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                Boolean onlyAvailable, String sort, int from, int size,
                                HttpServletRequest request);

    Event getPublicEventById(Long eventId, HttpServletRequest request);

    Long getConfirmedRequestsCount(Long eventId);

    Long getViewsForEvent(Event event);

    Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds);

    Map<Long, Long> getViewsForEvents(List<Event> events);

    UserShortDto getEventInitiator(Event event);

    Map<Long, UserShortDto> getEventInitiators(List<Event> events);
}