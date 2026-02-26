package ru.bre.admin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportDto {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("text")
    private String text;

    @JsonProperty("imageFile")
    private String imageFile;

    @JsonProperty("logFile")
    private String logFile;

    public ReportDto() {
    }

    public ReportDto(String title, String text, String imageFile, String logFile) {
        this.title = title;
        this.text = text;
        this.imageFile = imageFile;
        this.logFile = logFile;
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

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
