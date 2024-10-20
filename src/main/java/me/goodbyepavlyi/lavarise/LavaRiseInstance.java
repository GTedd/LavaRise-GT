package me.goodbyepavlyi.lavarise;

import me.goodbyepavlyi.lavarise.arena.Arena;
import me.goodbyepavlyi.lavarise.arena.ArenaManager;
import me.goodbyepavlyi.lavarise.commands.lavarise.LavaRiseCommand;
import me.goodbyepavlyi.lavarise.game.listeners.GameEventListener;
import me.goodbyepavlyi.lavarise.configs.Config;
import me.goodbyepavlyi.lavarise.configs.Messages;
import me.goodbyepavlyi.lavarise.game.listeners.GameGracePhaseEventListener;
import me.goodbyepavlyi.lavarise.queue.listeners.QueueEventListener;
import me.goodbyepavlyi.lavarise.utils.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;

public class LavaRiseInstance extends JavaPlugin {
    private final boolean DEBUG;

    private Config config;
    private Messages messages;
    private ArenaManager arenaManager;

    public LavaRiseInstance() {
        this.DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
    }

    @Override
    public void onEnable() {
        new Logger(this, this.DEBUG);

        this.config = new Config(this);
        this.messages = new Messages(this);
        this.arenaManager = new ArenaManager(this);

        this.getServer().getPluginManager().registerEvents(new QueueEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GameEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GameGracePhaseEventListener(this), this);
        this.getCommand("lavarise").setExecutor(new LavaRiseCommand(this));
        this.getCommand("lavarise").setTabCompleter(new LavaRiseCommand(this));

        if (this.config.Metrics() && !this.DEBUG) {
            Logger.debug("Enabling bStats metrics");
            new Metrics(this, 23679);
        } else Logger.debug("bStats metrics are disabled, skipping");
    }

    @Override
    public void onDisable() {
        this.arenaManager.getArenaList().forEach(arena -> {
            if (!arena.getState().equals(Arena.State.IN_GAME)) return;

            Logger.debug(String.format("Stopping game in arena %s", arena.getName()));
            arena.getGame().stop();
        });

        this.arenaManager.saveArenas();
    }

    public Config getConfiguration() {
        return this.config;
    }

    public Messages getMessages() {
        return this.messages;
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }
}