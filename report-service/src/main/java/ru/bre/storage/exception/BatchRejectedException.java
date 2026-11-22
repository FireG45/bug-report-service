package ru.bre.storage.exception;

public class BatchRejectedException extends RuntimeException {
    public BatchRejectedException(String message) {
        super(message);
    }
}
