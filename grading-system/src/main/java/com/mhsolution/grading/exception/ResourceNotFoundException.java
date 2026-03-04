package com.mhsolution.grading.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ResourceNotFoundException — thrown when an entity is not found.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) tells Spring MVC to return
 * HTTP 404 when this exception propagates up to the controller layer.
 *
 * For IDOR cases: when an Applicant tries to access another user's
 * assignment (which is filtered by uploader ID in the repository),
 * the repository returns Optional.empty(), and the service throws this
 * exception — resulting in a 404 response that reveals nothing.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
