package ru.dreamland.ExtraWGBlockRestricter.flags;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import ru.dreamland.ExtraWGBlockRestricter.util.Utils;
import javax.annotation.Nullable;

public class BlockMaterialFlag extends Flag<Material> {

    public BlockMaterialFlag(String name) {
        super(name);
    }

    @Override
    public Material parseInput(FlagContext context) throws InvalidFlagFormat {
        String input = context.getUserInput();
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidFlagFormat("Empty material value.");
        }

        String raw = input.trim();

        // ALiases
        Material alias = Utils.resolveAlias(raw);
        if (alias != null) {
            validateBlock(alias);
            return alias;
        }

        // Only names, without numbers
        if (Utils.isNumeric(raw)) {
            throw new InvalidFlagFormat(raw + " is a legacy numeric id and is not supported. Use a material name, e.g. STONE.");
        }

        Material mat = tryMatch(raw);
        if (mat == null) mat = tryMatch(raw.replace(' ', '_').replace('-', '_'));
        if (mat == null) mat = tryMatch(raw.toUpperCase());

        if (mat == null) {
            throw new InvalidFlagFormat(raw + " is not a valid material name.");
        }
        validateBlock(mat);
        return mat;
    }

    @Override
    public Material unmarshal(@Nullable Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        Material m = tryMatch(s);
        if (m == null) m = tryMatch(s.replace(' ', '_').replace('-', '_'));
        if (m == null) m = tryMatch(s.toUpperCase());
        return m;
    }

    @Override
    public Object marshal(@Nullable Material o) {
        return (o == null) ? null : o.name();
    }

    private static Material tryMatch(String s) {
        // namespaced?
        if (s.indexOf(':') >= 0) {
            try {
                NamespacedKey key = NamespacedKey.fromString(s.toLowerCase());
                if (key != null) {
                    Material m = Material.matchMaterial(key.toString());
                    if (m != null) return m;
                }
            } catch (IllegalArgumentException ignored) { }
        }
        Material m = Material.matchMaterial(s);
        if (m != null) return m;

        try {
            return Material.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void validateBlock(Material mat) throws InvalidFlagFormat {
        if (!mat.isBlock()) {
            throw new InvalidFlagFormat(mat.name() + " is not a block.");
        }
    }
}
