package com.murasame.security;

public class PermissionDeniedException extends RuntimeException  {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
