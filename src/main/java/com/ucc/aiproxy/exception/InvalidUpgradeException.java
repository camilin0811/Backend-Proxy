package com.ucc.aiproxy.exception;

/** Thrown when a plan change is not an upgrade (e.g. PRO -> FREE). Mapped to HTTP 400. */
public class InvalidUpgradeException extends RuntimeException {

    public InvalidUpgradeException(String message) {
        super(message);
    }
}
