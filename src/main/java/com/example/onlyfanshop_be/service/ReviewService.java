package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ReviewDTO;
import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.CreateReviewRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.ReviewResponse;
import com.example.onlyfanshop_be.entity.Review;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.ReviewStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.ReviewRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ApiResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        // Verify product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        Pageable pageable = PageRequest.of(page, size);
        // Only show approved reviews
        Page<Review> reviewPage = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.approved, pageable);

        List<ReviewDTO> reviewDTOs = reviewPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Calculate average rating
        Double averageRating = reviewRepository.findAverageRatingByProductIdAndStatus(productId, ReviewStatus.approved);
        if (averageRating == null) {
            averageRating = 0.0;
        }

        // Calculate rating distribution
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            Long count = reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.approved, rating);
            ratingDistribution.put(rating, count != null ? count : 0L);
        }

        ReviewResponse response = ReviewResponse.builder()
                .reviews(reviewDTOs)
                .totalPages(reviewPage.getTotalPages())
                .totalElements(reviewPage.getTotalElements())
                .currentPage(page)
                .pageSize(size)
                .averageRating(averageRating)
                .ratingDistribution(ratingDistribution)
                .build();

        return ApiResponse.<ReviewResponse>builder()
                .statusCode(200)
                .message("Lấy danh sách đánh giá thành công")
                .data(response)
                .build();
    }

    @Override
    public ApiResponse<ReviewDTO> getUserReview(Long productId, Long userId) {
        Optional<Review> reviewOpt = reviewRepository.findByProductIdAndUserId(productId, userId);
        
        if (reviewOpt.isEmpty()) {
            return ApiResponse.<ReviewDTO>builder()
                    .statusCode(200)
                    .message("Người dùng chưa đánh giá sản phẩm này")
                    .data(null)
                    .build();
        }

        ReviewDTO reviewDTO = convertToDTO(reviewOpt.get());
        return ApiResponse.<ReviewDTO>builder()
                .statusCode(200)
                .message("Lấy đánh giá thành công")
                .data(reviewDTO)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<ReviewDTO> createReview(CreateReviewRequest request, Long userId) {
        // Validate input
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Rating phải từ 1 đến 5");
        }

        // Verify product exists
        if (!productRepository.existsById(request.getProductId().intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }

        // Check if user already reviewed this product
        Optional<Review> existingReview = reviewRepository.findByProductIdAndUserId(request.getProductId(), userId);
        if (existingReview.isPresent()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Bạn đã đánh giá sản phẩm này rồi");
        }

        // Convert images list to JSON
        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.getImages());
            } catch (JsonProcessingException e) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Lỗi xử lý hình ảnh");
            }
        }

        // Create review
        Review review = Review.builder()
                .productId(request.getProductId())
                .userId(userId)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .imagesJson(imagesJson)
                .status(ReviewStatus.pending) // New reviews are pending by default
                .build();

        review = reviewRepository.save(review);

        ReviewDTO reviewDTO = convertToDTO(review);
        return ApiResponse.<ReviewDTO>builder()
                .statusCode(201)
                .message("Tạo đánh giá thành công")
                .data(reviewDTO)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<ReviewDTO> updateReview(Long reviewId, CreateReviewRequest request, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Không tìm thấy đánh giá"));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền chỉnh sửa đánh giá này");
        }

        // Validate rating if provided
        if (request.getRating() != null) {
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Rating phải từ 1 đến 5");
            }
            review.setRating(request.getRating());
        }

        // Update fields
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            review.setContent(request.getContent());
        }
        if (request.getImages() != null) {
            try {
                String imagesJson = request.getImages().isEmpty() 
                    ? null 
                    : objectMapper.writeValueAsString(request.getImages());
                review.setImagesJson(imagesJson);
            } catch (JsonProcessingException e) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Lỗi xử lý hình ảnh");
            }
        }

        // Reset status to pending when updated
        review.setStatus(ReviewStatus.pending);
        review.setApprovedAt(null);

        review = reviewRepository.save(review);

        ReviewDTO reviewDTO = convertToDTO(review);
        return ApiResponse.<ReviewDTO>builder()
                .statusCode(200)
                .message("Cập nhật đánh giá thành công")
                .data(reviewDTO)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Không tìm thấy đánh giá"));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền xóa đánh giá này");
        }

        reviewRepository.delete(review);

        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Xóa đánh giá thành công")
                .build();
    }

    private ReviewDTO convertToDTO(Review review) {
        // Parse images JSON
        List<String> images = null;
        if (review.getImagesJson() != null && !review.getImagesJson().isEmpty()) {
            try {
                images = objectMapper.readValue(review.getImagesJson(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                images = List.of();
            }
        }

        // Convert user to DTO
        UserDTO userDTO = null;
        if (review.getUser() != null) {
            User user = review.getUser();
            userDTO = UserDTO.builder()
                    .userID(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullname())
                    .build();
        }

        return ReviewDTO.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .images(images)
                .status(review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .approvedAt(review.getApprovedAt())
                .user(userDTO)
                .build();
    }
}

