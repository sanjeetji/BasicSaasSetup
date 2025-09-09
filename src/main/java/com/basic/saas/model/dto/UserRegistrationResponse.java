package com.basic.saas.model.dto;

public record UserRegistrationResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String gender,
        String dob,
        String createdAt
) {}
