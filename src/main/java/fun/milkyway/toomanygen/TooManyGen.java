package fun.milkyway.toomanygen;

import com.comphenix.protocol.ProtocolLibrary;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class TooManyGen extends JavaPlugin {
    private static TooManyGen instance;
    private ConfigurationManager configurationManager;
    private Map<String, ChunkGenerationListener> listeners;
    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;
        listeners = new HashMap<>();

        configurationManager = ConfigurationManager.getInstance();

        var worlds = configurationManager.getActiveWorlds();

        for (var world : worlds) {
            var serverWorld = getServer().getWorld(world);
            if (serverWorld == null) {
                getLogger().warning("Could not find world " + world);
                continue;
            }
            var listener = new ChunkGenerationListener(serverWorld);
            getServer().getPluginManager().registerEvents(listener, this);
            listeners.put(world, listener);
        }

        var command = getCommand("toomanygen");

        if (command == null) {
            throw new IllegalStateException("Could not find core command toomanygen!");
        }

        command.setExecutor(new TooManyGenCommand());

        reloadBstats();

        initProtocollib();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void onReload() {
        configurationManager.loadConfiguration();
        var worlds = configurationManager.getActiveWorlds();
        var iter = listeners.entrySet().iterator();
        while (iter.hasNext()) {
            var listener = iter.next();
            if (worlds.contains(listener.getKey())) {
                continue;
            }
            listener.getValue().shutdown();
            HandlerList.unregisterAll(listener.getValue());
            iter.remove();
        }
        for (var worldName : worlds) {
            if (listeners.containsKey(worldName)) {
                continue;
            }
            var world = getServer().getWorld(worldName);
            if (world == null) {
                getLogger().warning("Could not find world " + worldName);
                continue;
            }
            var listener = new ChunkGenerationListener(world);
            getServer().getPluginManager().registerEvents(listener, this);
            listeners.put(worldName, listener);
        }
        reloadBstats();
    }

    private void reloadBstats() {
        if (!configurationManager.getConfiguration().getBoolean("bStats", true) && metrics == null) {
            return;
        }
        if (!configurationManager.getConfiguration().getBoolean("bStats", true) && metrics != null) {
            getLogger().info("Disabling bStats");
            metrics.shutdown();
            metrics = null;
            return;
        }
        if (metrics != null) {
            return;
        }
        getLogger().info("Enabling bStats");
        metrics = new Metrics(this, 19161);
    }

    private void initProtocollib() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().info("Could not find ProtocolLib, add it to your server to remove flickering");
            return;
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener());
        getLogger().info("Found ProtocolLib, packet listener enabled");
    }

    public static TooManyGen getInstance() {
        return instance;
    }
}
