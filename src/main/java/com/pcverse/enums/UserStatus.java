package com.pcverse.enums;


public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DISABLED,
    DELETED;

    public static UserStatus from(String value) {
        return UserStatus.valueOf(value.trim().toUpperCase());
    }

}
