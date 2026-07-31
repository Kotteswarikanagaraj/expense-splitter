package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps any paginated result. We map Spring Data's Page<Entity> into this
 * instead of returning Page<ExpenseResponse> directly from the controller —
 * Spring's Page serializes with a lot of internal Pageable/Sort metadata we
 * don't want to expose as public API surface. This DTO exposes only what a
 * frontend actually needs to render pagination controls.
 */
@Getter
@Builder
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PageResponse<T> from(Page<?> sourcePage, List<T> mappedContent) {
        return PageResponse.<T>builder()
                .content(mappedContent)
                .page(sourcePage.getNumber())
                .size(sourcePage.getSize())
                .totalElements(sourcePage.getTotalElements())
                .totalPages(sourcePage.getTotalPages())
                .last(sourcePage.isLast())
                .build();
    }
}
