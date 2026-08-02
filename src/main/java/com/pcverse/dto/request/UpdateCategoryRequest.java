package com.pcverse.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.pcverse.exception.UnknownJsonFieldException;
import jakarta.validation.constraints.Size;

/**
 * RFC 7396 JSON Merge Patch document for a category.
 *
 * <p>A missing property leaves the current value unchanged. A present
 * {@code null} removes a nullable property, while any other value replaces
 * the current value.</p>
 */
public class UpdateCategoryRequest {

    @Size(max = 120, message = "Name must not exceed 120 characters")
    private String name;

    private String description;

    private boolean namePresent;
    private boolean descriptionPresent;

    public String name() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.namePresent = true;
        this.name = name;
    }

    public String description() {
        return description;
    }

    // Chỉ định setter dùng khi deserialize thuộc tính JSON
    @JsonSetter("description")
    public void setDescription(String description) {
        this.descriptionPresent = true;
        this.description = description;
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new UnknownJsonFieldException(fieldName);
    }

    @JsonIgnore
    public boolean isNamePresent() {
        return namePresent;
    }

    @JsonIgnore
    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }
}
