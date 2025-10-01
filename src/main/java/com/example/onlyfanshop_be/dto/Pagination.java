package com.example.onlyfanshop_be.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pagination {
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Long totalElements;
}
