package fun.milkyway.toomanygen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LangManager {
    private static LangManager instance;

    private LangManager() {
    }

    public static LangManager getInstance() {
        if (instance == null) {
            instance = new LangManager();
        }
        return instance;
    }

    public Component getComponent(String key, String... replacements) {
        var rawMM = ConfigurationManager.getInstance().getLangConfiguration().getString(key);
        if (rawMM == null) {
            TooManyGen.getInstance().getLogger().warning("Could not find message " + key);
            return Component.text("ERROR: " + key);
        }
        for (int i = 0; i < replacements.length; i++) {
            rawMM = rawMM.replace("{" + i + "}", replacements[i]);
        }
        return MiniMessage.miniMessage().deserialize(rawMM);
    }
}
