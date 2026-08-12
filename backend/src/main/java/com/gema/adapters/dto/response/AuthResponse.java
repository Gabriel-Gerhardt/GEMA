package com.gema.adapters.dto.response;

/**
 * @param userId the created/authenticated user's id. Returned alongside the
 *               token because the client otherwise has no way to learn its own
 *               id, which {@code POST /api/qrcodes} requires.
 */
public record AuthResponse(
        String token,
        Long userId,
        String username,
        String name
) {
}
