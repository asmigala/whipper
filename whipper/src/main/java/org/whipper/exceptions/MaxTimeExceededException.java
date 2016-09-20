package org.whipper.exceptions;

/**
 * Exception indicates that maximum time for scenario has been reached.
 *
 * @author Juraj Duráni
 */
public class MaxTimeExceededException extends WhipperException {

    private static final long serialVersionUID = 68460808307865098L;

    /**
     * Creates a new exception.
     *
     * @param message message
     * @param cause cause
     */
    public MaxTimeExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception.
     *
     * @param message message
     */
    public MaxTimeExceededException(String message) {
        super(message);
    }
}
