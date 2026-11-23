package ru.bre.storage.exception;

public class SteamUnavailableException extends RuntimeException {

    public SteamUnavailableException() {
        super("Steam Web API is unavailable");
    }
}
