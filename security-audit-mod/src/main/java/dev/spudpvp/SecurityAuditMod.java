package dev.spudpvp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

/**
 * Local-only security audit placeholder.
 *
 * This mod does not inspect browser data, read credentials or session tokens,
 * access private application files, or make network requests.
 */
public final class SecurityAuditMod implements ClientModInitializer {
    private boolean announced;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!announced && client.player != null) {
                announced = true;
                client.player.sendMessage(
                    Text.literal("Security Audit loaded. No personal data is collected or transmitted."),
                    false
                );
            }
        });
    }
}
