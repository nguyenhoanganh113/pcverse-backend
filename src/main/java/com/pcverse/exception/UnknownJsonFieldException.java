package com.pcverse.exception;

public class UnknownJsonFieldException extends IllegalArgumentException {

    private final String fieldName;

    public UnknownJsonFieldException(String fieldName) {
        super("Unknown JSON field: " + fieldName);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
