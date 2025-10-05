package ru.dreamland.ExtraWGBlockRestricter.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;

import ru.dreamland.ExtraWGBlockRestricter.ExtraWGBlockRestricterPlugin;

import java.util.*;
import java.util.logging.Logger;

public final class Utils {

    private static Logger LOGGER;

    /** maooing "item -> base block" for RIGHT_CLICK_BLOCK */
    public static final EnumMap<Material, Material> baseMaterials = new EnumMap<>(Material.class);

    /** aliases materilas names; 'any' used as wildcard (AIR) */
    private static final HashMap<String, Material> aliases = new HashMap<>();

    public static void init(Logger logger) {
        LOGGER = logger;

        baseMaterials.clear();
        aliases.clear();

        // Minimal needed:
        baseMaterials.put(Material.WATER_BUCKET, Material.WATER);
        baseMaterials.put(Material.LAVA_BUCKET,  Material.LAVA);
        baseMaterials.put(Material.STRING,       Material.TRIPWIRE);
        // sing (placed as block)
        try {
            baseMaterials.put(Material.OAK_SIGN, Material.OAK_SIGN);
        } catch (Throwable ignored) { }

        // Frendly aliases
        aliases.put("any", Material.AIR);
        aliases.put("redstone_lamp", Material.REDSTONE_LAMP);
        aliases.put("repeater", Material.REPEATER);
        aliases.put("comparator", Material.COMPARATOR);
        aliases.put("piston", Material.PISTON);
        aliases.put("sticky_piston", Material.STICKY_PISTON);
        aliases.put("sign", Material.OAK_SIGN);

        // Info for logs
        if (LOGGER != null) {
            LOGGER.info("ExtraWGBlockRestricter: aliases and base materials initialized.");
        }
    }

    // === public utils ===

    public static boolean placeAllowedAtLocation(Material mat, Location loc) {
        Material blockType = normalize(mat);
        RegionManager rm = getRegionManager(loc);
        if (rm == null) return true;

        ApplicableRegionSet regions = rm.getApplicableRegions(asBV3(loc));
        Boolean decision = decideByNearest(regions, blockType,
                ExtraWGBlockRestricterPlugin.ALLOW_PLACE,
                ExtraWGBlockRestricterPlugin.DENY_PLACE);

        if (decision != null) return decision;

        // fallback: global region
        return decideInGlobalOrDefault(rm, blockType,
                ExtraWGBlockRestricterPlugin.ALLOW_PLACE,
                ExtraWGBlockRestricterPlugin.DENY_PLACE);
    }

    public static boolean breakAllowedAtLocation(Material mat, Location loc) {
        Material blockType = normalize(mat);
        RegionManager rm = getRegionManager(loc);
        if (rm == null) return true;

        ApplicableRegionSet regions = rm.getApplicableRegions(asBV3(loc));
        Boolean decision = decideByNearest(regions, blockType,
                ExtraWGBlockRestricterPlugin.ALLOW_BREAK,
                ExtraWGBlockRestricterPlugin.DENY_BREAK);

        if (decision != null) return decision;

        return decideInGlobalOrDefault(rm, blockType,
                ExtraWGBlockRestricterPlugin.ALLOW_BREAK,
                ExtraWGBlockRestricterPlugin.DENY_BREAK);
    }

    /** understanding base WG logic: allow-blocks/deny-blocks as base */
    public static Object blockAllowedInRegion(ProtectedRegion region, Material blockType) {
        if (region == null) return null;

        Set<Material> allowed = region.getFlag(ExtraWGBlockRestricterPlugin.ALLOW_BLOCKS);
        Set<Material> denied  = region.getFlag(ExtraWGBlockRestricterPlugin.DENY_BLOCKS);

        boolean deny = false;
        if (contains(allowed, blockType) || contains(allowed, Material.AIR)) {
            return true;
        } else if (contains(denied, blockType) || contains(denied, Material.AIR)) {
            deny = true;
        } 
        return deny ? Boolean.FALSE : null;
    }

    public static Object placeAllowedInRegion(ProtectedRegion region, Material blockType) {
        if (region == null) return null;

        if (blockAllowedInRegion(region, blockType) == Boolean.FALSE) {
            return false;
        }

        Set<Material> allowed = region.getFlag(ExtraWGBlockRestricterPlugin.ALLOW_PLACE);
        Set<Material> denied  = region.getFlag(ExtraWGBlockRestricterPlugin.DENY_PLACE);

        boolean deny = false;
        if (contains(allowed, blockType) || contains(allowed, Material.AIR)) {
            return true;
        } else if (contains(denied, blockType) || contains(denied, Material.AIR)) {
            deny = true;
        }
        return deny ? Boolean.FALSE : null;
    }

