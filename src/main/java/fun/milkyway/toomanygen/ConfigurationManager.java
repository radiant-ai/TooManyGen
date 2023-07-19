package fun.milkyway.toomanygen;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class ConfigurationManager {
    private static ConfigurationManager instance;

    private Configuration configuration;
    private Configuration langConfiguration;

    private ConfigurationManager() {
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
            instance.loadConfiguration();
        }
        return instance;
    }

    public Collection<String> getWorlds() {
        var worldsSection = configuration.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return List.of();
        }
        return worldsSection.getKeys(false);
    }

    public void loadConfiguration() {
        if (TooManyGen.getInstance().getDataFolder().mkdir()) {
            TooManyGen.getInstance().getLogger().info("Created data folder");
        }
        var configFile = new File(TooManyGen.getInstance().getDataFolder(), "config.yml");
        var langFile = new File(TooManyGen.getInstance().getDataFolder(), "lang.yml");

        try {
            configuration = loadDefaultConfig(configFile, "config.yml");
            langConfiguration = loadDefaultConfig(langFile, "lang.yml");
        } catch (IOException e) {
            TooManyGen.getInstance().getLogger().log(Level.SEVERE, "Could not load configuration", e);
        }
    }

    private Configuration loadDefaultConfig(File configFile, String resourceName) throws IOException {
        var configInputStream = TooManyGen.getInstance().getResource(resourceName);
        if (configInputStream == null) {
            TooManyGen.getInstance().getLogger().log(Level.SEVERE, "Could not find resource " + resourceName);
            throw new IllegalStateException("Could not find resource " + resourceName);
        }

        var configuration = YamlConfiguration.loadConfiguration(configFile);
        configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(configInputStream)));
        configuration.options().copyDefaults(true);
        configuration.save(configFile);

        return configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Configuration getLangConfiguration() {
        return langConfiguration;
    }
}
