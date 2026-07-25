package com.pcverse.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
public class PaginationResponse<T> {

    private int currentPage;
    private int size;
    private int totalPages;
    private long totalElements;
    @Builder.Default
    private List<T> data = Collections.emptyList();

}
