package fun.milkyway.toomanygen;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class PacketListener extends PacketAdapter {
    public PacketListener() {
        super(TooManyGen.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.VIEW_DISTANCE);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        var player = event.getPlayer();
        if (!FlickerSuppressManager.getInstance().shouldSuppressFlicker(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }
}
