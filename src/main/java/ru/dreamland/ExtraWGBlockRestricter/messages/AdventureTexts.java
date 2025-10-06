package ru.dreamland.ExtraWGBlockRestricter.messages;

import java.util.Collections;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public final class AdventureTexts {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String MM_PREFIX = "<!mm>";

    private AdventureTexts() {
    }

    
    /** Basic method: parsing raw as MiniMessage (if has <!mm> or «like tags»), another legacy. */
    public static Component parse(String raw, Map<String, Component> placeholders) {
        if (raw == null || raw.isEmpty())
            return Component.empty();

        boolean forcedMM = raw.startsWith(MM_PREFIX);
        if (forcedMM)
            raw = raw.substring(MM_PREFIX.length());

        if (forcedMM || looksLikeMiniMessage(raw)) {
            TagResolver[] resolvers = buildResolvers(placeholders);
            return MM.deserialize(raw, resolvers);
        } else {
            Component base = LEGACY.deserialize(raw);
            if (placeholders != null && !placeholders.isEmpty()) {
                for (Map.Entry<String, Component> e : placeholders.entrySet()) {
                    base = base.replaceText(TextReplacementConfig.builder()
                            .matchLiteral("{" + e.getKey() + "}")
                            .replacement(e.getValue())
                            .build());
                }
            }
            return base;
        }
    }

    public static Component parse(String raw) {
        return parse(raw, Collections.emptyMap());
    }

    /** Locale material name: block.minecraft.stone / item.minecraft.apple */
    public static Component materialComponent(Material mat) {
        NamespacedKey key = mat.getKey(); // minecraft:stone
        String path = key.getKey(); // stone
        String ns = key.getNamespace(); // minecraft / mod
        String kind = mat.isBlock() ? "block" : "item";
        String transKey = kind + "." + ns + "." + path;
        return Component.translatable(transKey);
    }

    private static boolean looksLikeMiniMessage(String s) {
        int l = s.indexOf('<');
        if (l < 0)
            return false;
        int r = s.indexOf('>', l + 1);
        if (r < 0)
            return false;
        // small boost
        String inside = s.substring(l + 1, Math.min(r, l + 20)).toLowerCase();
        return inside.matches("[a-z/#].*");
    }

    private static TagResolver[] buildResolvers(Map<String, Component> placeholders) {
        if (placeholders == null || placeholders.isEmpty())
            return new TagResolver[0];
        return placeholders.entrySet().stream()
                .map(e -> Placeholder.component(e.getKey(), e.getValue()))
                .toArray(TagResolver[]::new);
    }
}
