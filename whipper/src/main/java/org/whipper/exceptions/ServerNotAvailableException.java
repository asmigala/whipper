package org.whipper.exceptions;

/**
 * Exception indicates that database server is not available.
 *
 * @author Juraj Duráni
 */
public class ServerNotAvailableException extends WhipperException {

    private static final long serialVersionUID = 1938750049163384722L;

    /**
     * Creates a new exception.
     *
     * @param message message
     * @param cause cause
     */
    public ServerNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception.
     *
     * @param message message
     */
    public ServerNotAvailableException(String message) {
        super(message);
    }
}
