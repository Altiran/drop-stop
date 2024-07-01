package com.altiran.dropstop;

import com.altiran.dropstop.utils.ProcessUtils;
import io.papermc.lib.PaperLib;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main class of the plugin.
 */
public class DropStop extends JavaPlugin implements Listener {
    private static final String MC_CHAR = String.valueOf((char) 167);
    private static DropStop instance;
    private final Map<String, Long> playerMessageTimestamps = new HashMap<>();
    private boolean unitTestEnv = false;
    private boolean isDisabled = false;

    /**
     * Constructor for the DropStop class.
     */
    public DropStop() {
        super();

        if (getClassLoader().getClass().getPackageName().startsWith("be.seeseemelk.mockbukkit")) { // Check if the class loader is from MockBukkit
            unitTestEnv = true;
        }
    }

    private static void setInstance(@Nullable DropStop pluginInstance) {
        instance = pluginInstance;
    }

    public static @Nullable DropStop instance() {
        return instance;
    }

    private static void validateInstance() {
        if (instance == null) {
            throw new IllegalStateException("Cannot invoke static method, DropStop instance is null.");
        }
    }

    public static @Nonnull Logger logger() {
        validateInstance();
        return instance.getLogger();
    }

    /**
     * Initializes the plugin, and it's configurations.
     */
    @Override
    public void onEnable() {
        setInstance(this);

        long timestamp = System.nanoTime();
        Logger logger = getLogger();

        if (!unitTestEnv) {
            if (PaperLib.isPaper()) {
                logger.log(Level.INFO, "PaperMC was detected! Performance optimizations have been applied.");
            } else {
                PaperLib.suggestPaper(this);
            }
        }

        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        logger.log(Level.INFO, "DropStop initialized in {0}. No players can drop items!", ProcessUtils.getTimeTaken(timestamp));
    }

    /**
     * Log a message when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (instance() == null || !isEnabled()) {
            return;
        }

        logger().info("Stopping DropStop...");
        setInstance(null);
        isDisabled = true;
    }

    public boolean isDisabled() {
        return isDisabled;
    }


    public boolean isUnitTest() {
        return unitTestEnv;
    }

    /**
     * Handles the event when a player tries to drop an item.
     *
     * @param e The event object.
     */
    @EventHandler
    public final void onPlayerItemDrop(PlayerDropItemEvent e) {
        if (getConfig().getBoolean("disable-item-drops", false)) {
            if (getConfig().getBoolean("item-allowlisting", false)) {
                if (!getConfig().getStringList("item-allowlist").contains(e.getItemDrop().getItemStack().getType().toString())) {
                    cancelItemDrop(e);
                }
            } else {
                cancelItemDrop(e);
            }
        }
    }

    private void cancelItemDrop(@Nonnull PlayerDropItemEvent e) {
        Player p = validatePlayerObject(e.getPlayer());
        e.setCancelled(true);

        if (getConfig().getBoolean("warn-player-on-drop")) {
            sendWarningMessage(p);
        }
    }

    /**
     * Reset the playerMessageTimestamps map, has no effect outside a unit test environment.
     */
    public void resetPlayerMessageTimestamps() {
        if (unitTestEnv) {
            playerMessageTimestamps.clear();
        }
    }

    private void sendWarningMessage(@Nonnull Player p) {
        long timeout = getConfig().getInt("warning-timeout") * 1000000000L; // Convert seconds to nanoseconds
        long buffer = 500000000L; // 0.5s buffer
        long now = System.nanoTime();
        Long lastMessageTime = playerMessageTimestamps.get(p.getName());

        if (lastMessageTime == null || now - lastMessageTime >= timeout + buffer) {
            p.sendMessage(formatChatMessage(validateWarningMessage(), p.getName()));
            playerMessageTimestamps.put(p.getName(), now);
        }
    }

    private String formatChatMessage(String str, String p) {
        return str.replaceAll("&", MC_CHAR).replaceAll("%player%", p);
    }

    private @Nonnull String validateWarningMessage() {
        @Nullable String msg = getConfig().getString("warning-message");
        if (msg == null || msg.isEmpty()) {
            throw new NullPointerException("The warn message you provided is not valid. If you don't want a warn message, please set 'warn-player-on-drop' to false. Or else if you want it, please provide a valid message and don't leave it blank.");
        }
        return msg;
    }

    private @Nonnull Player validatePlayerObject(@Nullable Player obj) {
        if (obj == null) {
            throw new NullPointerException("Player object cannot be null. Invalid event object.");
        }
        return obj;
    }
}
