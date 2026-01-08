package com.pgh.api_practice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "direct_chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_direct_chat_room_users",
                        columnNames = {"user1_id", "user2_id"}
                )
        }
)
@Getter
@NoArgsConstructor
public class DirectChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    public DirectChatRoom(Long userA, Long userB) {
        if (userA < userB) {
            this.user1Id = userA;
            this.user2Id = userB;
        } else {
            this.user1Id = userB;
            this.user2Id = userA;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTime = now;
        this.updatedTime = now;
        normalizeUsers();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedTime = LocalDateTime.now();
        normalizeUsers();
    }

    private void normalizeUsers() {
        if (user1Id != null && user2Id != null && user1Id > user2Id) {
            Long temp = user1Id;
            user1Id = user2Id;
            user2Id = temp;
        }
    }

    /** 현재 사용자가 상대방 id를 구할 때 사용 */
    public Long getOtherUserId(Long myUserId) {
        return myUserId.equals(user1Id) ? user2Id : user1Id;
    }
}