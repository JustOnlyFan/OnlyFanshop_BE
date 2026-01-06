package com.example.onlyfanshop_be.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateReviewRequest {
    private Long productId;
    private Integer rating; // 1-5
    private String title;
    private String content;
    private List<String> images;
}

