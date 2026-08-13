package com.gema.core.port;

import java.time.Duration;

/**
 * Delivers a password reset link.
 *
 * <p>A port rather than a direct dependency on a mail library: sending email is
 * an integration with something outside this system, and which provider sits
 * behind it is a deployment decision, not a business rule.
 */
public interface PasswordResetMailer {

    void sendResetLink(String recipient, String resetUrl, Duration validFor);
}
