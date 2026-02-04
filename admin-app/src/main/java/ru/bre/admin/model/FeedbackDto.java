package ru.bre.admin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeedbackDto {
    @JsonProperty("title")
    private String title;

    @JsonProperty("text")
    private String text;

    public FeedbackDto() {
    }

    public FeedbackDto(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
