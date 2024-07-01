package com.altiran.dropstop;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.altiran.dropstop.utils.ProcessUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DropStop.
 */
public class TestPlugin {
    /**
     * WHETHER TO RUN A FULL TEST OR A PARTIAL TEST
     *
     * <p>Full tests check can test the plugin very thoroughly, which is not required most of the time.
     * Running a full test can be extremely slow, taking several days to complete.
     *
     * <p>We, at Altiran, run one of these periodically to ensure the functionality of the plugin.
     * But we never run full tests in a CI environment (e.g., GitHub Actions). Running a full test
     * requires a lot of processing power (we have seen up to 100% CPU usage while a full test was
     * in progress) and can slow down or even damage your computer significantly. The allocated
     * memory for the JVM should be increased to at least 16 GB to run a full test. We recommend
     * running a full test only on a powerful computer with a lot of memory. Only change this
     * value if you are sure about what you are doing.
     *
     * <p>Normally this is set to false, meaning that only a partial test will be run. A partial test
     * will test the plugin with only the most common cases and will complete in a reasonable amount
     * of time. Hence, it is safe to run a partial test in most environments.
     */
    private static final boolean FULL_TEST = false;

    private static final Logger LOGGER = LoggerFactory.getLogger("Tests");
    private static final StringBuilder CHARACTERS = new StringBuilder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static long timestamp;
    private List<Material> DROPPABLE_MATERIALS;
    private DropStop plugin;
    private ServerMock server;
    private PlayerMock player;

    /**
     * Load the plugin and server before running tests.
     */
    @BeforeAll
    public static void load() {
        // Set timestamp before initialization
        timestamp = System.nanoTime();

        // Add numbers
        CHARACTERS.append("0123456789");

        // Add uppercase letters
        CHARACTERS.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        // Add lowercase letters
        CHARACTERS.append("abcdefghijklmnopqrstuvwxyz");

        // Add special characters
        CHARACTERS.append("!@#$%^*()-_=+[{]};:'\",<.>/?`~");

        // Log the initialization time
        LOGGER.info("Tests initialized in {}. Beginning the tests now...", ProcessUtils.getTimeTaken(timestamp));
    }

    /**
     * Tear down the test environment after each test.
     */
    @AfterAll
    public static void unload() {
        LOGGER.info("Completed all tests in {}.", ProcessUtils.getTimeTaken(timestamp));
    }

