package com.tpi.history;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class HistoryService {

    private final HistoryEventRepository historyEventRepository;
    private final UserOperationViewRepository userOperationViewRepository;

    public HistoryService(HistoryEventRepository historyEventRepository,
                          UserOperationViewRepository userOperationViewRepository) {
        this.historyEventRepository = historyEventRepository;
        this.userOperationViewRepository = userOperationViewRepository;
    }

    @Transactional
    public HistoryEventResponse registerEvent(HistoryEventRequest request) {
        var event = new HistoryEvent();
        event.setEventId(request.eventId() != null ? request.eventId() : UUID.randomUUID());
        event.setEventType(request.eventType());
        event.setUserId(request.userId());
        event.setOrderId(request.orderId());
        event.setCorrelationId(request.correlationId());
        event.setCausationId(request.causationId());
        event.setPayloadJson(request.payloadJson());
        var saved = historyEventRepository.save(event);
        persistReadModel(saved);
        return toResponse(saved);
    }

    public List<HistoryEventResponse> getUserHistory(String userId) {
        return historyEventRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<HistoryEventResponse> getAllHistory() {
        return historyEventRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void persistReadModel(HistoryEvent saved) {
        if (saved.getUserId() == null || saved.getUserId().isBlank()) {
            return;
        }
        var item = new UserOperationView();
        item.setUserId(saved.getUserId());
        item.setOperationType(saved.getEventType());
        item.setSymbol((String) saved.getPayloadJson().get("symbol"));
        item.setAmountArs(extractAmount(saved.getPayloadJson().get("amountArs")));
        userOperationViewRepository.save(item);
    }

    private BigDecimal extractAmount(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private HistoryEventResponse toResponse(HistoryEvent event) {
        return new HistoryEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getUserId(),
                event.getOrderId(),
                event.getCorrelationId(),
                event.getCausationId(),
                event.getPayloadJson(),
                event.getOccurredAt()
        );
    }
}

