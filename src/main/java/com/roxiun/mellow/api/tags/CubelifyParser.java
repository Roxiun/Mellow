package com.roxiun.mellow.api.tags;

import com.google.gson.*;
import com.roxiun.mellow.api.provider.model.*;
import java.util.*;

/** Decode the shared envelope; each provider explicitly maps its tags and HTTP-200 error badges. */
public final class CubelifyParser {
    private CubelifyParser() {}
    public interface Mapping {
        String error(JsonObject response);
        PlayerTag tag(JsonObject tag);
    }
    public static ProviderResult<List<PlayerTag>> parse(String body, Mapping mapping) {
        try {
            JsonObject response = new JsonParser().parse(body).getAsJsonObject();
            String error = mapping.error(response);
            if (error != null) return ProviderResult.failure(FetchFailureReason.PROVIDER_ERROR, error);
            JsonArray tags = response.getAsJsonArray("tags");
            if (tags == null) return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, "Missing tags");
            List<PlayerTag> result = new ArrayList<>();
            for (JsonElement entry : tags) {
                PlayerTag tag = mapping.tag(entry.getAsJsonObject());
                if (tag != null) result.add(tag);
            }
            return ProviderResult.success(Collections.unmodifiableList(result));
        } catch (RuntimeException error) {
            return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, error.getMessage());
        }
    }
}
