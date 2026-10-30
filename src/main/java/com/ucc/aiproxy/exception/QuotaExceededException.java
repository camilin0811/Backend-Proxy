package com.ucc.aiproxy.exception;

/** Thrown by the QuotaProxyService. Mapped to HTTP 402 Payment Required. */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
