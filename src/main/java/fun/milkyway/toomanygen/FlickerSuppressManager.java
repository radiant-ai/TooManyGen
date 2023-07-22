package fun.milkyway.toomanygen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlickerSuppressManager {
    private static FlickerSuppressManager instance;

    private final Map<UUID, Long> flickerSuppressMap;

    public static FlickerSuppressManager getInstance() {
        if (instance == null) {
            instance = new FlickerSuppressManager();
        }
        return instance;
    }

    private FlickerSuppressManager() {
        flickerSuppressMap = new ConcurrentHashMap<>();
    }

    public void suppressFlicker(UUID playerUUID) {
        flickerSuppressMap.put(playerUUID, System.currentTimeMillis() + 100L);
    }

    public boolean shouldSuppressFlicker(UUID playerUUID) {
        var time = flickerSuppressMap.get(playerUUID);
        if (time == null) {
            return false;
        }
        if (time < System.currentTimeMillis()) {
            flickerSuppressMap.remove(playerUUID);
            return false;
        }
        return true;
    }

    public void clearFlickerSuppress(UUID playerUUID) {
        flickerSuppressMap.remove(playerUUID);
    }
}
