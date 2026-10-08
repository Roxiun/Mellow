package com.roxiun.mellow.config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.polyfrost.oneconfig.api.config.v1.annotations.DraggableList;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;

/** UI bounds do not validate values loaded from older profiles. */
final class ConfigValidation {
    private ConfigValidation() {}

    static boolean sanitize(Object config) {
        boolean changed = false;
        for (Field field : config.getClass().getDeclaredFields()) {
            Dropdown dropdown = field.getAnnotation(Dropdown.class);
            Number number = field.getAnnotation(Number.class);
            DraggableList list = field.getAnnotation(DraggableList.class);
            if (dropdown == null && number == null && list == null) continue;
            try {
                field.setAccessible(true);
                if (field.getType() == int.class && (dropdown != null || number != null)) {
                    int current = field.getInt(config);
                    int minimum = dropdown != null ? 0 : (int) number.min();
                    int maximum = dropdown != null ? dropdown.options().length - 1 : (int) number.max();
                    int normalized = Math.max(minimum, Math.min(maximum, current));
                    if (current != normalized) {
                        field.setInt(config, normalized);
                        changed = true;
                    }
                } else if (field.getType() == String[].class && list != null) {
                    String[] current = (String[]) field.get(config);
                    Set<String> allowed = new LinkedHashSet<>(Arrays.asList(list.options()));
                    String[] normalized = current == null ? new String[0] : Arrays.stream(current)
                        .filter(allowed::contains).distinct().toArray(String[]::new);
                    if (!Arrays.equals(current, normalized)) {
                        field.set(config, normalized);
                        changed = true;
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot validate option " + field.getName(), e);
            }
        }
        return changed;
    }
}
