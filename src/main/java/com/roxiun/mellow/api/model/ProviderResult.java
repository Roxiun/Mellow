package com.roxiun.mellow.api.model;

public class ProviderResult<T> {

    private final T value;
    private final String error;
    private final FetchFailureReason failureReason;

    private ProviderResult(
        T value,
        String error,
        FetchFailureReason failureReason
    ) {
        this.value = value;
        this.error = error;
        this.failureReason = failureReason;
    }

    public static <T> ProviderResult<T> success(T value) {
        return new ProviderResult<>(value, null, null);
    }

    public static <T> ProviderResult<T> failure(String error) {
        return failure(FetchFailureReason.UNKNOWN, error);
    }

    public static <T> ProviderResult<T> failure(
        FetchFailureReason failureReason,
        String error
    ) {
        return new ProviderResult<>(
            null,
            error,
            failureReason == null ? FetchFailureReason.UNKNOWN : failureReason
        );
    }

    public boolean isSuccess() {
        return value != null;
    }

    public T getValue() {
        return value;
    }

    public String getError() {
        return error;
    }

    public FetchFailureReason getFailureReason() {
        return failureReason;
    }
}
