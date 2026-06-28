package com.example.userapi.exception;

/**
 * Thrown when an operation targets a user id that does not exist.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }
}
