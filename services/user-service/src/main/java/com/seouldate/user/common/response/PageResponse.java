package com.seouldate.user.common.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 공통 응답.
 *
 * <p>Spring Data {@link Page} 를 그대로 직렬화하면 불필요한 필드가 많이 포함되므로,
 * 클라이언트 계약에 필요한 필드만 노출한다.
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = page + 1 < totalPages;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
