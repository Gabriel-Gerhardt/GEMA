package com.gema.adapters.dto.response;

import com.gema.core.model.Role;

import java.util.List;

public record UserDetailsResponse(
        Long id,
        String username,
        String name,
        Role role,
        List<UserQrcodeResponse> qrcodes
) {
}
