package dev.spudpvp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

public final class SecurityAuditMod implements ClientModInitializer {
    private boolean noticeShown = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[Security Audit] Mod initialized; no personal data is collected.");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!noticeShown && client.player != null) {
                noticeShown = true;
                client.player.sendMessage(
                    Text.literal("Security Audit loaded — no personal data is collected."),
                    false
                );
            }
        });
    }
}
