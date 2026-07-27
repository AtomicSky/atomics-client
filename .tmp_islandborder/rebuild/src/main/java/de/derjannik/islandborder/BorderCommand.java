/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.BorderConfig;
import de.derjannik.islandborder.IslandBorderPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class BorderCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "info", "reload", "toggle", "pos1", "pos2", "setworld", "setouter", "setdivider", "enforcedivider", "switchside", "layers", "protection");
    private final IslandBorderPlugin plugin;
    private final BorderConfig config;
    private final BorderListener borderListener;
    private final BiomeTeamManager teamManager;
    private final Map<UUID, double[]> pendingCorners = new HashMap<UUID, double[]>();

    public BorderCommand(IslandBorderPlugin plugin, BorderConfig config, BorderListener borderListener, BiomeTeamManager teamManager) {
        this.plugin = plugin;
        this.config = config;
        this.borderListener = borderListener;
        this.teamManager = teamManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("islandborder.admin")) {
            sender.sendMessage(this.config.message("no-permission"));
            return true;
        }
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help": {
                this.sendHelp(sender);
                return true;
            }
            case "info": {
                this.sendInfo(sender);
                return true;
            }
            case "reload": {
                this.config.load();
                this.teamManager.load();
                this.borderListener.clearStoredSides();
                this.plugin.restartParticleTask();
                sender.sendMessage(this.config.message("reloaded"));
                return true;
            }
            case "toggle": {
                this.config.setEnabled(!this.config.isEnabled());
                this.config.save();
                sender.sendMessage(this.config.message(this.config.isEnabled() ? "toggled-on" : "toggled-off"));
                return true;
            }
            case "pos1": {
                return this.handlePos1(sender);
            }
            case "pos2": {
                return this.handlePos2(sender);
            }
            case "setworld": {
                return this.handleSetWorld(sender, args);
            }
            case "setouter": {
                return this.handleSetOuter(sender, args);
            }
            case "setdivider": {
                return this.handleSetDivider(sender, args);
            }
            case "enforcedivider": {
                return this.handleEnforceDivider(sender, args);
            }
            case "switchside": {
                return this.handleSwitchSide(sender, args);
            }
            case "layers": {
                return this.handleLayers(sender, args);
            }
            case "protection": {
                return this.handleProtection(sender, args);
            }
        }
        sender.sendMessage(this.config.message("invalid-usage"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "=== IslandBorder setup ===");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "The quick way, no config file needed:");
        sender.sendMessage(String.valueOf(ChatColor.WHITE) + " 1. " + String.valueOf(ChatColor.YELLOW) + "/border pos1" + String.valueOf(ChatColor.GRAY) + " - stand in one corner of the whole area");
        sender.sendMessage(String.valueOf(ChatColor.WHITE) + " 2. " + String.valueOf(ChatColor.YELLOW) + "/border pos2" + String.valueOf(ChatColor.GRAY) + " - stand in the opposite corner");
        sender.sendMessage(String.valueOf(ChatColor.WHITE) + " 3. " + String.valueOf(ChatColor.YELLOW) + "/border setdivider x" + String.valueOf(ChatColor.GRAY) + " - stand on the line splitting the islands");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "    (use " + String.valueOf(ChatColor.YELLOW) + "z" + String.valueOf(ChatColor.GRAY) + " instead of " + String.valueOf(ChatColor.YELLOW) + "x" + String.valueOf(ChatColor.GRAY) + " if the split runs the other way - /border info shows both)");
        sender.sendMessage(String.valueOf(ChatColor.WHITE) + " 4. " + String.valueOf(ChatColor.YELLOW) + "/border info" + String.valueOf(ChatColor.GRAY) + " - check it all looks right. Done.");
        sender.sendMessage("");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Other commands:");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border toggle" + String.valueOf(ChatColor.GRAY) + " - turn the border on/off");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border enforcedivider <true|false>" + String.valueOf(ChatColor.GRAY) + " - block or allow crossing between the islands");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border switchside <player|selector>" + String.valueOf(ChatColor.GRAY) + " - move selected players to the other side");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border layers <count> [gap] [distance]" + String.valueOf(ChatColor.GRAY) + " - extra particle lines above/below the border");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border protection <true|false>" + String.valueOf(ChatColor.GRAY) + " - block building/hitting/shooting across the border");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border setworld [name]" + String.valueOf(ChatColor.GRAY) + " - use the world you are standing in");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border setouter <x1> <z1> <x2> <z2>" + String.valueOf(ChatColor.GRAY) + " - type corners manually");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + " /border reload" + String.valueOf(ChatColor.GRAY) + " - reload config.yml");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Note: ops walk through the border on purpose. Test with a normal player.");
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "=== IslandBorder Info ===");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "World: " + String.valueOf(ChatColor.WHITE) + this.config.getWorldName() + (String)(Bukkit.getWorld((String)this.config.getWorldName()) == null ? String.valueOf(ChatColor.RED) + " (not loaded on this server!)" : ""));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Outer border: " + String.valueOf(ChatColor.WHITE) + "(" + this.fmt(this.config.getMinX()) + ", " + this.fmt(this.config.getMinZ()) + ") to (" + this.fmt(this.config.getMaxX()) + ", " + this.fmt(this.config.getMaxZ()) + ")");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Divider: " + String.valueOf(ChatColor.WHITE) + this.config.getDividerAxis() + " = " + this.fmt(this.config.getDividerCoordinate()) + " (positive side = " + this.config.getDividerPositiveSide() + ")");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Divider enforced: " + String.valueOf(ChatColor.WHITE) + this.config.isDividerEnforced());
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Border enabled: " + String.valueOf(ChatColor.WHITE) + this.config.isEnabled());
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Protection (no building/hitting/shooting across): " + String.valueOf(ChatColor.WHITE) + this.config.isProtectionEnabled());
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Particle layers: " + String.valueOf(ChatColor.WHITE) + this.config.getParticleLayers() + String.valueOf(ChatColor.GRAY) + " above + " + String.valueOf(ChatColor.WHITE) + this.config.getParticleLayers() + String.valueOf(ChatColor.GRAY) + " below, gap " + String.valueOf(ChatColor.WHITE) + this.fmt(this.config.getParticleLayerSpacing()) + String.valueOf(ChatColor.GRAY) + ", from " + String.valueOf(ChatColor.WHITE) + this.config.getParticleLayerDistance() + String.valueOf(ChatColor.GRAY) + " blocks away");
        if (sender instanceof Player) {
            Player player = (Player)sender;
            double[] corner = this.pendingCorners.get(player.getUniqueId());
            if (corner != null) {
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Pending corner 1: " + String.valueOf(ChatColor.WHITE) + "(" + this.fmt(corner[0]) + ", " + this.fmt(corner[1]) + ")" + String.valueOf(ChatColor.GRAY) + " - run /border pos2 in the opposite corner");
            }
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "You are standing at: " + String.valueOf(ChatColor.WHITE) + "x " + this.fmt(player.getLocation().getX()) + ", z " + this.fmt(player.getLocation().getZ()) + String.valueOf(ChatColor.GRAY) + " in " + String.valueOf(ChatColor.WHITE) + player.getWorld().getName());
        }
    }

    private boolean handlePos1(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        this.pendingCorners.put(player.getUniqueId(), new double[]{x, z});
        sender.sendMessage(this.config.message("pos1-set").replace("%x%", this.fmt(x)).replace("%z%", this.fmt(z)));
        return true;
    }

    private boolean handlePos2(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        double[] first = this.pendingCorners.remove(player.getUniqueId());
        if (first == null) {
            sender.sendMessage(this.config.message("pos1-missing"));
            return true;
        }
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        this.config.setOuter(first[0], first[1], x, z);
        this.config.setWorldName(player.getWorld().getName());
        this.config.save();
        sender.sendMessage(this.config.message("pos2-set").replace("%x%", this.fmt(x)).replace("%z%", this.fmt(z)));
        sender.sendMessage(this.config.message("outer-set"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "World set to " + String.valueOf(ChatColor.WHITE) + player.getWorld().getName() + String.valueOf(ChatColor.GRAY) + ". Next: stand on the island split and run " + String.valueOf(ChatColor.YELLOW) + "/border setdivider x" + String.valueOf(ChatColor.GRAY) + " (or z).");
        return true;
    }

    private boolean handleSetWorld(CommandSender sender, String[] args) {
        String worldName;
        if (args.length >= 2) {
            World world = Bukkit.getWorld((String)args[1]);
            if (world == null) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "No loaded world named \"" + args[1] + "\".");
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Loaded worlds: " + String.valueOf(ChatColor.WHITE) + this.loadedWorldNames());
                return true;
            }
            worldName = world.getName();
        } else {
            Player player = this.requirePlayer(sender);
            if (player == null) {
                return true;
            }
            worldName = player.getWorld().getName();
        }
        this.config.setWorldName(worldName);
        this.config.save();
        sender.sendMessage(this.config.message("world-set").replace("%world%", worldName));
        return true;
    }

    private boolean handleSetOuter(CommandSender sender, String[] args) {
        if (args.length != 5) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /border setouter <x1> <z1> <x2> <z2>");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Tip: /border pos1 and /border pos2 do this without typing numbers.");
            return true;
        }
        try {
            double x1 = Double.parseDouble(args[1]);
            double z1 = Double.parseDouble(args[2]);
            double x2 = Double.parseDouble(args[3]);
            double z2 = Double.parseDouble(args[4]);
            this.config.setOuter(x1, z1, x2, z2);
            this.config.save();
            sender.sendMessage(this.config.message("outer-set"));
        }
        catch (NumberFormatException e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Coordinates must be numbers.");
        }
        return true;
    }

    private boolean handleSetDivider(CommandSender sender, String[] args) {
        double coordinate;
        if (args.length < 2 || !args[1].equalsIgnoreCase("x") && !args[1].equalsIgnoreCase("z")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /border setdivider <x|z> [coordinate]");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Leave the coordinate out to use where you are standing.");
            return true;
        }
        boolean axisIsX = args[1].equalsIgnoreCase("x");
        if (args.length >= 3) {
            try {
                coordinate = Double.parseDouble(args[2]);
            }
            catch (NumberFormatException e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Coordinate must be a number.");
                return true;
            }
        } else {
            Player player = this.requirePlayer(sender);
            if (player == null) {
                return true;
            }
            coordinate = axisIsX ? player.getLocation().getX() : player.getLocation().getZ();
        }
        this.config.setDivider(args[1], coordinate);
        this.config.save();
        sender.sendMessage(this.config.message("divider-set").replace("%axis%", axisIsX ? "x" : "z").replace("%coord%", this.fmt(coordinate)));
        return true;
    }

    private boolean handleEnforceDivider(CommandSender sender, String[] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("true") && !args[1].equalsIgnoreCase("false")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /border enforcedivider <true|false>");
            return true;
        }
        boolean enforce = Boolean.parseBoolean(args[1]);
        this.config.setDividerEnforced(enforce);
        this.config.save();
        sender.sendMessage(this.config.message(enforce ? "divider-enforce-on" : "divider-enforce-off"));
        return true;
    }

    private boolean handleSwitchSide(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /border switchside <player|selector>");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Examples: /border switchside Steve, /border switchside @a");
            return true;
        }
        ArrayList<Player> targets = new ArrayList<Player>();
        if (args[1].startsWith("@")) {
            try {
                for (Entity entity : Bukkit.selectEntities(sender, args[1])) {
                    if (entity instanceof Player) {
                        targets.add((Player)entity);
                    }
                }
            }
            catch (IllegalArgumentException e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid selector: " + e.getMessage());
                return true;
            }
        } else {
            Player target = Bukkit.getPlayerExact((String)args[1]);
            if (target != null) {
                targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "No online players matched \"" + args[1] + "\".");
            return true;
        }
        int switched = 0;
        int skipped = 0;
        for (Player target : targets) {
            if (this.borderListener.switchSide(target)) {
                ++switched;
                target.sendMessage(String.valueOf(ChatColor.YELLOW) + "An administrator moved you to the other border side.");
                continue;
            }
            ++skipped;
        }
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Switched " + switched + " player" + (switched == 1 ? "" : "s") + " to the other side.");
        if (skipped > 0) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Skipped " + skipped + " player" + (skipped == 1 ? "" : "s") + " outside the border world or blocked from teleporting.");
        }
        return true;
    }

    private boolean handleLayers(CommandSender sender, String[] args) {
        int count;
        if (args.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /border layers <count> [gap] [distance]");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "count    = extra lines above AND below the main one (0-10, now " + String.valueOf(ChatColor.WHITE) + this.config.getParticleLayers() + String.valueOf(ChatColor.GRAY) + ")");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "gap      = blocks between the lines (now " + String.valueOf(ChatColor.WHITE) + this.fmt(this.config.getParticleLayerSpacing()) + String.valueOf(ChatColor.GRAY) + ")");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "distance = how close a player must be for them to appear (now " + String.valueOf(ChatColor.WHITE) + this.config.getParticleLayerDistance() + String.valueOf(ChatColor.GRAY) + ")");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Example: " + String.valueOf(ChatColor.YELLOW) + "/border layers 2 2 12");
            return true;
        }
        try {
            count = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "The layer count must be a whole number between 0 and 10.");
            return true;
        }
        if (count < 0 || count > 10) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "The layer count must be between 0 and 10.");
            return true;
        }
        this.config.setParticleLayers(count);
        if (args.length >= 3) {
            try {
                this.config.setParticleLayerSpacing(Double.parseDouble(args[2]));
            }
            catch (NumberFormatException e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "The gap must be a number, for example 2 or 1.5.");
                return true;
            }
        }
        if (args.length >= 4) {
            try {
                this.config.setParticleLayerDistance(Integer.parseInt(args[3]));
            }
            catch (NumberFormatException e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "The distance must be a whole number of blocks.");
                return true;
            }
        }
        this.config.save();
        sender.sendMessage(this.config.message("layers-set").replace("%count%", String.valueOf(this.config.getParticleLayers())).replace("%gap%", this.fmt(this.config.getParticleLayerSpacing())).replace("%distance%", String.valueOf(this.config.getParticleLayerDistance())));
        return true;
    }

    private boolean handleProtection(CommandSender sender, String[] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("true") && !args[1].equalsIgnoreCase("false")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /border protection <true|false>");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Currently: " + String.valueOf(ChatColor.WHITE) + this.config.isProtectionEnabled());
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "This blocks building, breaking, interacting, hitting, shooting, throwing, pistons and vehicles across the border.");
            return true;
        }
        boolean enabled = Boolean.parseBoolean(args[1]);
        this.config.setProtectionEnabled(enabled);
        this.config.save();
        sender.sendMessage(this.config.message(enabled ? "protection-on" : "protection-off"));
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player)sender;
        }
        sender.sendMessage(String.valueOf(ChatColor.RED) + "Only a player in-game can use that - it uses your position.");
        return null;
    }

    private String loadedWorldNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return String.join((CharSequence)", ", names);
    }

    private String fmt(double value) {
        return String.format("%.1f", value);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setdivider")) {
            completions.addAll(Arrays.asList("x", "z"));
            return completions;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("enforcedivider") || args[0].equalsIgnoreCase("protection"))) {
            completions.addAll(Arrays.asList("true", "false"));
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("switchside")) {
            for (String selector : Arrays.asList("@a", "@p", "@r", "@s")) {
                if (selector.startsWith(args[1].toLowerCase())) {
                    completions.add(selector);
                }
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("layers")) {
            completions.addAll(Arrays.asList("0", "1", "2", "3", "4"));
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("layers")) {
            completions.addAll(Arrays.asList("1", "1.5", "2", "3"));
            return completions;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("layers")) {
            completions.addAll(Arrays.asList("8", "12", "16", "24"));
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setworld")) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(world.getName());
                }
            }
        }
        return completions;
    }
}
