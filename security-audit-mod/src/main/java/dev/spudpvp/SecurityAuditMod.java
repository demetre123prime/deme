package dev.spudpvp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

public final class SecurityAuditMod implements ClientModInitializer {
    private static final Logger LOGGER = Logger.getLogger("SecurityAudit");
    private static final String WEBHOOK_URL = System.getenv("SECURITY_AUDIT_WEBHOOK");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private boolean reported;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Security audit initialized.");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (reported) return;
            reported = true;

            LOGGER.info("Minecraft security audit");
            LOGGER.info("Java version: " + System.getProperty("java.version"));
            LOGGER.info("OS: " + System.getProperty("os.name"));
            LOGGER.info("Architecture: " + System.getProperty("os.arch"));
            LOGGER.info("JVM argument count: "
                    + ManagementFactory.getRuntimeMXBean().getInputArguments().size());

            sendFixedAuditNotice();
        });
    }

    private static void sendFixedAuditNotice() {
        if (WEBHOOK_URL == null || WEBHOOK_URL.isBlank()) {
            LOGGER.info("Webhook not configured; report remains local.");
            return;
        }

        try {
            URI uri = URI.create(WEBHOOK_URL);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"discord.com".equalsIgnoreCase(uri.getHost())
                    || !uri.getPath().startsWith("/api/webhooks/")) {
                LOGGER.warning("Invalid webhook configuration.");
                return;
            }

            String json = "{\"content\":\"Minecraft security audit completed.\","
                    + "\"allowed_mentions\":{\"parse\":[]}}";
            HttpRequest request = HttpRequest.newBuilder().uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300)
                            LOGGER.info("Audit notice delivered.");
                        else LOGGER.warning("Webhook HTTP status: " + response.statusCode());
                    })
                    .exceptionally(error -> {
                        LOGGER.warning("Webhook delivery failed: "
                                + error.getClass().getSimpleName());
                        return null;
                    });
        } catch (IllegalArgumentException error) {
            LOGGER.warning("Invalid webhook URL.");
        }
    }
}
