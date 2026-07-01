package ru.practicum.main.service.event.service;

import ru.practicum.main.service.event.dto.NewEventDto;
import ru.practicum.main.service.event.dto.UpdateEventUserRequest;
import ru.practicum.main.service.event.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PrivateEventService {

    Event createEvent(Long userId, NewEventDto dto);

    List<Event> getUserEvents(Long userId, int from, int size);

    Event getUserEventById(Long userId, Long eventId);

    Event updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto);

    Long getConfirmedRequestsCount(Long eventId);

    Long getViewsForEvent(Event event);

    Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds);

    Map<Long, Long> getViewsForEvents(List<Event> events);
}