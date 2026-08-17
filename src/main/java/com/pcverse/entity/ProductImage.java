package com.pcverse.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @NotBlank(message = "Image URL must not be blank")
    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String url;

    @Size(max = 255, message = "Image alt text must not exceed 255 characters")
    private String altText;

    @Builder.Default
    @PositiveOrZero(message = "Image display order must be greater than or equal to 0")
    private int displayOrder = 0;

    @Builder.Default
    private boolean primary = false;

}
