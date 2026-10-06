package com.roxiun.mellow.launch;

/** Matches the official loader's four-component version negotiation format. */
final class ApiVersion {
    private ApiVersion() {}

    static long parse(String version) {
        String[] parts = version.split("\\.", -1);
        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Unsupported Hypixel API version: " + version);
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            int part = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            if (part < 0 || part >= 10000) {
                throw new IllegalArgumentException("Invalid Hypixel API version: " + version);
            }
            result = result * 10000 + part;
        }
        return result;
    }
}
