package ru.practicum.main.service.request.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.userDto.UserDto;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.faign.UserServiceFeign;
import ru.practicum.main.service.event.model.Event;
import ru.practicum.main.service.event.model.EventState;
import ru.practicum.main.service.event.repository.EventRepository;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.service.request.mapper.RequestMapper;
import ru.practicum.main.service.request.model.ParticipationRequest;
import ru.practicum.main.service.request.model.RequestStatus;
import ru.practicum.main.service.request.repository.RequestRepository;
import ru.practicum.main.service.request.service.RequestService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserServiceFeign userServiceFeign;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequest> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя с id: {}", userId);
        userServiceFeign.getUser(userId);
        return requestRepository.findAllByRequesterId(userId);
    }

    @Override
    @Transactional
    public ParticipationRequest createRequest(Long userId, Long eventId) {
        log.info("Создание заявки от пользователя {} на событие {}", userId, eventId);

        // Проверка существования пользователя
        UserDto requester = userServiceFeign.getUser(userId);

        // Проверка существования события
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Инициатор не может подать заявку на своё событие
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос " +
                    "на участие в своём событии");
        }

        // Событие должно быть опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // Проверка на существующую заявку
        requestRepository.findByEventIdAndRequesterId(eventId, userId)
                .ifPresent(r -> {
                    throw new ConflictException("Нельзя добавить повторный запрос на это событие");
                });

        // Проверка лимита участников (если лимит установлен и достигнут)
        if (event.getParticipantLimit() > 0) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников для события");
            }
        }

        // Создание заявки
        ParticipationRequest request = RequestMapper.toNewRequest(event, userId);

        // Если пре-модерация отключена, заявка сразу подтверждается
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        request = requestRepository.save(request);
        log.info("Заявка создана с id: {}", request.getId());
        return request;
    }

    @Override
    @Transactional
    public ParticipationRequest cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки {} пользователем {}", requestId, userId);

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId +
                        " не найден или не принадлежит пользователю"));

        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        log.info("Заявка {} отменена", requestId);
        return request;
    }

    @Override
    public List<ParticipationRequest> getEventRequests(Long userId, Long eventId) {
        log.info("Получение заявок на событие {} для пользователя {}", eventId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConditionsNotMetException("Пользователь не является инициатором события");
        }
        return requestRepository.findAllByEventId(eventId);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId,
                                                                    EventRequestStatusUpdateRequest updateRequest) {
        log.info("updateEventRequestsStatus: userId={}, eventId={}, requestIds={}", userId, eventId, updateRequest.getRequestIds());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        log.info("Event initiator id = {}", event.getInitiatorId());

        // Проверка прав инициатора
        if (!event.getInitiatorId().equals(userId)) {
            log.warn("Initiator mismatch: event initiator id = {}, userId = {}", event.getInitiatorId(), userId);
            throw new ConditionsNotMetException("Пользователь не является инициатором события");
        }

        List<Long> requestIds = updateRequest.getRequestIds();
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(requestIds);

        // Проверка принадлежности событию
        for (ParticipationRequest req : requests) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new ConditionsNotMetException("Запрос с id=" + req.getId() + " не относится к событию " + eventId);
            }
        }

        RequestStatus newStatus = updateRequest.getStatus();
        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (newStatus == RequestStatus.CONFIRMED) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            long limit = event.getParticipantLimit();

            for (ParticipationRequest req : requests) {
                if (req.getStatus() != RequestStatus.PENDING) {
                    throw new ConditionsNotMetException("Нельзя подтвердить запрос, который не в статусе PENDING");
                }
                if (limit == 0 || confirmedCount < limit) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(req);
                    confirmedCount++;
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(req);
                    throw new ConflictException("Лимит участников достигнут");
                }
            }
        } else if (newStatus == RequestStatus.REJECTED) {
            for (ParticipationRequest req : requests) {
                if (req.getStatus() == RequestStatus.CONFIRMED) {
                    throw new ConflictException("Нельзя отменить принятую заявку");
                }
                if (req.getStatus() != RequestStatus.REJECTED) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(req);
                } else {
                    rejected.add(req);
                }
            }
        } else {
            throw new IllegalArgumentException("Недопустимый статус: " + newStatus);
        }

        requestRepository.saveAll(requests);
        log.info("Статусы заявок обновлены");

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream().map(RequestMapper::toDto).collect(Collectors.toList()))
                .rejectedRequests(rejected.stream().map(RequestMapper::toDto).collect(Collectors.toList()))
                .build();
    }
}
