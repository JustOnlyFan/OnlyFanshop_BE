package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.request.CreateReviewRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.ReviewResponse;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.IReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private IReviewService reviewService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/product/{productId}")
    public ApiResponse<ReviewResponse> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reviewService.getProductReviews(productId, page, size);
    }

    @GetMapping("/product/{productId}/user")
    public ApiResponse<com.example.onlyfanshop_be.dto.ReviewDTO> getUserReview(
            @PathVariable Long productId,
            HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        return reviewService.getUserReview(productId, userId);
    }

    @PostMapping
    public ApiResponse<com.example.onlyfanshop_be.dto.ReviewDTO> createReview(
            @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        return reviewService.createReview(request, userId);
    }

    @PutMapping("/{reviewId}")
    public ApiResponse<com.example.onlyfanshop_be.dto.ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        return reviewService.updateReview(reviewId, request, userId);
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        return reviewService.deleteReview(reviewId, userId);
    }
}

