package com.roxiun.mellow.config;

import org.junit.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.DraggableList;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import static org.junit.Assert.*;

public class ConfigValidationTest {
    public static class Settings {
        @Dropdown(title = "Provider", options = {"None", "Aurora", "Luna"})
        public int provider = 3;
        @Number(title = "Limit", min = -1, max = 500)
        public int limit = -20;
        @DraggableList(title = "Columns", options = {"Name", "Ping"})
        public String[] columns = {"Ping", "removed", null, "Name", "Ping"};
    }

    @Test public void sanitizesSavedValuesWithoutReorderingValidColumns() {
        Settings settings = new Settings();
        assertTrue(ConfigValidation.sanitize(settings));
        assertEquals(2, settings.provider);
        assertEquals(-1, settings.limit);
        assertArrayEquals(new String[]{"Ping", "Name"}, settings.columns);
        assertFalse(ConfigValidation.sanitize(settings));
    }

    @Test public void preservesAnIntentionallyEmptyColumnList() {
        Settings settings = new Settings();
        settings.columns = new String[0];
        ConfigValidation.sanitize(settings);
        assertArrayEquals(new String[0], settings.columns);
    }
}
