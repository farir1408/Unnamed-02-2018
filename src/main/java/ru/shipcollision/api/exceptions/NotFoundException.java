package ru.shipcollision.api.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String errorMessage) {
        super(errorMessage);
    }

    @Override
    protected String getDefaultErrorMessage() {
        return "Not Found";
    }

    @Override
    protected String getDefaultErrorCode() {
        return "not_found";
    }

    @Override
    protected HttpStatus getDefaultHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
