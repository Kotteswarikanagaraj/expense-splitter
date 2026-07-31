package com.expensesplitter.service;

import com.expensesplitter.dto.ws.GroupEventMessage;
import com.expensesplitter.dto.ws.WsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Single place responsible for pushing messages to WebSocket subscribers.
 * Other services (ExpenseService, SettlementService) call this after they've
 * already committed a change — they don't touch SimpMessagingTemplate directly.
 * That keeps "how do we notify clients" as one swappable concern (e.g. if we
 * later add push notifications or email digests, this is the one class that grows).
 */
@Service
@RequiredArgsConstructor
public class GroupNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(Long groupId, WsEventType type, Object data) {
        GroupEventMessage<Object> message = GroupEventMessage.builder()
                .type(type)
                .groupId(groupId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        // Every client subscribed to this exact topic string receives the message.
        // Destination convention: /topic/group/{groupId}
        messagingTemplate.convertAndSend("/topic/group/" + groupId, message);
    }
}
