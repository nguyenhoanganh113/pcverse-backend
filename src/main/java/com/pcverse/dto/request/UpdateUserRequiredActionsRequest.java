package com.pcverse.dto.request;

import com.pcverse.enums.KeycloakRequiredAction;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRequiredActionsRequest(

        @NotNull(message = "Required actions must not be null")
        List<@NotNull(message = "Required action must not be null") KeycloakRequiredAction> actions

) {
}
