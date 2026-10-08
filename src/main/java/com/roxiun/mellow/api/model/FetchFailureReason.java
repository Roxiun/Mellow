package com.roxiun.mellow.api.model;

public enum FetchFailureReason {
    UUID_UNAVAILABLE,
    MISSING_API_KEY,
    NETWORK_ERROR,
    RATE_LIMITED,
    PROVIDER_ERROR,
    NO_PLAYER_DATA,
    PARSE_ERROR,
    UNKNOWN,
}
