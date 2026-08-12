package com.gema.adapters.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating a plan.
 *
 * <p>There is deliberately no {@code userId} field: ownership used to be picked
 * by the caller, which let anyone create plans in someone else's name. The owner
 * is now the authenticated subject.
 */
public record QrcodeSaveRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        /*
         * Optional. This field predates the sections model; a plan's content now
         * lives in its sections, so requiring it here forced clients to invent a
         * value just to satisfy validation.
         */
        @Size(max = 20000)
        String content,

        /** The person the plan is about, if not the account holder. Optional. */
        @Size(max = 255)
        String ownerName,

        @Size(max = 255)
        String emergencyContactName,

        @Size(max = 40)
        String emergencyContactPhone,

        /*
         * Sections may be supplied here so a plan and its content are created in
         * one transaction. Creating them in two calls (POST the plan, then PUT
         * the sections) left an orphaned, empty plan behind whenever the second
         * call failed — and an empty plan is a QR code that helps nobody.
         * Optional: omit to create a plan and fill it in later.
         */
        @Valid
        List<SectionSaveRequest> sections
) {
}
