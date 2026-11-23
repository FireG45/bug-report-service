package ru.bre.storage.exception;

public class SteamTicketRejectedException extends RuntimeException {

    public SteamTicketRejectedException() {
        super("Steam rejected the ticket");
    }
}
