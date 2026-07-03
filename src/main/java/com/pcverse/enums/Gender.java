package com.pcverse.enums;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    public static Gender from(String value) {
        return Gender.valueOf(value.trim().toUpperCase());
    }
}
