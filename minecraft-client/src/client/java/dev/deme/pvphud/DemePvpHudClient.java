package dev.deme.pvphud;

import java.util.ArrayDeque;
import java.util.Deque;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class DemePvpHudClient implements ClientModInitializer {
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static boolean wasLeftDown;
    private static boolean wasRightDown;

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

        long now = System.currentTimeMillis();
        long window = mc.getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (leftDown && !wasLeftDown) LEFT_CLICKS.addLast(now);
        if (rightDown && !wasRightDown) RIGHT_CLICKS.addLast(now);
        wasLeftDown = leftDown;
        wasRightDown = rightDown;

        trimOldClicks(LEFT_CLICKS, now);
        trimOldClicks(RIGHT_CLICKS, now);

        int x = 8;
        int y = 8;
        graphics.fill(x - 4, y - 4, x + 150, y + 77, 0x99090D18);
        graphics.drawString(mc.font, "DEME HUD", x, y, 0xFF55FFFF, false);
        graphics.drawString(mc.font, "FPS: " + mc.getFps(), x, y + 12, 0xFFFFFFFF, false);
        graphics.drawString(mc.font, String.format("XYZ: %.1f %.1f %.1f",
            mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y + 24, 0xFFFFFFFF, false);
        graphics.drawString(mc.font, "CPS: " + LEFT_CLICKS.size() + " | " + RIGHT_CLICKS.size(),
            x, y + 36, 0xFFFFD76A, false);

        String keys = (mc.options.keyUp.isDown() ? "W" : "w") + " "
            + (mc.options.keyLeft.isDown() ? "A" : "a") + " "
            + (mc.options.keyDown.isDown() ? "S" : "s") + " "
            + (mc.options.keyRight.isDown() ? "D" : "d");
        graphics.drawString(mc.font, "Keys: " + keys, x, y + 48, 0xFFB8C7FF, false);
        graphics.drawString(mc.font,
            (mc.options.keyJump.isDown() ? "[SPACE]" : " space ")
            + (mc.options.keyShift.isDown() ? " [SHIFT]" : ""),
            x, y + 60, 0xFFB8C7FF, false);
    }

    private static void trimOldClicks(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.removeFirst();
        }
    }
}
