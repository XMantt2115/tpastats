package pl.tpastats;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class TpaStatsClient implements ClientModInitializer {

    private static final Set<String> TPA_ALIASES = Set.of("tpa", "etpa");
    private static final long TIMEOUT_MS = 60_000;
    private static final double TELEPORT_DISTANCE = 8.0;

    private String pendingNick = null;
    private long pendingSince = 0;
    private Vec3d lastPos = null;

    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.COMMAND.register(command -> {
            String[] parts = command.trim().split("\\s+");
            if (parts.length >= 2 && TPA_ALIASES.contains(parts[0].toLowerCase())) {
                pendingNick = parts[1];
                pendingSince = System.currentTimeMillis();
                lastPos = null;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                pendingNick = null;
                lastPos = null;
                return;
            }

            Vec3d current = client.player.getPos();

            if (pendingNick != null) {
                if (System.currentTimeMillis() - pendingSince > TIMEOUT_MS) {
                    pendingNick = null;
                } else if (lastPos != null && current.distanceTo(lastPos) > TELEPORT_DISTANCE) {
                    sendStats(client, pendingNick);
                    pendingNick = null;
                }
            }

            lastPos = current;
        });
    }

    private void sendStats(MinecraftClient client, String nick) {
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand("stats " + nick);
        }
    }
}
