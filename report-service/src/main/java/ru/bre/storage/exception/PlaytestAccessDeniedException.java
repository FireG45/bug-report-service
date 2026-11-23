package ru.bre.storage.exception;

public class PlaytestAccessDeniedException extends RuntimeException {

    public PlaytestAccessDeniedException() {
        super("Account has no active playtest license");
    }
}
