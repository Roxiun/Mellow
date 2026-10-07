package com.roxiun.mellow.config;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.polyfrost.oneconfig.api.config.v1.Node;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;

/** Native OneConfig accordions bound to the existing fields, without duplicate preference state. */
final class PreferenceLayout {
    private PreferenceLayout() {}

    private static final List<String> CATEGORIES = List.of("General", "Stats", "Tab Stats",
        "Tags & Blacklists", "Denicker", "Requests", "Hitboxes & Nametags", "Replays", "Anticheat", "API Keys");

    static Tree arrange(Tree collected) {
        Tree result = new Tree(collected.getID(), collected.getTitle(), collected.description, null);
        result.addMetadata(collected.getMetadata());
        List<Node> nodes = new ArrayList<>(collected.map.values());
        // Stable sorting preserves declaration order inside each category.
        nodes.sort(java.util.Comparator.comparingInt(node -> {
            String category = node.getMetadata("category");
            int index = category == null ? -1 : CATEGORIES.indexOf(category);
            return index < 0 ? CATEGORIES.size() : index;
        }));
        for (Node node : nodes) {
            String group = groupFor(node.getID());
            if (group == null) {
                result.put(node);
                continue;
            }
            Tree accordion = result.getChild(group);
            if (accordion == null) {
                accordion = new Tree(group, groupTitle(group), null, null);
                accordion.addMetadata("category", node.getMetadata("category"));
                accordion.addMetadata("subcategory", node.getMetadata("subcategory"));
                accordion.addMetadata("collapsed", true);
                result.put(accordion);
            }
            accordion.put(node);
        }
        return result;
    }

    static String groupFor(String field) {
        if (field.startsWith("showDot")) return "tabSeparators";
        if (field.startsWith("hitbox")) return "colourAdjustments";
        return switch (field) {
            case "bedwarsStatOrder" -> "bedwarsLayout";
            case "skywarsStatOrder" -> "skywarsLayout";
            case "duelsStatOrder" -> "duelsLayout";
            case "finalsRange", "bedsRange", "maxResults" -> "denickerSearch";
            case "anticheatVerbose", "anticheatVl", "anticheatCooldown" -> "anticheatAlerts";
            case "winstreakMinStars", "winstreakMinFkdr" -> "winstreakFilters";
            default -> null;
        };
    }

    private static String groupTitle(String group) {
        return switch (group) {
            case "tabSeparators" -> "Standard View Separators";
            case "colourAdjustments" -> "Colour Adjustments";
            case "bedwarsLayout" -> "BedWars Stat Order";
            case "skywarsLayout" -> "SkyWars Stat Order";
            case "duelsLayout" -> "Duels Stat Order";
            case "denickerSearch" -> "Search Settings";
            case "anticheatAlerts" -> "Alert Settings";
            case "winstreakFilters" -> "Fetch Filters";
            default -> throw new IllegalArgumentException(group);
        };
    }

    static boolean hasSavedValue(JsonObject saved, String field) {
        if (saved == null) return false;
        String group = groupFor(field);
        return saved.has(field) || (group != null && saved.has(group)
            && saved.get(group).isJsonObject() && saved.getAsJsonObject(group).has(field));
    }

    /** Old flat files load as extra root properties. Move their values once, then remove the stale keys. */
    static boolean migrateFlatValues(Tree tree, JsonObject saved) {
        Map<String, Node> retained = new LinkedHashMap<>(tree.map);
        boolean changed = false;
        for (Node node : tree.map.values()) {
            if (!(node instanceof Tree group)) continue;
            JsonObject nested = saved != null && saved.has(group.getID()) && saved.get(group.getID()).isJsonObject()
                ? saved.getAsJsonObject(group.getID()) : null;
            for (Node child : group.map.values()) {
                if (!group.getID().equals(groupFor(child.getID()))) continue;
                Property<?> old = tree.getProp(child.getID());
                if (old == null) continue;
                if (nested == null || !nested.has(child.getID())) {
                    try {
                        child.overwrite(old, false);
                    } catch (RuntimeException invalidValue) {
                        org.apache.logging.log4j.LogManager.getLogger("Mellow").warn(
                            "Could not migrate preference {}; keeping its default", child.getID());
                    }
                }
                retained.remove(child.getID());
                changed = true;
            }
        }
        if (changed) {
            var metadata = new LinkedHashMap<>(tree.getMetadata());
            tree.clear();
            tree.addMetadata(metadata);
            retained.values().forEach(tree::put);
        }
        return changed;
    }
}
