package ru.practicum.main.service.request.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.service.event.model.Event;
import ru.practicum.dto.requestDto.ParticipationRequestDto;
import ru.practicum.main.service.request.model.ParticipationRequest;
import ru.practicum.dto.requestDto.RequestStatus;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) return null;

        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }

    public static ParticipationRequest toNewRequest(Event event, Long userId) {
        return ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .build();
    }
}
