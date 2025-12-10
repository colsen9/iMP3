package com.imp3.Backend.chat;

import com.imp3.Backend.user.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="chat")

public class Chat {

    // ============================================================
    // Internal Table Data
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chatId;

    // ============================================================
    // Required Chat Details
    // ============================================================

    @Column(name = "sender", nullable = false)
    private Integer sender;

    @Column(name = "receiver", nullable = false)
    private Integer receiver;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "time", nullable = false)
    private LocalDateTime time = LocalDateTime.now();

    @Column(name = "unread", nullable = false)
    private Boolean unread;

}
