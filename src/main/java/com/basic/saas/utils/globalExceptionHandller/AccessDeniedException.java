package com.basic.saas.utils.globalExceptionHandller;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
