package com.pcverse.dto.request;

import com.pcverse.enums.KeycloakRequiredAction;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UpdateUserRequiredActionsRequest(

        @NotNull(message = "Required actions must not be null")
        List<@NotNull(message = "Required action must not be null") KeycloakRequiredAction> actions

) {

    public UpdateUserRequiredActionsRequest {
        if (actions != null) {
            actions = List.copyOf(actions);
        }
    }
}
