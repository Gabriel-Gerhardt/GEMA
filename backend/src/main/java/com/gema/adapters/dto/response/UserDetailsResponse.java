package com.gema.adapters.dto.response;

import com.gema.core.model.Role;

/**
 * The authenticated account.
 *
 * <p>The caller's plans are no longer embedded here: they are a paginated
 * collection of their own at {@code GET /api/qrcodes}. Profile only needs the
 * count ("Planos criados"), and inlining an unbounded list in the account
 * payload meant it grew without limit.
 */
public record UserDetailsResponse(
        Long id,
        String username,
        String name,
        Role role,
        long planCount
) {
}
