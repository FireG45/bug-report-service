package ru.bre.kafka.message;

import java.util.Date;

public record SummaryMessage(String title, String text, Date date) {
}
