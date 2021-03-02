package com.qrapids.backlog_jira.data;

public class ErrorResponse {
    private String error_message;

    public ErrorResponse(String message) {
        this.error_message = message;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }
}
