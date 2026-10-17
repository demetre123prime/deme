package dev.spudpvp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A harmless local audit mod. It logs that the mod loaded and shows one notice in chat.
 * It does not read files, browser data, passwords, cookies, tokens, or send network requests.
 */
public final class SecurityAuditMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("security_audit");
    private boolean noticeShown;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Security Audit mod loaded. No personal data is collected or transmitted.");
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (!noticeShown && client.player != null) {
            client.player.sendMessage(
                    Text.literal("Security Audit loaded — no personal data is collected."), false);
            noticeShown = true;
        }
    }
}
