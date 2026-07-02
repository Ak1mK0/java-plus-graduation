package ru.practicum.main.service.compilation.controller;

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
import ru.practicum.dto.compilationDto.CompilationDto;
import ru.practicum.dto.compilationDto.NewCompilationDto;
import ru.practicum.dto.compilationDto.UpdateCompilationRequest;
import ru.practicum.main.service.compilation.mapper.CompilationMapper;
import ru.practicum.main.service.compilation.model.Compilation;
import ru.practicum.main.service.compilation.service.AdminCompilationService;
import ru.practicum.dto.eventDto.EventShortDto;
import ru.practicum.main.service.event.mapper.EventMapper;
import ru.practicum.main.service.event.model.Event;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCompilationController {

    private final AdminCompilationService adminCompilationService;
    private final UserServiceFeign userServiceFeign;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        log.info("POST /admin/compilations - создание подборки");

        Compilation compilation = adminCompilationService.createCompilation(newCompilationDto);
        List<EventShortDto> eventShortDtos = buildEventShortDtos(compilation.getEvents());

        return CompilationMapper.toDto(compilation, eventShortDtos);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable @Positive Long compId) {
        log.info("DELETE /admin/compilations/{}", compId);
        adminCompilationService.deleteCompilation(compId);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@PathVariable @Positive Long compId,
                                            @Valid @RequestBody UpdateCompilationRequest request) {
        log.info("PATCH /admin/compilations/{} - обновление подборки", compId);

        Compilation compilation = adminCompilationService.updateCompilation(compId, request);
        List<EventShortDto> eventShortDtos = buildEventShortDtos(compilation.getEvents());

        return CompilationMapper.toDto(compilation, eventShortDtos);
    }

    private List<EventShortDto> buildEventShortDtos(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> confirmedMap = adminCompilationService.getConfirmedRequestsCounts(
                events.stream().map(Event::getId).toList()
        );

        Map<Long, Long> viewsMap = adminCompilationService.getViewsForEvents(events);

        Map<Long, UserShortDto> initiatorMap = getEventInitiators(events);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    UserShortDto initiator = initiatorMap.get(event.getInitiatorId());

                    return EventMapper.toShortDto(event, confirmedRequests, views, initiator);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, UserShortDto> getEventInitiators(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        try {
            List<UserDto> users = userServiceFeign.getAllUsersById(userIds);
            return users.stream()
                    .collect(Collectors.toMap(
                            UserDto::getId,
                            user -> new UserShortDto(user.getId(), user.getName())
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить данные пользователей для событий: {}", e.getMessage());
            return Map.of();
        }
    }
}