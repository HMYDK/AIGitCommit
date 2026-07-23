package com.hmydk.aigit.compat;

/**
 * Signals that the current IDE changed a version-sensitive VCS API.
 */
public final class IdeVcsCompatibilityException extends IllegalStateException {

    public IdeVcsCompatibilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
