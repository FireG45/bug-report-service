package ru.bre.storage.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidEventsException extends RuntimeException {

    private final List<Integer> invalidEventIndices;

    public InvalidEventsException(List<Integer> invalidEventIndices) {
        super("Invalid event indices: " + invalidEventIndices);
        this.invalidEventIndices = invalidEventIndices;
    }
}
