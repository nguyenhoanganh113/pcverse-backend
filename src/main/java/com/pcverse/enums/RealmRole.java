package com.pcverse.enums;

public enum RealmRole {
    ADMIN("PC-Verse administrator"),
    CUSTOMER("PC-Verse customer");

    private final String description;

    RealmRole(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
