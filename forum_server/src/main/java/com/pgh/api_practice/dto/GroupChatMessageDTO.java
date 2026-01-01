package com.pgh.api_practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupChatMessageDTO {
    private Long id;
    private String message;
    private String username;
    private String nickname;
    private String displayName;  // 채팅방별 별명
    private String profileImageUrl;
    
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    
    private LocalDateTime createdTime;
    private int readCount;
    
    private Long replyToMessageId;  // 답장한 메시지 ID
    private ReplyToMessageInfo replyToMessage;  // 답장한 메시지 정보
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ReplyToMessageInfo {
        private Long id;
        private String message;
        private String username;
        private String nickname;
        private String displayName;
        private String profileImageUrl;
    }
}
