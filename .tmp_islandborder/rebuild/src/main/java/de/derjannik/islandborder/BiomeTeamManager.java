package de.derjannik.islandborder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class BiomeTeamManager {
    public enum BiomeTeam {
        ADMIN("admin"),
        DESERT("desert"),
        PLAINS("plains"),
        NONE("");

        private final String configKey;

        BiomeTeam(String configKey) {
            this.configKey = configKey;
        }

        public String configKey() {
            return this.configKey;
        }
    }

    private final IslandBorderPlugin plugin;
    private Team adminTeam;
    private Team desertTeam;
    private Team plainsTeam;
    private Location desertStartLocation;
    private Location plainsStartLocation;

    public BiomeTeamManager(IslandBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = this.plugin.getConfig();
        this.desertStartLocation = this.loadLocation(config, "desert-start");
        this.plainsStartLocation = this.loadLocation(config, "plains-start");
        this.setupTeams(config);
    }

    public void applyScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        player.setScoreboard(scoreboard);
    }

    public BiomeTeam getTeam(String playerName) {
        if (this.adminTeam.hasEntry(playerName)) {
            return BiomeTeam.ADMIN;
        }
        if (this.desertTeam.hasEntry(playerName)) {
            return BiomeTeam.DESERT;
        }
        if (this.plainsTeam.hasEntry(playerName)) {
            return BiomeTeam.PLAINS;
        }
        return BiomeTeam.NONE;
    }

    public Boolean getAssignedPlainsSide(String playerName) {
        return switch (this.getTeam(playerName)) {
            case PLAINS -> Boolean.TRUE;
            case DESERT -> Boolean.FALSE;
            default -> null;
        };
    }

    public void assign(String playerName, BiomeTeam team) {
        this.adminTeam.removeEntry(playerName);
        this.desertTeam.removeEntry(playerName);
        this.plainsTeam.removeEntry(playerName);
        switch (team) {
            case ADMIN -> this.adminTeam.addEntry(playerName);
            case DESERT -> this.desertTeam.addEntry(playerName);
            case PLAINS -> this.plainsTeam.addEntry(playerName);
            case NONE -> {
            }
        }
        this.saveTeam(playerName, team);
    }

    public Location getStartLocation(BiomeTeam team) {
        if (team == BiomeTeam.PLAINS) {
            return this.plainsStartLocation.clone();
        }
        return this.desertStartLocation.clone();
    }

    public void setStartLocation(BiomeTeam team, Location location) {
        if (team == BiomeTeam.PLAINS) {
            this.plainsStartLocation = location.clone();
            this.saveLocation("plains-start", location);
        } else if (team == BiomeTeam.DESERT) {
            this.desertStartLocation = location.clone();
            this.saveLocation("desert-start", location);
        }
    }

    private Location loadLocation(FileConfiguration config, String path) {
        String worldName = config.getString(path + ".world", this.plugin.getBorderConfig().getWorldName());
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().getFirst();
        }
        return new Location(
            world,
            config.getDouble(path + ".x", 0.0),
            config.getDouble(path + ".y", 64.0),
            config.getDouble(path + ".z", 0.0),
            (float)config.getDouble(path + ".yaw", 0.0),
            (float)config.getDouble(path + ".pitch", 0.0)
        );
    }

    private void setupTeams(FileConfiguration config) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.adminTeam = this.prepareTeam(scoreboard, "admin");
        this.desertTeam = this.prepareTeam(scoreboard, "desert");
        this.plainsTeam = this.prepareTeam(scoreboard, "plains");
        this.adminTeam.color(NamedTextColor.DARK_RED);
        this.adminTeam.prefix(Component.text("[Admin] ", NamedTextColor.DARK_RED));
        this.desertTeam.color(NamedTextColor.GOLD);
        this.plainsTeam.color(NamedTextColor.GREEN);
        this.loadEntries(this.adminTeam, config.getStringList("teams.admin"));
        this.loadEntries(this.desertTeam, config.getStringList("teams.desert"));
        this.loadEntries(this.plainsTeam, config.getStringList("teams.plains"));
    }

    private Team prepareTeam(Scoreboard scoreboard, String name) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            return scoreboard.registerNewTeam(name);
        }
        for (String entry : new HashSet<String>(team.getEntries())) {
            team.removeEntry(entry);
        }
        return team;
    }

    private void loadEntries(Team team, List<String> entries) {
        for (String entry : entries) {
            team.addEntry(entry);
        }
    }

    private void saveTeam(String playerName, BiomeTeam team) {
        FileConfiguration config = this.plugin.getConfig();
        for (BiomeTeam existingTeam : List.of(BiomeTeam.ADMIN, BiomeTeam.DESERT, BiomeTeam.PLAINS)) {
            String path = "teams." + existingTeam.configKey();
            ArrayList<String> entries = new ArrayList<String>(config.getStringList(path));
            entries.removeIf(entry -> entry.equalsIgnoreCase(playerName));
            config.set(path, entries);
        }
        if (team != BiomeTeam.NONE) {
            String path = "teams." + team.configKey();
            ArrayList<String> entries = new ArrayList<String>(config.getStringList(path));
            entries.add(playerName);
            config.set(path, entries);
        }
        this.plugin.saveConfig();
    }

    private void saveLocation(String path, Location location) {
        FileConfiguration config = this.plugin.getConfig();
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        this.plugin.saveConfig();
    }
}
