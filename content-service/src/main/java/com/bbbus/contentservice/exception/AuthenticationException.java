package com.bbbus.contentservice.exception;

import lombok.Getter;

@Getter
public class AuthenticationException extends RuntimeException {
    private final Integer code;

    public AuthenticationException(String message) {
        super(message);
        this.code = 401;
    }

    public AuthenticationException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}