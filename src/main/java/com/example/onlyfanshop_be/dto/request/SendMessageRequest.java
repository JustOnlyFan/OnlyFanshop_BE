package com.example.onlyfanshop_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String roomId;
    private String message;
    private String attachmentUrl;
    private String attachmentType;
    private String replyToMessageId;
}

