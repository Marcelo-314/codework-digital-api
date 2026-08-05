package com.codeworkdigital.api.shared.web;

public class RequestBodyTooLargeException extends RuntimeException {

    public RequestBodyTooLargeException() {
        super("Request body exceeds the allowed size");
    }
}
