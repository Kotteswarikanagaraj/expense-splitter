package com.expensesplitter.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Generic envelope for every message pushed over /topic/group/{groupId}.
 * We deliberately keep 'data' as a light payload (the DTO that just changed)
 * rather than, say, the entire group state — the frontend treats this as a
 * "something changed, here's a hint of what" notification and decides for
 * itself whether to refetch. Keeping the socket payload small is a general
 * WebSocket best practice: sockets are for notification, REST stays the
 * source of truth for full data fetches.
 */
@Getter
@Builder
@AllArgsConstructor
public class GroupEventMessage<T> {
    private WsEventType type;
    private Long groupId;
    private T data;
    private LocalDateTime timestamp;
}
