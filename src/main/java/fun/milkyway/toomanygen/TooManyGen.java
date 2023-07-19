package fun.milkyway.toomanygen;

import org.bukkit.plugin.java.JavaPlugin;

public final class TooManyGen extends JavaPlugin {
    private static TooManyGen instance;
    private ConfigurationManager configurationManager;

    @Override
    public void onEnable() {
        instance = this;
        configurationManager = ConfigurationManager.getInstance();

        var worlds = configurationManager.getWorlds();

        for (var world : worlds) {
            var serverWorld = getServer().getWorld(world);
            if (serverWorld == null) {
                getLogger().warning("Could not find world " + world);
                continue;
            }
            getServer().getPluginManager().registerEvents(new ChunkGenerationListener(serverWorld), this);
        }

        var command = getCommand("toomanygen");

        if (command == null) {
            throw new IllegalStateException("Could not find core command toomanygen!");
        }

        command.setExecutor(new TooManyGenCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static TooManyGen getInstance() {
        return instance;
    }
}
