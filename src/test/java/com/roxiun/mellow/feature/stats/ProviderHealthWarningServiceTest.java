package com.roxiun.mellow.feature.stats;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProviderHealthWarningServiceTest {

    @Test
    public void warnsForDeprecatedProviders() {
        String nadeshikoWarning = ProviderHealthWarningService.getWarningMessage(
            1,
            ""
        );
        String abyssWarning = ProviderHealthWarningService.getWarningMessage(2, "");

        assertTrue(nadeshikoWarning.contains("Nadeshiko is deprecated"));
        assertTrue(nadeshikoWarning.contains("Bordic"));
        assertTrue(nadeshikoWarning.contains("Hypixel Public API"));
        assertTrue(abyssWarning.contains("Abyss is deprecated"));
        assertTrue(abyssWarning.contains("Bordic"));
        assertTrue(abyssWarning.contains("Hypixel Public API"));
    }

    @Test
    public void warnsWhenHypixelHasNoApiKey() {
        assertTrue(
            ProviderHealthWarningService.getWarningMessage(0, null).contains(
                "no API key is set"
            )
        );
        assertTrue(
            ProviderHealthWarningService.getWarningMessage(0, "   ").contains(
                "Bordic"
            )
        );
    }

    @Test
    public void doesNotWarnForConfiguredHypixelOrBordic() {
        assertNull(ProviderHealthWarningService.getWarningMessage(0, "api-key"));
        assertNull(ProviderHealthWarningService.getWarningMessage(3, ""));
    }
}
