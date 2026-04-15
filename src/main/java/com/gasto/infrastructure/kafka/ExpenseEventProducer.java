package com.gasto.infrastructure.kafka;

import com.gasto.domain.expense.Expense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseEventProducer {

    private final KafkaTemplate<String, ExpenseEvent> kafkaTemplate;

    @Value("${app.kafka.topics.expense-events}")
    private String topic;

    @Async
    public void publishExpenseCreated(Expense expense) {
        publish(buildEvent(expense, ExpenseEvent.EVENT_CREATED));
    }

    @Async
    public void publishExpenseUpdated(Expense expense) {
        publish(buildEvent(expense, ExpenseEvent.EVENT_UPDATED));
    }

    private void publish(ExpenseEvent event) {
        kafkaTemplate.send(topic, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish expense event [{}] for expenseId={}: {}",
                                event.eventType(), event.expenseId(), ex.getMessage());
                    } else {
                        log.debug("Published expense event [{}] expenseId={} offset={}",
                                event.eventType(), event.expenseId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private ExpenseEvent buildEvent(Expense expense, String eventType) {
        return new ExpenseEvent(
                eventType,
                expense.getId(),
                expense.getUser().getId(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getAmount(),
                expense.getExpenseDate(),
                OffsetDateTime.now()
        );
    }
}
