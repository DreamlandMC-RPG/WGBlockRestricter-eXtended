package ru.dreamland.ExtraWGBlockRestricter.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import io.papermc.paper.plugin.configuration.PluginMeta;
import ru.dreamland.ExtraWGBlockRestricter.ExtraWGBlockRestricterPlugin;
import ru.dreamland.ExtraWGBlockRestricter.util.Utils;

public class ExtraWGBRCommand implements CommandExecutor, TabCompleter {
    private final ExtraWGBlockRestricterPlugin plugin;

    public ExtraWGBRCommand(ExtraWGBlockRestricterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("ExtraWGBlockRestricter — use: /" + label + " reload | version");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload": {
                if (!sender.hasPermission("extrawgblockrestricter.reload")) {
                    sender.sendMessage("§cPermission denied.");
                    return true;
                }
                // перечитать конфиг и переинициализировать утилиты
                plugin.reloadConfig();
                Utils.init(plugin.getLogger());
                sender.sendMessage("§aExtraWGBlockRestricter reloaded.");
                plugin.getLogger().info(sender.getName() + " used /" + label + " reload");
                return true;
            }
            case "version": {
                PluginMeta meta = plugin.getPluginMeta();
                String name = meta.getDisplayName() != null ? meta.getDisplayName() : meta.getName();
                String ver  = meta.getVersion();
                sender.sendMessage("§7" + name + " §av" + ver);
                return true;
            }
            default:
                sender.sendMessage("ExtraWGBlockRestricter — use: /" + label + " reload | version");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("extrawgblockrestricter.reload")) subs.add("reload");
            subs.add("version");
            List<String> out = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], subs, out);
            Collections.sort(out);
            return out;
        }
        return Collections.emptyList();
    }
}
