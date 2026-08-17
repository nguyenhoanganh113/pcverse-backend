package com.pcverse.enums;

public enum UserSearchField {
    USERNAME("username"),
    EMAIL("email"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    PHONE_NUMBER("phoneNumber"),
    GENDER("gender"),
    USER_STATUS("userStatus"),
    DATE_OF_BIRTH("dateOfBirth");

    private final String path;

    UserSearchField(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
