package fun.milkyway.toomanygen;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkGenerationListener implements Listener {
    private final Map<UUID, Integer> rates;
    private final Map<UUID, Long> lastViewDistanceUpdate;
    private final Map<UUID, BossBar> bossBars;
    private final World world;
    private final BukkitTask cooldownTask;

    public ChunkGenerationListener(World world) {
        TooManyGen.getInstance().getLogger().info("Initializing listener for world " + world.getName());
        this.rates = new HashMap<>();
        this.bossBars = new HashMap<>();
        this.lastViewDistanceUpdate = new HashMap<>();
        this.world = world;
        cooldownTask = coolDownTask();
    }

    public void shutdown() {
        TooManyGen.getInstance().getLogger().info("Shutting down listener for world " + world.getName());
        cooldownTask.cancel();
        for (var bossBar : bossBars.entrySet()) {
            var player = TooManyGen.getInstance().getServer().getPlayer(bossBar.getKey());
            if (player == null) {
                continue;
            }
            player.hideBossBar(bossBar.getValue());
        }
        world.getPlayers().forEach(player -> player.setViewDistance(world.getViewDistance()));
    }

    private BukkitTask coolDownTask() {
        return TooManyGen.getInstance().getServer().getScheduler().runTaskTimer(TooManyGen.getInstance(), () -> {
            var iter = rates.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                var rate = entry.getValue();
                var multiplier = getCooldownMultiplier(rate);
                entry.setValue(rate - multiplier * getLocalConfiguration().getInt("coolingRate"));
                updateBossbar(entry.getKey(), entry.getValue());
                var update = updateViewDistance(entry.getKey(), entry.getValue(), false);
                if (entry.getValue() <= 0) {
                    iter.remove();
                    if (!update) {
                        updateViewDistance(entry.getKey(), 0, true);
                    }
                }
            }
        }, 0, 10);
    }

    @EventHandler
    public void onChunkGeneration(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }
        if (!event.getWorld().equals(world)) {
            return;
        }
        var closest = getClosest(event.getChunk().getX(), event.getChunk().getZ(), getLocalConfiguration().getInt("chunkGenDistance"));
        if (closest == null) {
            return;
        }
        if (closest.hasPermission("toomanygen.bypass") && !closest.isOp()) {
            return;
        }
        var rate = rates.getOrDefault(closest.getUniqueId(), 0);

        if (isOcean(event.getChunk().getWorld().getComputedBiome(event.getChunk().getX() * 16, 63, event.getChunk().getZ() * 16))) {
            rates.put(closest.getUniqueId(), rate + 2);
            return;
        }

        rates.put(closest.getUniqueId(), rate + 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onElytraDamage(PlayerItemDamageEvent event) {
        if (!getLocalConfiguration().getBoolean("elytraDamage")) {
            return;
        }
        if (!event.getItem().getType().equals(Material.ELYTRA)) {
            return;
        }
        if (event.getPlayer().getWorld() != world) {
            return;
        }
        var rate = rates.getOrDefault(event.getPlayer().getUniqueId(), 0);
        var multiplier = getMultiplier(rate);

        var damage = event.getDamage() * multiplier;

        if (damage < getLocalConfiguration().getInt("elytraVanishMultiplier")) {
            var damageable = (Damageable) event.getItem().getItemMeta();
            var health = event.getItem().getType().getMaxDurability() - damageable.getDamage();
            damage = Math.min(health - 1, damage);
        }

        if (!getLocalConfiguration().getBoolean("elytraVanish")) {
            return;
        }

        event.setDamage(damage);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld() != world) {
            return;
        }
        updateViewDistance(event.getPlayer().getUniqueId(), rates.getOrDefault(event.getPlayer().getUniqueId(), 0), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getWorld() != world) {
            return;
        }
        updateViewDistance(event.getPlayer().getUniqueId(), rates.getOrDefault(event.getPlayer().getUniqueId(), 0), true);
        FlickerSuppressManager.getInstance().clearFlickerSuppress(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        hideBossBar(event.getPlayer());
    }

    private boolean isOcean(Biome biome) {
        return biome.equals(Biome.OCEAN)
                || biome.equals(Biome.DEEP_OCEAN)
                || biome.equals(Biome.FROZEN_OCEAN)
                || biome.equals(Biome.DEEP_FROZEN_OCEAN)
                || biome.equals(Biome.COLD_OCEAN)
                || biome.equals(Biome.DEEP_COLD_OCEAN)
                || biome.equals(Biome.LUKEWARM_OCEAN)
                || biome.equals(Biome.DEEP_LUKEWARM_OCEAN)
                || biome.equals(Biome.WARM_OCEAN);
    }

    private int getCooldownMultiplier(int rate) {
        var threshold = getLocalConfiguration().getInt("punishThreshold");
        var thresholdModifier = getLocalConfiguration().getDouble("coolingSpeedup");
        var thresholdModified = (int) (threshold * thresholdModifier);
        if (rate <= thresholdModified) {
            return 1;
        }
        return rate / thresholdModified;
    }

    private int getMultiplier(int rate) {
        var threshold = getLocalConfiguration().getInt("punishThreshold");
        if (rate <= threshold) {
            return 1;
        }
        var baseMultiplier = getLocalConfiguration().getDouble("elytraDamageMultiplier");
        return (int) Math.pow(baseMultiplier, (double) rate / threshold);
    }

    private void updateBossbar(UUID uuid, int rate) {
        var player = TooManyGen.getInstance().getServer().getPlayer(uuid);
        if (player == null) {
            return;
        }
        var threshold = getLocalConfiguration().getInt("punishThreshold");
        if (rate <= threshold || !getLocalConfiguration().getBoolean("elytraDamage")) {
            hideBossBar(player);
            return;
        }
        var bossbar = bossBars.getOrDefault(uuid, null);
        if (bossbar == null) {
            bossbar = BossBar.bossBar(LangManager.getInstance().getComponent("chunkGeneration.limitReached"), 0.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            player.showBossBar(bossbar);
            bossBars.put(uuid, bossbar);
        }
        decorateBossBar(bossbar, rate);
    }

    private void hideBossBar(Player player) {
        if (bossBars.containsKey(player.getUniqueId())) {
            player.hideBossBar(bossBars.get(player.getUniqueId()));
            bossBars.remove(player.getUniqueId());
        }
    }

    private void decorateBossBar(BossBar bossBar, int rate) {
        var multiplier = getMultiplier(rate);
        var vanishMultiplier = getLocalConfiguration().getInt("elytraVanishMultiplier");
        var percentage = Math.min(multiplier / (float) vanishMultiplier, 1.0f);
        var componentBuilder = Component.text().append(LangManager.getInstance().getComponent("chunkGeneration.limitReached"));
        if (!getLocalConfiguration().getBoolean("elytraVanish") || multiplier < vanishMultiplier) {
            componentBuilder.append(LangManager.getInstance().getComponent("chunkGeneration.multiplier", String.valueOf(multiplier)));
        }
        else {
            componentBuilder.append(LangManager.getInstance().getComponent("chunkGeneration.vanishWarning"));
        }
        bossBar.name(componentBuilder.build());
        bossBar.progress(percentage);
    }

    private @Nullable Player getClosest(int x, int z, int maxDistance) {
        var closestDistance = Double.MAX_VALUE;
        Player closest = null;
        for (var player : world.getPlayers()) {
            if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
                continue;
            }
            if (player.isFlying()) {
                continue;
            }
            var distance = Math.sqrt(Math.pow(player.getChunk().getX() - x, 2) + Math.pow(player.getChunk().getZ() - z, 2));
            if (distance > maxDistance) {
                continue;
            }
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }
        }
        return closest;
    }

    private boolean updateViewDistance(UUID uuid, int rate, boolean force) {
        var player = TooManyGen.getInstance().getServer().getPlayer(uuid);
        if (player == null || player.getWorld() != world) {
            return false;
        }
        var viewDistance = getViewDistance(rate);
        // we always update view distance if it's lower than the current one, but we also update it
        // if it's higher than the current one and the last update was more than 60 seconds ago
        if (player.getViewDistance() > viewDistance || (
                player.getViewDistance() < viewDistance && (force || System.currentTimeMillis() - lastViewDistanceUpdate.getOrDefault(uuid, 0L) > 60000L))) {
            player.setViewDistance(viewDistance);
            lastViewDistanceUpdate.put(uuid, System.currentTimeMillis());
            FlickerSuppressManager.getInstance().suppressFlicker(player.getUniqueId());
            return true;
        }
        return false;
    }

    private int getViewDistance(int rate) {
        var threshold = getLocalConfiguration().getInt("punishThreshold");
        var viewDistances = getLocalConfiguration().getIntegerList("viewDistanceValues");
        var thresholds = getLocalConfiguration().getDoubleList("viewDistanceThresholds");
        var viewDistance = 32;
        for (var i = 0; i < thresholds.size(); i++) {
            if (rate > thresholds.get(i) * threshold) {
                viewDistance = viewDistances.get(i);
            }
            else {
                break;
            }
        }
        return Math.min(world.getViewDistance(), viewDistance);
    }

    private ConfigurationSection getLocalConfiguration() {
        return ConfigurationManager.getInstance().getConfiguration().getConfigurationSection("worlds."+world.getName());
    }
}
