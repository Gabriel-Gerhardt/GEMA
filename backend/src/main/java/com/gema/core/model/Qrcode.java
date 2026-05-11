package com.gema.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Qrcode {
    private Long id;
    private String publicId;
    private String title;
    private boolean isActive;
    private String content;
    private User user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
