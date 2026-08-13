package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration payload.
 *
 * <p>Note there is deliberately no {@code role} field: it used to be supplied
 * by the caller, which let anyone register themselves as {@code ADMIN}. Role is
 * now assigned server-side.
 */
public record UserSaveRequest(

        /*
         * The design's Login/Create Account screens use an email address here,
         * and the previous 20-character ceiling rejected ordinary ones
         * ("eduarda.souza@exemplo.com" is 25). 254 is the maximum length of an
         * email address per RFC 5321 and fits the VARCHAR(255) column.
         */
        @NotBlank
        @Size(min = 4, max = 254)
        String username,

        /*
         * Minimum raised from 6 to 8 to match the "pelo menos 8 caracteres"
         * hint the Create Account screen already shows. The old max of 20 was
         * an arbitrary ceiling that only weakened long passphrases; 72 is
         * bcrypt's own limit, beyond which input is silently truncated.
         */
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        /** Display name for the Profile screen. Optional. */
        @Size(max = 255)
        String name
) {
}
