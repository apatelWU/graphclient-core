package com.skycstech.graphclient.core.exception;

public class GraphFeignException extends RuntimeException {

    public GraphFeignException(String message) {
        super(message);
    }

    public GraphFeignException(String message, Throwable cause) {
        super(message, cause);
    }

    public GraphFeignException(Throwable cause) {
        super(cause);
    }

    public GraphFeignException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
