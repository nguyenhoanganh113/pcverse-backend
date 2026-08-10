package com.pcverse.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.pcverse.exception.UnknownJsonFieldException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * RFC 7396 JSON Merge Patch document for a category.
 *
 * <p>A missing property leaves the current value unchanged. A present
 * {@code null} removes a nullable property, while any other value replaces
 * the current value.</p>
 */
public class UpdateCategoryRequest {

    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    private String description;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @PositiveOrZero(message = "Display order must be greater than or equal to 0")
    private Integer displayOrder;

    @NotNull(message = "Version must not be null")
    @PositiveOrZero(message = "Version must be greater than or equal to 0")
    private Long version;

    private boolean namePresent;
    private boolean descriptionPresent;
    private boolean imageUrlPresent;
    private boolean displayOrderPresent;

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

    public String imageUrl() {
        return imageUrl;
    }

    public Integer displayOrder() {
        return displayOrder;
    }

    public Long version() {
        return version;
    }

    // Chỉ định setter dùng khi deserialize thuộc tính JSON
    @JsonSetter("description")
    public void setDescription(String description) {
        this.descriptionPresent = true;
        this.description = description;
    }

    @JsonSetter("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrlPresent = true;
        this.imageUrl = imageUrl;
    }

    @JsonSetter("displayOrder")
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrderPresent = true;
        this.displayOrder = displayOrder;
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
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

    @JsonIgnore
    public boolean isImageUrlPresent() {
        return imageUrlPresent;
    }

    @JsonIgnore
    public boolean isDisplayOrderPresent() {
        return displayOrderPresent;
    }

    @JsonIgnore
    public boolean hasAnyField() {
        return namePresent
                || descriptionPresent
                || imageUrlPresent
                || displayOrderPresent;
    }
}
