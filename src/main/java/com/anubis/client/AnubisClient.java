package com.anubis.client;

import com.anubis.client.gui.ClickGui;
import com.anubis.client.module.Module;
import com.anubis.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AnubisClient implements ClientModInitializer {
    public static KeyBinding openKey, zoomKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("anubis", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.anubis.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        zoomKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.anubis.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));

        ModuleManager.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> Config.load());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            for (Module m : ModuleManager.ALL) if (m.enabled) m.onTick(client);
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("anubis", "hud"), (ctx, tick) -> renderHud(ctx));
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

        List<String> lines = new ArrayList<>();
        for (Module m : ModuleManager.ALL) {
            if (!m.enabled) continue;
            String l = m.hudLine(mc);
            if (l != null) lines.add(l);
            m.onHud(ctx, mc, w, h);
        }
        if (lines.isEmpty()) return;

        int tw = mc.textRenderer.getWidth("ANUBIS");
        for (String l : lines) tw = Math.max(tw, mc.textRenderer.getWidth(l));
        int x = 6, y = 6, ph = 14 + lines.size() * 11;
        ctx.fill(x, y, x + tw + 14, y + ph, 0xAA0B0612);
        ctx.fill(x, y, x + 2, y + ph, 0xFFFF7A18);
        ctx.drawText(mc.textRenderer, "ANUBIS", x + 7, y + 4, 0xFFFF7A18, true);
        int ly = y + 15;
        for (String l : lines) { ctx.drawText(mc.textRenderer, l, x + 7, ly, 0xFFD9C2FF, true); ly += 11; }
    }
}
