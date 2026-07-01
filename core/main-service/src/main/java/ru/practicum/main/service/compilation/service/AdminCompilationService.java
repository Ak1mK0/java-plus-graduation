package ru.practicum.main.service.compilation.service;

import ru.practicum.main.service.compilation.dto.NewCompilationDto;
import ru.practicum.main.service.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.service.compilation.model.Compilation;
import ru.practicum.main.service.event.model.Event;

import java.util.List;
import java.util.Map;

public interface AdminCompilationService {

    Compilation createCompilation(NewCompilationDto newCompilationDto);

    void deleteCompilation(Long compId);

    Compilation updateCompilation(Long compId, UpdateCompilationRequest request);

    Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds);

    Map<Long, Long> getViewsForEvents(List<Event> events);
}