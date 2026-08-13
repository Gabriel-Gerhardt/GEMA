package com.gema.adapters.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Profile edit. Only the display name is editable here — changing a password or
 * an email is a credential change that needs its own flow (current-password
 * confirmation, and re-issuing the token), not a field on this payload.
 */
public record UserUpdateRequest(

        @Size(max = 255)
        String name
) {
}
