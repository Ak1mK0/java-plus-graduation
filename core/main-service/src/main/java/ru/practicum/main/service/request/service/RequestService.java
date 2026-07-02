package ru.practicum.main.service.request.service;

import ru.practicum.dto.requestDto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requestDto.EventRequestStatusUpdateResult;
import ru.practicum.main.service.request.model.ParticipationRequest;
import ru.practicum.dto.requestDto.RequestStatus;

import java.util.List;

public interface RequestService {
    List<ParticipationRequest> getUserRequests(Long userId);

    ParticipationRequest createRequest(Long userId, Long eventId);

    ParticipationRequest cancelRequest(Long userId, Long requestId);

    List<ParticipationRequest> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);

    boolean confirmUserRegisterOnEvent(Long userId, Long eventId, RequestStatus requestStatus);
}