package ru.dreamland.ExtraWGBlockRestricter;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import ru.dreamland.ExtraWGBlockRestricter.command.ExtraWGBRCommand;
import ru.dreamland.ExtraWGBlockRestricter.flags.BlockMaterialFlag;
import ru.dreamland.ExtraWGBlockRestricter.listener.BlockListener;
import ru.dreamland.ExtraWGBlockRestricter.util.Utils;

public final class ExtraWGBlockRestricterPlugin extends JavaPlugin {

    public static Flag<Material> BLOCK_TYPE_FLAG;

    public static SetFlag<Material> ALLOW_BLOCKS;
    public static SetFlag<Material> DENY_BLOCKS;
    public static SetFlag<Material> ALLOW_PLACE;
    public static SetFlag<Material> DENY_PLACE;
    public static SetFlag<Material> ALLOW_BREAK;
    public static SetFlag<Material> DENY_BREAK;

    @Override
    public void onLoad() {
        // Register castom flags before WG started
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

            BLOCK_TYPE_FLAG = new BlockMaterialFlag("block-type");
            ALLOW_BLOCKS = new SetFlag<>("allow-blocks", BLOCK_TYPE_FLAG);
            DENY_BLOCKS  = new SetFlag<>("deny-blocks", BLOCK_TYPE_FLAG);
            ALLOW_PLACE  = new SetFlag<>("allow-place", BLOCK_TYPE_FLAG);
            DENY_PLACE   = new SetFlag<>("deny-place", BLOCK_TYPE_FLAG);
            ALLOW_BREAK  = new SetFlag<>("allow-break", BLOCK_TYPE_FLAG);
            DENY_BREAK   = new SetFlag<>("deny-break", BLOCK_TYPE_FLAG);

            registry.register(ALLOW_BLOCKS);
            registry.register(DENY_BLOCKS);
            registry.register(ALLOW_PLACE);
            registry.register(DENY_PLACE);
            registry.register(ALLOW_BREAK);
            registry.register(DENY_BREAK);
        } catch (FlagConflictException e) {
            getLogger().severe("Flag name conflict: " + e.getMessage());
            // NO continue with fail
        } catch (Throwable t) {
            getLogger().severe("Failed to register flags: " + t.getMessage());
        }
    }

    @Override
    public void onEnable() {
        // Check WG
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard not found. Disabling ExtraWGBlockRestricter.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        Utils.init(getLogger()); // initial aliases/base mapping

        // Register listener
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        
        // Регистрируем команду /ewgbr
        ExtraWGBRCommand cmd = new ExtraWGBRCommand(this);
        Objects.requireNonNull(getCommand("ewgbr"), "Команда ewgbr не объявлена в plugin.yml")
                .setExecutor(cmd);
        getCommand("ewgbr").setTabCompleter(cmd);

        getLogger().info("ExtraWGBlockRestricter enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ExtraWGBlockRestricter disabled.");
    }
}
