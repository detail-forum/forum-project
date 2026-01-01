package com.pgh.api_practice.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupChatMessageDTO {
    @Size(min = 1, message = "메시지는 1자 이상이어야 합니다.")
    private String message;
    
    private Long replyToMessageId;  // 답장한 메시지 ID
}
