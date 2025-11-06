package ru.bre.storage.service.summary.exception;

public class LLMException extends RuntimeException {
    private final int status;

    public LLMException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
