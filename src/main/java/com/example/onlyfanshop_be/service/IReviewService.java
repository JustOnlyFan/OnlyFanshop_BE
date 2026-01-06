package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ReviewDTO;
import com.example.onlyfanshop_be.dto.request.CreateReviewRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.ReviewResponse;

public interface IReviewService {
    ApiResponse<ReviewResponse> getProductReviews(Long productId, int page, int size);
    
    ApiResponse<ReviewDTO> getUserReview(Long productId, Long userId);
    
    ApiResponse<ReviewDTO> createReview(CreateReviewRequest request, Long userId);
    
    ApiResponse<ReviewDTO> updateReview(Long reviewId, CreateReviewRequest request, Long userId);
    
    ApiResponse<Void> deleteReview(Long reviewId, Long userId);
}

