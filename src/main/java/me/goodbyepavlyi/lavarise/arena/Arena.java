package me.goodbyepavlyi.lavarise.arena;

import me.goodbyepavlyi.lavarise.arena.utils.ArenaConfig;
import me.goodbyepavlyi.lavarise.game.Game;
import me.goodbyepavlyi.lavarise.arena.models.ArenaPlayer;
import me.goodbyepavlyi.lavarise.queue.Queue;
import me.goodbyepavlyi.lavarise.utils.ChatUtils;
import me.goodbyepavlyi.lavarise.utils.Logger;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Consumer;

public class Arena {
    private final ArenaManager arenaManager;
    private final String name;
    private final Set<ArenaPlayer> players;
    private final ArenaConfig config;
    private Game game;
    private Queue queue;
    private State state;
    private final List<BukkitTask> tasks;

    public enum State {
        WAITING, STARTING, IN_GAME
    }

    public enum ArenaSetupResult {
        NO_MINIMUM_PLAYERS("Minimum players not set"),
        NO_MAXIMUM_PLAYERS("Maximum players not set"),
        NO_LOBBY("Lobby not set"),
        NO_GAME_AREA_TOP("Game area top not set"),
        NO_GAME_AREA_BOTTOM("Game area bottom not set");

        private final String message;

        ArenaSetupResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }
    }

    public enum AnnouncementType {
        QUEUE_PLAYERJOIN,
        QUEUE_PLAYERLEAVE,
        QUEUE_CANCELLED,
        QUEUE_GAME_STARTING_IN,
        GAME_START,
        GAME_END,
        PLAYER_DEATH,
        PLAYER_KILLED
    }

    public Arena(ArenaManager arenaManager, String name) {
        this.arenaManager = arenaManager;
        this.name = name;
        this.state = State.WAITING;

        this.config = new ArenaConfig(this);
        this.game = new Game(this, this.arenaManager.getInstance());
        this.queue = new Queue(this);

        this.players = new HashSet<>();
        this.tasks = new ArrayList<>();
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    public String getName() {
        return this.name;
    }

    public Set<ArenaPlayer> getPlayers() {
        return this.players;
    }

    public ArenaPlayer getPlayer(UUID playerUUID) {
        return this.players.stream()
                .filter(arenaPlayer -> arenaPlayer.getPlayerUUID().equals(playerUUID))
                .findFirst()
                .orElse(null);
    }

    public ArenaConfig getConfig() {
        return this.config;
    }

    public Queue getQueue() {
        return this.queue;
    }

    public Game getGame() {
        return this.game;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public List<BukkitTask> getTasks() {
        return this.tasks;
    }

    public void delayFireTicks(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setFireTicks(0);
            }
        }.runTaskLater(this.arenaManager.getInstance(), 2L);
    }

    public void removePlayer(Player player, boolean dontRemove) {
        ArenaPlayer arenaPlayer = this.getPlayer(player.getUniqueId());
        if (arenaPlayer == null) return;

        arenaPlayer.restoreData();
        this.delayFireTicks(player);

        if (!dontRemove) {
            if (this.getState().equals(Arena.State.IN_GAME)) this.getGame().removePlayer(arenaPlayer);
            else this.getQueue().removePlayer(player);
        }

        // Clear the player's scoreboard
        player.setScoreboard(this.arenaManager.getInstance().getServer().getScoreboardManager().getMainScoreboard());
    }

    public void removePlayer(Player player) {
        this.removePlayer(player, false);
    }

    public void announceMessage(AnnouncementType announcementType, String... arguments) {
        String message = null;

        switch (announcementType) {
            case QUEUE_PLAYERJOIN:
                if (arguments.length < 1) break;

                message = arenaManager.getInstance().getMessages().QUEUE_PLAYERJOIN(
                        arguments[0],
                        this.getPlayers().size(),
                        this.getConfig().getMaximumPlayers()
                );
                break;
            case QUEUE_PLAYERLEAVE:
                if (arguments.length < 1) break;

                message = arenaManager.getInstance().getMessages().QUEUE_PLAYERLEAVE(
                        arguments[0],
                        this.getPlayers().size()-1, // Decrease by 1 since the player is being removed later
                        this.getConfig().getMaximumPlayers()
                );
                break;
            case QUEUE_CANCELLED:
                message = arenaManager.getInstance().getMessages().QUEUE_CANCELLED();
                break;
            case QUEUE_GAME_STARTING_IN:
                if (arguments.length < 1) break;
                message = arenaManager.getInstance().getMessages().QUEUE_GAMESTARTINGIN(Integer.parseInt(arguments[0]));
                break;
            case GAME_START:
                message = arenaManager.getInstance().getMessages().GAME_EVENTS_GAMESTART();
                break;
            case GAME_END:
                message = arenaManager.getInstance().getMessages().GAME_EVENTS_GAMEENDED();
                break;
            case PLAYER_DEATH:
                if (arguments.length < 1) break;
                message = arenaManager.getInstance().getMessages().GAME_EVENTS_PLAYERDEATH(arguments[0]);
                break;
            case PLAYER_KILLED:
                if (arguments.length < 2) break;
                message = arenaManager.getInstance().getMessages().GAME_EVENTS_PLAYERKILLED(arguments[0], arguments[1]);
                break;
        }

        if (message != null)
            this.sendMessage(message);
    }

    private void sendMessage(String message) {
        this.doForAllPlayers(player -> player.sendMessage(ChatUtils.color(message)));
    }

    public void doForAllPlayers(Consumer<Player> callback) {
        Logger.debug(String.format("Running callback for all Players (%d) in arena %s", this.getPlayers().size(), this.name));
        this.getPlayers().stream()
            .filter(Objects::nonNull)
            .map(ArenaPlayer::getPlayer)
            .forEach(callback);
    }

    public Set<ArenaSetupResult> checkSetup() {
        Set<ArenaSetupResult> results = new HashSet<>();

        if (this.config.getMinimumPlayers() == 0) results.add(ArenaSetupResult.NO_MINIMUM_PLAYERS);
        if (this.config.getMaximumPlayers() == 0) results.add(ArenaSetupResult.NO_MAXIMUM_PLAYERS);
        if (this.config.getLobby() == null) results.add(ArenaSetupResult.NO_LOBBY);
        if (this.config.getGameArea(ArenaConfig.GameArea.TOP) == null) results.add(ArenaSetupResult.NO_GAME_AREA_TOP);
        if (this.config.getGameArea(ArenaConfig.GameArea.BOTTOM) == null) results.add(ArenaSetupResult.NO_GAME_AREA_BOTTOM);

        if (!results.isEmpty()) {
            results.forEach(result -> Logger.debug(String.format("Arena %s is missing %s", this.name, result.getMessage())));
        }

        return results;
    }

    public void reset() {
        Logger.debug(String.format("Resetting arena %s", this.name));
        this.players.clear();

        this.game = new Game(this, this.arenaManager.getInstance());
        this.queue = new Queue(this);
        this.setState(State.WAITING);

        Logger.debug(String.format("Clearing tasks (%d) for arena %s", this.getTasks().size(), this.name));
        this.getTasks().forEach(BukkitTask::cancel);
        this.getTasks().clear();
    }
}