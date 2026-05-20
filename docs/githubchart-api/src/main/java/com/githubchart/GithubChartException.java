package com.githubchart;

public class GithubChartException extends RuntimeException {
    public GithubChartException(String message) {
        super(message);
    }

    public GithubChartException(String message, Throwable cause) {
        super(message, cause);
    }
}
