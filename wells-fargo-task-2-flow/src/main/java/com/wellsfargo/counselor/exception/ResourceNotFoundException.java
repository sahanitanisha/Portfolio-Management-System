package com.wellsfargo.counselor.exception;

/**
 * Thrown when a requested aggregate cannot be found. Mapped to HTTP 404 by the global exception
 * handler, keeping "not found" out of service-layer control flow as an explicit, typed signal.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " " + id + " not found");
    }
}