    public static Object breakAllowedInRegion(ProtectedRegion region, Material blockType) {
        if (region == null) return null;

        if (blockAllowedInRegion(region, blockType) == Boolean.FALSE) {
            return false;
        }

        Set<Material> allowed = region.getFlag(ExtraWGBlockRestricterPlugin.ALLOW_BREAK);
        Set<Material> denied  = region.getFlag(ExtraWGBlockRestricterPlugin.DENY_BREAK);

        boolean deny = false;
        if (contains(allowed, blockType) || contains(allowed, Material.AIR)) {
            return true;
        } else if (contains(denied, blockType) || contains(denied, Material.AIR)) {
            deny = true;
        }
        return deny ? Boolean.FALSE : null;
    }

    // === private helpers ===

    private static BlockVector3 asBV3(Location loc) {
        return BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static RegionManager getRegionManager(Location loc) {
        return WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(loc.getWorld()));
    }

    /** copy «dauter priority top of parent» and «allow > deny» */
    private static Boolean decideByNearest(
        com.sk89q.worldguard.protection.ApplicableRegionSet regions,
        org.bukkit.Material blockType,
        com.sk89q.worldguard.protection.flags.SetFlag<org.bukkit.Material> allowFlag,
        com.sk89q.worldguard.protection.flags.SetFlag<org.bukkit.Material> denyFlag
    ) {
        java.util.Map<com.sk89q.worldguard.protection.regions.ProtectedRegion, Boolean> regionsToCheck = new java.util.HashMap<>();
        java.util.Set<com.sk89q.worldguard.protection.regions.ProtectedRegion> ignoredParents = new java.util.HashSet<>();

        for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regions) {
            if (ignoredParents.contains(region)) continue;

            Boolean allowed = null;

            // base allow/deny for blocks
            Object base = blockAllowedInRegion(region, blockType);
            if (base == Boolean.FALSE) {
                allowed = false;
            } else {
                java.util.Set<org.bukkit.Material> allowedSet = region.getFlag(allowFlag);
                java.util.Set<org.bukkit.Material> deniedSet  = region.getFlag(denyFlag);

                if (contains(allowedSet, blockType) || contains(allowedSet, org.bukkit.Material.AIR)) {
                    allowed = true;
                } else if (contains(deniedSet, blockType) || contains(deniedSet, org.bukkit.Material.AIR)) {
                    allowed = false;
                }
            }

            if (allowed != null) {
                // «dauters top of parents»
                com.sk89q.worldguard.protection.regions.ProtectedRegion parent = region.getParent();
                while (parent != null) {
                    ignoredParents.add(parent);
                    parent = parent.getParent();
                }
                regionsToCheck.put(region, allowed);
            }
        }

        if (!regionsToCheck.isEmpty()) {
            // allow > deny
            for (java.util.Map.Entry<com.sk89q.worldguard.protection.regions.ProtectedRegion, Boolean> e : regionsToCheck.entrySet()) {
                if (ignoredParents.contains(e.getKey())) continue;
                if (Boolean.TRUE.equals(e.getValue())) return true;
            }
            return false;
        }
        return null;
    }

    private static boolean decideInGlobalOrDefault(
        com.sk89q.worldguard.protection.managers.RegionManager rm,
        org.bukkit.Material blockType,
        com.sk89q.worldguard.protection.flags.SetFlag<org.bukkit.Material> allowFlag,
        com.sk89q.worldguard.protection.flags.SetFlag<org.bukkit.Material> denyFlag
    ) {
        com.sk89q.worldguard.protection.regions.ProtectedRegion global = rm.getRegion("__global__");
        if (global != null) {
            java.util.Set<org.bukkit.Material> allowed = global.getFlag(allowFlag);
            java.util.Set<org.bukkit.Material> denied  = global.getFlag(denyFlag);

            if (contains(allowed, blockType) || contains(allowed, org.bukkit.Material.AIR)) return true;
            if (contains(denied,  blockType) || contains(denied,  org.bukkit.Material.AIR)) return false;
        }
        return true; // default: true
    }

    private static boolean contains(Set<Material> set, Material m) {
        return set != null && m != null && set.contains(m);
    }

    private static Material normalize(Material m) {
        // mapping "special" items to base blocks
        if (baseMaterials.containsKey(m)) return baseMaterials.get(m);
        return m;
    }

    public static Material resolveAlias(String input) {
        String key = input.toLowerCase().trim().replace(' ', '_');
        return aliases.getOrDefault(key, null);
    }

    public static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

}
