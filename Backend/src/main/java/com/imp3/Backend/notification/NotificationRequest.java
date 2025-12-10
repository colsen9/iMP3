package com.imp3.Backend.notification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {
    private Integer recipientId;
    private String message;
    private String type;
}
