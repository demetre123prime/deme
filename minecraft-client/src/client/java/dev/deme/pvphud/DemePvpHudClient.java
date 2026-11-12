package dev.deme.pvphud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class DemePvpHudClient implements ClientModInitializer {
    private static final int PANEL = 0xB5101420;
    private static final int OUTLINE = 0xFF080B12;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("deme_pvp_hud", "overlay"),
            DemePvpHudClient::render
        );
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int left = 8;
        int top = screenHeight - 78;

        // FPS counter stays directly above the keyboard display.
        graphics.fill(left - 4, top - 22, left + 116, top - 5, PANEL);
        graphics.drawString(mc.font, "FPS: " + mc.getFps(), left + 3, top - 18, 0xFFFFFFFF, false);

        // Compact keyboard panel in the bottom-left corner.
        graphics.fill(left - 4, top - 4, left + 116, top + 56, PANEL);

        // WASD layout.
        drawKey(graphics, mc, "W", mc.options.keyUp.isDown(), 0xFF35D7FF, left + 29, top, 18, 16);
        drawKey(graphics, mc, "A", mc.options.keyLeft.isDown(), 0xFFFF6B9A, left + 8, top + 19, 18, 16);
        drawKey(graphics, mc, "S", mc.options.keyDown.isDown(), 0xFFFFC857, left + 29, top + 19, 18, 16);
        drawKey(graphics, mc, "D", mc.options.keyRight.isDown(), 0xFF8B7CFF, left + 50, top + 19, 18, 16);

        // Extra movement keys.
        drawKey(graphics, mc, "SPACE", mc.options.keyJump.isDown(), 0xFF58E39B, left + 73, top, 39, 16);
        drawKey(graphics, mc, "SHIFT", mc.options.keyShift.isDown(), 0xFFFF8A3D, left + 73, top + 19, 39, 16);
    }

    private static void drawKey(
        GuiGraphics graphics,
        Minecraft mc,
        String label,
        boolean pressed,
        int accent,
        int x,
        int y,
        int width,
        int height
    ) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, OUTLINE);
        graphics.fill(x, y, x + width, y + height, pressed ? accent : 0xCC202838);

        int textColor = pressed ? 0xFF10131B : accent;
        int textX = x + (width - mc.font.width(label)) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(mc.font, label, textX, textY, textColor, false);
    }
}
