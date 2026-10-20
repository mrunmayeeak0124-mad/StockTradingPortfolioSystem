package com.stockportfolio.exception;

/**
 * Thrown when a value given to a class is not valid
 * (for example an empty name or a negative price).
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}