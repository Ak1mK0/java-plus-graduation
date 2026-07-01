package ru.practicum.main.service.request.service;

import ru.practicum.main.service.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.service.request.dto.ParticipationRequestDto;
import ru.practicum.main.service.request.model.ParticipationRequest;

import java.util.List;

public interface RequestService {
    List<ParticipationRequest> getUserRequests(Long userId);

    ParticipationRequest createRequest(Long userId, Long eventId);

    ParticipationRequest cancelRequest(Long userId, Long requestId);

    List<ParticipationRequest> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);
}