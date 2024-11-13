package com.androidapp.guardian.utils;

public class Alert {
    private String from;
    private String message;
    private String detectedWords;

    public Alert(){}

    public Alert(String from, String message, String detectedWords) {
        this.from = from;
        this.message = message;
        this.detectedWords = detectedWords;
    }

    public String getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

    public String getDetectedWords() {
        return detectedWords;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDetectedWords(String detectedWords) {
        this.detectedWords = detectedWords;
    }
}
