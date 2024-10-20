package me.goodbyepavlyi.lavarise.arena.utils;

import me.goodbyepavlyi.lavarise.arena.Arena;
import me.goodbyepavlyi.lavarise.utils.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class ArenaConfig {
    private final Arena arena;
    private final ConfigurationSection config;

    public enum GameArea {
        TOP("Top"),
        BOTTOM("Bottom");

        private final String name;

        GameArea(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public ArenaConfig(Arena arena) {
        this.arena = arena;
        if (this.arena.getArenaManager().getStorageConfig().getSection(this.arena.getName()) == null) {
            this.arena.getArenaManager().getStorageConfig().createSection(this.arena.getName());
            Logger.debug(String.format("Created config section for arena %s", arena.getName()));
        }

        this.config = this.arena.getArenaManager().getStorageConfig().getSection(this.arena.getName());
        Logger.debug(String.format("Initialized options for arena %s", arena.getName()));
    }

    public void saveConfig() {
        this.arena.getArenaManager().getStorageConfig().getConfig().createSection(this.arena.getName());
        this.arena.getArenaManager().getStorageConfig().getConfig().set(this.arena.getName(), this.config);
        Logger.debug(String.format("Saved configuration for arena %s", this.arena.getName()));
    }

    public void removeConfig() {
        this.arena.getArenaManager().getStorageConfig().getConfig().set(this.arena.getName(), null);
        Logger.debug(String.format("Removed configuration for arena %s", this.arena.getName()));
    }

    public void setMinimumPlayers(int minimumPlayers) {
        this.config.set("minimumPlayers", minimumPlayers);
    }

    public int getMinimumPlayers() {
        return this.config.getInt("minimumPlayers");
    }

    public void setMaximumPlayers(int maximumPlayers) {
        this.config.set("maximumPlayers", maximumPlayers);
    }

    public int getMaximumPlayers() {
        return this.config.getInt("maximumPlayers");
    }

    public void setLobby(Location lobby) {
        this.config.set("lobby.world", lobby.getWorld().getName());
        this.config.set("lobby.x", lobby.getBlockX());
        this.config.set("lobby.y", lobby.getBlockY());
        this.config.set("lobby.z", lobby.getBlockZ());
        this.config.set("lobby.yaw", lobby.getYaw());
        this.config.set("lobby.pitch", lobby.getPitch());
    }

    public Location getLobby() {
        String worldName = this.config.getString("lobby.world");
        if (worldName == null) return null;

        World world = this.arena.getArenaManager().getInstance().getServer().getWorld(worldName);
        int blockX = this.config.getInt("lobby.x");
        int blockY = this.config.getInt("lobby.y");
        int blockZ = this.config.getInt("lobby.z");
        float yaw = this.config.getInt("lobby.yaw");
        float pitch = this.config.getInt("lobby.pitch");

        return new Location(world, blockX, blockY, blockZ, yaw, pitch);
    }

    public void setGameAreaLocation(GameArea gameArea, Location gameAreaLocation) {
        this.config.set("gameArea.world", gameAreaLocation.getWorld().getName());
        this.config.set("gameArea." + gameArea + ".x", gameAreaLocation.getBlockX());
        this.config.set("gameArea." + gameArea + ".y", gameAreaLocation.getBlockY());
        this.config.set("gameArea." + gameArea + ".z", gameAreaLocation.getBlockZ());
    }

    public World getGameAreaWorld() {
        String worldName = this.config.getString("gameArea.world");
        if (worldName == null) return null;

        return this.arena.getArenaManager().getInstance().getServer().getWorld(worldName);
    }

    public Location getGameArea(GameArea gameArea) {
        World world = this.getGameAreaWorld();
        int blockX = this.config.getInt("gameArea." + gameArea + ".x");
        int blockY = this.config.getInt("gameArea." + gameArea + ".y");
        int blockZ = this.config.getInt("gameArea." + gameArea + ".z");

        return new Location(world, blockX, blockY, blockZ);
    }
}