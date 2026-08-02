package com.pcverse.exception;

import lombok.Getter;

@Getter
public class UnknownJsonFieldException extends IllegalArgumentException {

    private final String fieldName;

    public UnknownJsonFieldException(String fieldName) {
        super("Unknown JSON field: " + fieldName);
        this.fieldName = fieldName;
    }

}
