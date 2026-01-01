package com.pgh.api_practice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupPostDetailDTO {
    private Long id;
    private String title;
    private String body;
    private String username;
    private String nickname;
    private String Views;
    private LocalDateTime createDateTime;
    private LocalDateTime updateDateTime;
    private String profileImageUrl;
    private boolean isAuthor;
    private boolean canEdit;
    private boolean canDelete;
    private boolean isPublic;  // 모임 외부 노출 여부
    private long likeCount;  // 좋아요 수
    private boolean isLiked;  // 현재 사용자가 좋아요를 눌렀는지 여부
}
