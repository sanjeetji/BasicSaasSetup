package com.basic.saas.model.dto;

public record UserLoginDto(
        String email,
        String password,
        String apiKey
) {}
