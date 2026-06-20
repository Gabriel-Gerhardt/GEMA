package com.gema.adapters.dto.response;

import com.gema.core.model.Role;

import java.util.List;

public record UserDetailsResponse(
        String username,
        Role role,
        List<UserQrcodeResponse> qrcodes
) {
}
