package com.tpi.history.controller;

import com.tpi.history.dto.HistoryEventRequest;
import com.tpi.history.dto.HistoryEventResponse;
import com.tpi.history.service.HistoryService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping("/events")
    public HistoryEventResponse registerEvent(@Valid @RequestBody HistoryEventRequest request) {
        return historyService.registerEvent(request);
    }

    @GetMapping("/users/{userId}/history")
    public List<HistoryEventResponse> getUserHistory(@PathVariable String userId) {
        return historyService.getUserHistory(userId);
    }

    @GetMapping("/admin/history")
    public List<HistoryEventResponse> getAllHistory() {
        return historyService.getAllHistory();
    }
}

