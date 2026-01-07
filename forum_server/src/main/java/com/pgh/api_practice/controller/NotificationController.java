package com.pgh.api_practice.controller;

import com.pgh.api_practice.dto.ApiResponse;
import com.pgh.api_practice.dto.NotificationDTO;
import com.pgh.api_practice.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@AllArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getNotifications(Pageable pageable) {
        Page<NotificationDTO> notifications = notificationService.getNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.ok(notifications, "알림 목록 조회 성공"));
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.ok(count, "읽지 않은 알림 개수 조회 성공"));
    }

    /**
     * 모든 알림을 읽음 처리
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.ok(null, "모든 알림을 읽음 처리했습니다."));
    }

    /**
     * 특정 알림을 읽음 처리
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "알림을 읽음 처리했습니다."));
    }
}
