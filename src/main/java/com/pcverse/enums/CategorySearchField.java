package com.pcverse.enums;

public enum CategorySearchField {

    NAME("name"),
    SLUG("slug"),
    DESCRIPTION("description");

    private final String path;

    CategorySearchField(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
