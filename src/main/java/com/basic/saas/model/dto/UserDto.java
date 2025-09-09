package com.basic.saas.model.dto;

public record UserDto(
        String name,
        String phoneNumber,
        String email,
        String gender,
        String dob,
        String password,
        String roles,
        String apiKey
) {}
