package com.roxiun.mellow.config;

import java.util.ArrayList;
import java.util.List;
import org.polyfrost.oneconfig.api.config.v1.Node;
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
            case "buildBattleStatOrder" -> "buildBattleLayout";
            case "tntTagStatOrder" -> "tntTagLayout";
            case "bowSpleefStatOrder" -> "bowSpleefLayout";
            case "murderMysteryStatOrder" -> "murderMysteryLayout";
            case "tntRunStatOrder" -> "tntRunLayout";
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
            case "buildBattleLayout" -> "Build Battle Stat Order";
            case "tntTagLayout" -> "TNT Tag Stat Order";
            case "bowSpleefLayout" -> "Bow Spleef Stat Order";
            case "murderMysteryLayout" -> "Murder Mystery Stat Order";
            case "tntRunLayout" -> "TNT Run Stat Order";
            case "denickerSearch" -> "Search Settings";
            case "anticheatAlerts" -> "Alert Settings";
            case "winstreakFilters" -> "Fetch Filters";
            default -> throw new IllegalArgumentException(group);
        };
    }

}