    /**
     * Set up the test environment before each test.
     */
    @BeforeEach
    public void prepare() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DropStop.class);

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        player = server.addPlayer();

        DROPPABLE_MATERIALS = Arrays.stream(Material.values())
            .filter(material -> material.isItem() && material != Material.AIR && material != Material.LEGACY_AIR)
            .collect(Collectors.toList());

        LOGGER.info("The current test can use {} droppable materials out of a total of {}.", DROPPABLE_MATERIALS.size(), Material.values().length);
    }

    /**
     * Tear down the test environment after each test.
     */
    @AfterEach
    public void dispose() {
        server.getScheduler().cancelTasks(plugin);
        MockBukkit.unmock();
    }

    /**
     * Generate a random text message.
     */
    private String getWarningMessageText() {
        return CHARACTERS.toString();
    }

    /**
     * Test a given combination of items.
     */
    private void testGivenCombination(int bitmask, int n, boolean shouldAllow) {
        List<String> allowlist = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((bitmask & (1 << i)) != 0) {
                allowlist.add(DROPPABLE_MATERIALS.get(i).name());
            }
        }

        plugin.getConfig().set("item-allowlist", allowlist);

        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
            server.getPluginManager().callEvent(event);

            if (shouldAllow) {
                if (allowlist.contains(material.name())) {
                    assertFalse(event.isCancelled(), "Item " + material.name() + " should be allowed to drop");
                } else {
                    assertTrue(event.isCancelled(), "Item " + material.name() + " should not be allowed to drop");
                }
            } else {
                if (!allowlist.contains(material.name())) {
                    assertTrue(event.isCancelled(), "Item " + material.name() + " should not be allowed to drop");
                } else {
                    assertFalse(event.isCancelled(), "Item " + material.name() + " should be allowed to drop");
                }
            }
        }

        System.gc(); // Free up memory
    }

    /**
     * Perform a drop test with a given timeout period.
     */
    private void performDropTestWithTimeout(int timeout) throws InterruptedException {
        player = server.addPlayer(); // Reset the player before each test

        String message = getWarningMessageText();
        plugin.getConfig().set("warning-message", message);
        plugin.getConfig().set("warning-timeout", timeout);

        ItemStack item = new ItemStack(DROPPABLE_MATERIALS.get(RANDOM.nextInt(DROPPABLE_MATERIALS.size())));
        player.getInventory().setItemInMainHand(item);

        // Drop an item for the first time
        PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(message, player.nextMessage());

        // Drop an item within the timeout period
        event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        String nextMessage = player.nextMessage();

        if (nextMessage != null) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected chat message: {0}", nextMessage);
        }

        assertNull(nextMessage); // No new message should be sent

        // Wait for the timeout period to pass and one more second
        Thread.sleep((timeout + 1) * 1000L);

        // Drop an item after the timeout period
        event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(message, player.nextMessage()); // A new message should be sent
    }

    @Test
    @DisplayName("Verify that we are in a test environment")
    void verifyTestEnvironment() {
        assertTrue(plugin.isUnitTest());
    }

    @Test
    @DisplayName("Verify that the plugin starts correctly")
    void testPluginStarts() {
        assertTrue(plugin.isEnabled());
    }

    @Test
    @DisplayName("Verify that the plugin stops correctly")
    void testOnDisable() {
        plugin.onDisable();
        assertTrue(plugin.isDisabled());
    }

    @Test
    @DisplayName("Verify that items cannot be dropped when configured true")
    void testItemDropPrevention() {
        plugin.getConfig().set("disable-item-drops", true);
        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
            server.getPluginManager().callEvent(event);

            assertTrue(event.isCancelled());
        }
    }

    @Test
    @DisplayName("Verify that items can be dropped when configured false")
    void testItemDropAllowed() {
        plugin.getConfig().set("disable-item-drops", false);
        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
            server.getPluginManager().callEvent(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    @DisplayName("Verify that items can be dropped when configuration is not set")
    void testNoConfiguration() {
        plugin.getConfig().set("disable-item-drops", null); // Set to null

        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
            server.getPluginManager().callEvent(event);

            assertFalse(event.isCancelled());
        }
    }

    @Test
    @DisplayName("Verify that warning message is sent when item is dropped and configuration is set")
    void testWarningMessage() {
        plugin.getConfig().set("disable-item-drops", true);
        plugin.getConfig().set("warn-player-on-drop", true);

        for (Material material : DROPPABLE_MATERIALS) {
            String message = getWarningMessageText();
            plugin.getConfig().set("warning-message", message);

            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
            server.getPluginManager().callEvent(event);

            assertTrue(event.isCancelled());
            assertEquals(message, player.nextMessage());

            plugin.resetPlayerMessageTimestamps();
        }
    }

    @Test
    @DisplayName("Verify that items on the allowlist can be dropped")
    void testItemAllowlisting() {
        plugin.getConfig().set("disable-item-drops", true);
        plugin.getConfig().set("item-allowlisting", true);

        if (FULL_TEST) {
            // Full testing: consider all combinations of items
            int n = DROPPABLE_MATERIALS.size();
            for (int bitmask = 0; bitmask < (1 << n); bitmask++) {
                testGivenCombination(bitmask, n, true);
            }
        } else {
            // Partial testing: consider only the most common cases

            // Case 1: Allowlist has all items
            testGivenCombination((1 << DROPPABLE_MATERIALS.size()) - 1, DROPPABLE_MATERIALS.size(), true);

            // Case 2: Allowlist has no items
            testGivenCombination(0, DROPPABLE_MATERIALS.size(), true);

            // Case 3: Allowlist has a random half of the items
            List<Material> shuffledMaterials = new ArrayList<>(DROPPABLE_MATERIALS);
            Collections.shuffle(shuffledMaterials);
            int bitmask = 0;
            for (int i = 0; i < shuffledMaterials.size() / 2; i++) {
                bitmask |= 1 << DROPPABLE_MATERIALS.indexOf(shuffledMaterials.get(i));
            }
            testGivenCombination(bitmask, DROPPABLE_MATERIALS.size(), true);

            // Case 4: Allowlist has a random single item
            testGivenCombination(1 << RANDOM.nextInt(DROPPABLE_MATERIALS.size()), DROPPABLE_MATERIALS.size(), true);

            // Case 5: Allowlist has multiple random items
            bitmask = 0;
            for (int i = 0; i < RANDOM.nextInt(DROPPABLE_MATERIALS.size()); i++) {
                bitmask |= 1 << RANDOM.nextInt(DROPPABLE_MATERIALS.size());
            }
            testGivenCombination(bitmask, DROPPABLE_MATERIALS.size(), true);
        }
    }

    @Test
    @DisplayName("Verify that items not on the allowlist cannot be dropped")
    void testItemNotAllowlisted() {
        plugin.getConfig().set("disable-item-drops", true);
        plugin.getConfig().set("item-allowlisting", true);

        if (FULL_TEST) {
            // Full testing: consider all combinations of items
            int n = DROPPABLE_MATERIALS.size();
            for (int bitmask = 0; bitmask < (1 << n); bitmask++) {
                testGivenCombination(bitmask, n, false);
            }
        } else {
            // Partial testing: consider only the most common cases

            // Case 1: Allowlist has all items
            testGivenCombination((1 << DROPPABLE_MATERIALS.size()) - 1, DROPPABLE_MATERIALS.size(), false);

            // Case 2: Allowlist has no items
            testGivenCombination(0, DROPPABLE_MATERIALS.size(), false);

            // Case 3: Allowlist has a random half of the items
            List<Material> shuffledMaterials = new ArrayList<>(DROPPABLE_MATERIALS);
            Collections.shuffle(shuffledMaterials);
            int bitmask = 0;
            for (int i = 0; i < shuffledMaterials.size() / 2; i++) {
                bitmask |= 1 << DROPPABLE_MATERIALS.indexOf(shuffledMaterials.get(i));
            }
            testGivenCombination(bitmask, DROPPABLE_MATERIALS.size(), false);

            // Case 4: Allowlist has a random single item
            testGivenCombination(1 << RANDOM.nextInt(DROPPABLE_MATERIALS.size()), DROPPABLE_MATERIALS.size(), false);

            // Case 5: Allowlist has multiple random items
            bitmask = 0;
            for (int i = 0; i < RANDOM.nextInt(DROPPABLE_MATERIALS.size()); i++) {
                bitmask |= 1 << RANDOM.nextInt(DROPPABLE_MATERIALS.size());
            }
            testGivenCombination(bitmask, DROPPABLE_MATERIALS.size(), false);
        }
    }

    @Test
    @DisplayName("Verify that null items cannot be dropped")
    void testNullItemDrop() {
        plugin.getConfig().set("disable-item-drops", true);
        player.getInventory().setItemInMainHand(null);

        assertThrows(NullPointerException.class, () -> {
            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), null));
            server.getPluginManager().callEvent(event);
        });
    }

    @Test
    @DisplayName("Verify that no warning message is sent when warning message is null")
    void testNullWarningMessage() {
        plugin.getConfig().set("disable-item-drops", true);
        plugin.getConfig().set("warn-player-on-drop", true);
        // Set the warning message to empty instead of null, because it is a file configuration.
        // Otherwise, null will be treated as a string.
        plugin.getConfig().set("warning-message", "");

        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            assertThrows(NullPointerException.class, () -> {
                PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
                server.getPluginManager().callEvent(event);
            });

            assertNull(player.nextMessage());
        }
    }

    @Test
    @DisplayName("Verify that items cannot be dropped by null player")
    void testNullPlayerDrop() {
        plugin.getConfig().set("disable-item-drops", true);
        player = null;

        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            WorldMock world = server.addSimpleWorld(getWarningMessageText());

            assertThrows(NullPointerException.class, () -> {
                PlayerDropItemEvent event = new PlayerDropItemEvent(player, world.dropItem(new Location(world, 0, 0, 0), item));
                server.getPluginManager().callEvent(event);
            });
        }
    }

    @Test
    @DisplayName("Verify that warning message is not sent repeatedly within the timeout period")
    void testWarningMessageTimeout() throws InterruptedException {
        plugin.getConfig().set("disable-item-drops", true);
        plugin.getConfig().set("warn-player-on-drop", true);

        // Test for timeouts from 1 to 15 seconds
        for (int timeout = 1; timeout <= 15; timeout++) {
            performDropTestWithTimeout(timeout);
        }

        // Test for a random timeout between 15 and 30 seconds
        performDropTestWithTimeout(RANDOM.nextInt(15) + 15);

        // Test for a random timeout between 30 and 120 seconds
        performDropTestWithTimeout(RANDOM.nextInt(90) + 30);
    }

    @Test
    @DisplayName("Measure how long it takes to process an item drop event")
    void testItemDropEventProcessingTime() {
        long totalDuration = 0;
        int itemCount = 0;

        for (Material material : DROPPABLE_MATERIALS) {
            ItemStack item = new ItemStack(material);
            player.getInventory().setItemInMainHand(item);

            long startTime = System.nanoTime();
            PlayerDropItemEvent event = new PlayerDropItemEvent(player, player.getWorld().dropItem(player.getLocation(), item));
            server.getPluginManager().callEvent(event);
            long endTime = System.nanoTime();

            long duration = (endTime - startTime);
            totalDuration += duration;
            itemCount++;
        }

        long averageDuration = totalDuration / itemCount;
        float ms = averageDuration / 1000000F;
        String performanceRating;

        if (ms <= 1) {
            performanceRating = "Excellent";
        } else if (ms <= 10) {
            performanceRating = "Good";
        } else if (ms <= 100) {
            performanceRating = "Average";
        } else {
            performanceRating = "Bad";
        }

        LOGGER.info("Average time taken to process an item drop: {} ms", ms);
        LOGGER.info("Performance score of the plugin: {}", performanceRating.toUpperCase());
        assertTrue(ms <= 1000); // The average time should be less than 1 second
    }
}
