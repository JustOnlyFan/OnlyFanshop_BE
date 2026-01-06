package com.example.onlyfanshop_be.dto.response;

import com.example.onlyfanshop_be.dto.ReviewDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {
    private List<ReviewDTO> reviews;
    private Integer totalPages;
    private Long totalElements;
    private Integer currentPage;
    private Integer pageSize;
    private Double averageRating;
    private Map<Integer, Long> ratingDistribution; // Key: rating (1-5), Value: count
}

