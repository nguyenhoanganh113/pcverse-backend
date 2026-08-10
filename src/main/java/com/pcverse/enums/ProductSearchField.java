package com.pcverse.enums;

import lombok.Getter;

@Getter
public enum ProductSearchField {

    NAME("name"),
    DESCRIPTION("description"),
    PRICE("price"),
    STOCK_AVAILABILITY("quantity"),
    PRODUCT_STATUS("productStatus");

    private final String path;

    ProductSearchField(String path) {
        this.path = path;
    }
}
