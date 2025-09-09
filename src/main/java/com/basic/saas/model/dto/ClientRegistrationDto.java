package com.basic.saas.model.dto;

public record ClientRegistrationDto(
        String name, String email,String phoneNumber, String password, boolean active
) {}
