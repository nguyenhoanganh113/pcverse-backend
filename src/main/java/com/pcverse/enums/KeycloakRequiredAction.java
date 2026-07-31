package com.pcverse.enums;

public enum KeycloakRequiredAction {
    VERIFY_EMAIL("VERIFY_EMAIL"),
    UPDATE_EMAIL("UPDATE_EMAIL"),
    UPDATE_PASSWORD("UPDATE_PASSWORD"),
    CONFIGURE_TOTP("CONFIGURE_TOTP"),
    UPDATE_PROFILE("UPDATE_PROFILE"),
    VERIFY_PROFILE("VERIFY_PROFILE");

    private final String providerId;

    KeycloakRequiredAction(String providerId) {
        this.providerId = providerId;
    }

    public String providerId() {
        return providerId;
    }
}
