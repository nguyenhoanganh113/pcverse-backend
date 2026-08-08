package com.pcverse.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateAttributeDefinitionRequest {

        @Size(
                max = 150,
                message = "Tên thuộc tính không được vượt quá 150 ký tự"
        )
        private String name;

        @JsonIgnore
        private boolean namePresent;

        @NotNull(message = "Version must not be null")
        private Long version;

        @JsonSetter("name")
        public void setName(String name) {
                this.namePresent = true;
                this.name = stripToNull(name);
        }

        @JsonSetter("version")
        public void setVersion(Long version) {
                this.version = version;
        }

        @JsonIgnore
        public boolean hasAnyField() {
                return namePresent;
        }

        private static String stripToNull(String value) {
                return value == null || value.isBlank()
                        ? null
                        : value.strip();
        }
}
