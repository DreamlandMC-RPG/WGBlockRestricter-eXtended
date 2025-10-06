package ru.dreamland.ExtraWGBlockRestricter.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.session.SessionManager;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.dreamland.ExtraWGBlockRestricter.ExtraWGBlockRestricterPlugin;
import ru.dreamland.ExtraWGBlockRestricter.messages.AdventureTexts;
import ru.dreamland.ExtraWGBlockRestricter.util.Utils;
import net.kyori.adventure.text.Component;



public class BlockListener implements Listener {
    private final ExtraWGBlockRestricterPlugin plugin;

    public BlockListener(ExtraWGBlockRestricterPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasWgBypass(Player player) {
        // WG bypass: worldguard.region.bypass.<world>
        SessionManager sm = WorldGuard.getInstance().getPlatform().getSessionManager();
        return sm.hasBypass(WorldGuardPlugin.inst().wrapPlayer(player), BukkitAdapter.adapt(player.getWorld()));
    }
        // deny message understand MiniMessages and Legacy
        private void sendMsg(Player p, String path, String def, Material mat) {
        String raw = plugin.getConfig().getString(path, def);
        if (raw == null || raw.isEmpty()) return;

        Component msg = AdventureTexts.parse(
            raw,
            Map.of("block", AdventureTexts.materialComponent(mat)) // <block> / {block} → locale component
        );

        p.sendMessage(msg);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!e.hasItem()) return;

        Player p = e.getPlayer();
        if (hasWgBypass(p)) return;
        if (p.hasPermission("wgblockrestricter.ignore")) return;

        Material itemType = e.getItem().getType();
        if (!Utils.baseMaterials.containsKey(itemType)) return;

        Material asBlock = Utils.baseMaterials.get(itemType);
        // Location of target block
        if (e.getClickedBlock() == null || e.getBlockFace() == null) return;

        // Check permission to "place"
        if (!Utils.placeAllowedAtLocation(asBlock, e.getClickedBlock().getRelative(e.getBlockFace()).getLocation())) {
            sendMsg(p, "messages.deny-block-place", "&cYou are not allowed to place {block} here.", asBlock);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (hasWgBypass(p)) return;
        if (p.hasPermission("wgblockrestricter.ignore")) return;

        Material mat = e.getBlockPlaced().getType();
        if (!Utils.placeAllowedAtLocation(mat, e.getBlockPlaced().getLocation())) {
            sendMsg(p, "messages.deny-block-place", "&cYou are not allowed to place {block} here.", mat);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (hasWgBypass(p)) return;
        if (p.hasPermission("wgblockrestricter.ignore")) return;

        Material mat = e.getBlock().getType();
        if (!Utils.breakAllowedAtLocation(mat, e.getBlock().getLocation())) {
            sendMsg(p, "messages.deny-block-break", "&cYou are not allowed to break {block} here.", mat);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        Player p = e.getPlayer();
        if (p == null) return;

        if (hasWgBypass(p)) return;
        if (p.hasPermission("wgblockrestricter.ignore")) return;

        Material mat = (e.getEntity() instanceof ItemFrame) ? Material.ITEM_FRAME : Material.PAINTING;
        // check place near target block
        if (!Utils.placeAllowedAtLocation(mat, e.getBlock().getRelative(e.getBlockFace()).getLocation())) {
            sendMsg(p, "messages.deny-hanging-place", "&cYou are not allowed to place {block} here.", mat);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player)) return;

        Player p = (Player) e.getRemover();
        if (hasWgBypass(p)) return;
        if (p.hasPermission("wgblockrestricter.ignore")) return;

        Material mat = (e.getEntity() instanceof ItemFrame) ? Material.ITEM_FRAME : Material.PAINTING;
        if (!Utils.breakAllowedAtLocation(mat, e.getEntity().getLocation())) {
            sendMsg(p, "messages.deny-hanging-break", "&cYou are not allowed to break {block} here.", mat);
            e.setCancelled(true);
        }
    }
}
