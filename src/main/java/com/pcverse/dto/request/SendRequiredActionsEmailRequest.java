package com.pcverse.dto.request;

import com.pcverse.enums.KeycloakRequiredAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record SendRequiredActionsEmailRequest(

        @NotEmpty(message = "At least one required action is required")
        List<@NotNull(message = "Required action must not be null") KeycloakRequiredAction> actions,

        @Min(value = 60, message = "Email action lifespan must be at least 60 seconds")
        @Max(value = 86400, message = "Email action lifespan must not exceed 86400 seconds")
        Integer lifespanSeconds

) {

    private static final int DEFAULT_LIFESPAN_SECONDS = 900;

    public SendRequiredActionsEmailRequest {
        if (actions != null) {
            actions = List.copyOf(actions);
        }
    }

    public int resolvedLifespanSeconds() {
        return lifespanSeconds == null
                ? DEFAULT_LIFESPAN_SECONDS
                : lifespanSeconds;
    }
}
