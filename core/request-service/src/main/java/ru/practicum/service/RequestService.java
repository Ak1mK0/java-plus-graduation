package ru.practicum.service;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.eventDto.EventFullDto;
import ru.practicum.dto.requestDto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requestDto.EventRequestStatusUpdateResult;
import ru.practicum.dto.userDto.UserDto;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.dto.requestDto.RequestStatus;

import java.util.List;

public interface RequestService {
    List<ParticipationRequest> getUserRequests(Long userId);

    ParticipationRequest createRequest(Long userId, Long eventId, EventFullDto event, UserDto requester);

    ParticipationRequest cancelRequest(Long userId, Long requestId);

    List<ParticipationRequest> getEventRequests(Long userId, Long eventId, EventFullDto event);

    EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId,
                                                             EventRequestStatusUpdateRequest updateRequest,
                                                             EventFullDto event);

    boolean confirmUserRegisterOnEvent(Long userId, Long eventId, RequestStatus requestStatus);

    List<ParticipationRequest> getAllByEventIdInAndStatus(Long userId, List<Long> eventId, RequestStatus requestStatus);
}