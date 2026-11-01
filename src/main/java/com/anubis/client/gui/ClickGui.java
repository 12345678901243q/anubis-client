package com.anubis.client.gui;

import com.anubis.client.Config;
import com.anubis.client.module.Category;
import com.anubis.client.module.Module;
import com.anubis.client.module.ModuleManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Anubis ClickGUI (Halloween edition). Mouse/keys are polled through GLFW so it
 *  doesn't depend on the Screen input method signatures that change between versions. */
public class ClickGui extends Screen {
    private static final Identifier LOGO = Identifier.of("anubis", "textures/gui/logo.png");

    // palette
    private static final int BG = 0xF2100818, PANEL = 0xFF190D2A, CARD = 0xFF1E1233, CARD_HOVER = 0xFF271848;
    private static final int PURPLE = 0xFF8B3DFF, PURPLE_DARK = 0xFF6D28D9, ORANGE = 0xFFFF7A18, TEXT = 0xFFFFFFFF, MUTED = 0xFFA897C9;

    private static final int W = 500, H = 320, SIDE = 112, CARD_W = 178, CARD_H = 46, GAP = 6;

    private Category tab = Category.CRYSTALS;
    private int scroll = 0;
    private boolean wasDown, wasShift;
    private Module dragging;

    public ClickGui() { super(Text.literal("Anubis")); }

    @Override public boolean shouldPause() { return false; }

    @Override protected void init() { wasShift = key(GLFW.GLFW_KEY_RIGHT_SHIFT); wasDown = true; }

    @Override public void removed() { Config.save(); }

    // we draw our own backdrop (avoids a double-blur crash)
    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x99000000);
    }

    private boolean key(int k) { return GLFW.glfwGetKey(client.getWindow().getHandle(), k) == GLFW.GLFW_PRESS; }
    private boolean mouseDown() { return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS; }

    // layout helpers
    private int wx() { return (width - W) / 2; }
    private int wy() { return (height - H) / 2; }
    private int viewTop() { return wy() + 92; }
    private int viewBottom() { return wy() + H - 24; }
    private int cardX(int i) { return wx() + SIDE + 10 + (i % 2) * (CARD_W + GAP); }
    private int cardY(int i) { return viewTop() + (i / 2) * (CARD_H + GAP) - scroll; }
    private int maxScroll(int n) {
        int rows = (n + 1) / 2;
        return Math.max(0, rows * (CARD_H + GAP) - (viewBottom() - viewTop()));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        boolean sh = key(GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (sh && !wasShift) { close(); return; }
        wasShift = sh;

        boolean down = mouseDown();
        List<Module> mods = ModuleManager.in(tab);
        if (down && !wasDown) handleClick(mx, my, mods);
        if (!down) dragging = null;
        if (dragging != null) {
            int cx = cardX(mods.indexOf(dragging));
            double f = (mx - (cx + 10)) / (double) (CARD_W - 20);
            dragging.setValue(dragging.min + Math.max(0, Math.min(1, f)) * (dragging.max - dragging.min));
        }
        wasDown = down;

        int x = wx(), y = wy();

        // window + glow border
        ctx.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0x558B3DFF);
        ctx.fill(x - 1, y - 1, x + W + 1, y + H + 1, PURPLE);
        ctx.fill(x, y, x + W, y + H, BG);
        ctx.fill(x, y + 70, x + W, y + 71, 0xFF3A2268);
        ctx.fill(x, y + 71, x + SIDE, y + H, PANEL);

        // header: logo + title
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, x + 10, y + 8, 0f, 0f, 56, 56, 128, 128);
        ctx.drawText(textRenderer, Text.literal("Anubis ").formatted(Formatting.BOLD), x + 76, y + 20, TEXT, true);
        ctx.drawText(textRenderer, Text.literal("Client").formatted(Formatting.BOLD), x + 76 + textRenderer.getWidth("Anubis "), y + 20, 0xFFB07CFF, true);
        ctx.drawText(textRenderer, "Spooky Edition - Clean. Fast. Legit.", x + 76, y + 36, ORANGE, true);
        ctx.drawText(textRenderer, "Press RIGHT SHIFT to close", x + 76, y + 48, MUTED, false);

        drawPumpkin(ctx, x + W - 52, y + 18);
        drawBat(ctx, x + W - 118, y + 14, 0xFF6D28D9);
        drawBat(ctx, x + W - 150, y + 30, 0xFF4B2A8F);
        drawWeb(ctx, x + W - 1, y + H - 1);

        // tabs
        int ty = y + 80;
        for (Category c : Category.values()) {
            boolean sel = c == tab, hov = mx >= x + 8 && mx <= x + SIDE - 8 && my >= ty && my <= ty + 26;
            ctx.fill(x + 8, ty, x + SIDE - 8, ty + 26, sel ? PURPLE_DARK : hov ? CARD_HOVER : 0x00000000);
            if (sel) ctx.fill(x + 8, ty, x + 10, ty + 26, ORANGE);
            ctx.drawText(textRenderer, c.label.toUpperCase(), x + 20, ty + 9, sel ? TEXT : MUTED, true);
            ty += 30;
        }
        ctx.drawText(textRenderer, "Anubis v1.0", x + 14, y + H - 16, 0xFFB07CFF, false);

        // category title
        ctx.drawText(textRenderer, Text.literal(tab.label.toUpperCase()).formatted(Formatting.BOLD), x + SIDE + 12, y + 76, TEXT, true);
        ctx.drawText(textRenderer, tab.subtitle, x + SIDE + 12 + textRenderer.getWidth(tab.label.toUpperCase()) + 10, y + 76, MUTED, false);

        // module cards (scrollable)
        scroll = Math.max(0, Math.min(scroll, maxScroll(mods.size())));
        ctx.enableScissor(x + SIDE, viewTop(), x + W - 4, viewBottom());
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int cx = cardX(i), cy = cardY(i);
            if (cy + CARD_H < viewTop() || cy > viewBottom()) continue;
            boolean hov = mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H
                    && my >= viewTop() && my <= viewBottom();
            ctx.fill(cx - 1, cy - 1, cx + CARD_W + 1, cy + CARD_H + 1, m.enabled ? PURPLE : 0xFF2C1B4A);
            ctx.fill(cx, cy, cx + CARD_W, cy + CARD_H, hov ? CARD_HOVER : CARD);
            ctx.drawText(textRenderer, m.name, cx + 8, cy + 7, TEXT, true);
            String desc = m.hasSlider ? "Value: " + (int) Math.round(m.value) : m.description;
            while (textRenderer.getWidth(desc) > CARD_W - 54 && desc.length() > 4) desc = desc.substring(0, desc.length() - 4) + "..";
            ctx.drawText(textRenderer, desc, cx + 8, cy + 20, MUTED, false);

            // switch
            int sx = cx + CARD_W - 32, sy = cy + 8;
            ctx.fill(sx, sy, sx + 24, sy + 12, m.enabled ? PURPLE : 0xFF3B3350);
            int kx = m.enabled ? sx + 13 : sx + 1;
            ctx.fill(kx, sy + 1, kx + 10, sy + 11, 0xFFFFFFFF);

            if (m.hasSlider) {
                int tx = cx + 10, tw = CARD_W - 20, ty2 = cy + 36;
                ctx.fill(tx, ty2, tx + tw, ty2 + 3, 0xFF3B3350);
                int fw = (int) (tw * (m.value - m.min) / (m.max - m.min));
                ctx.fill(tx, ty2, tx + fw, ty2 + 3, ORANGE);
                ctx.fill(tx + fw - 2, ty2 - 2, tx + fw + 2, ty2 + 5, 0xFFFFFFFF);
            }
        }
        ctx.disableScissor();
    }

    private void handleClick(int mx, int my, List<Module> mods) {
        int x = wx(), ty = wy() + 80;
        for (Category c : Category.values()) {
            if (mx >= x + 8 && mx <= x + SIDE - 8 && my >= ty && my <= ty + 26) { tab = c; scroll = 0; return; }
            ty += 30;
        }
        if (my < viewTop() || my > viewBottom()) return;
        for (int i = 0; i < mods.size(); i++) {
            int cx = cardX(i), cy = cardY(i);
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                Module m = mods.get(i);
                if (m.hasSlider && my >= cy + 30) { dragging = m; return; }
                m.toggle();
                return;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= (int) (vertical * 18);
        scroll = Math.max(0, Math.min(scroll, maxScroll(ModuleManager.in(tab).size())));
        return true;
    }

    // ---------- halloween decorations ----------
    private void drawPumpkin(DrawContext c, int x, int y) {
        int o = 0xFFFF7A18, d = 0xFFC9540A;
        c.fill(x + 12, y - 5, x + 16, y + 1, 0xFF3FA34D);          // stem
        c.fill(x + 2, y + 1, x + 26, y + 25, d);                     // dark body
        c.fill(x, y + 4, x + 28, y + 22, d);
        c.fill(x + 4, y + 2, x + 24, y + 24, o);                     // light body
        c.fill(x + 2, y + 5, x + 26, y + 21, o);
        c.fill(x + 13, y + 1, x + 15, y + 25, d);                    // ridge
        c.fill(x + 7, y + 8, x + 11, y + 12, 0xFF1A0B2E);            // eyes
        c.fill(x + 17, y + 8, x + 21, y + 12, 0xFF1A0B2E);
        c.fill(x + 8, y + 15, x + 20, y + 18, 0xFF1A0B2E);           // mouth
        c.fill(x + 10, y + 18, x + 12, y + 20, 0xFF1A0B2E);
        c.fill(x + 16, y + 18, x + 18, y + 20, 0xFF1A0B2E);
    }

    private void drawBat(DrawContext c, int x, int y, int col) {
        c.fill(x + 6, y + 2, x + 9, y + 7, col);                     // body
        c.fill(x + 6, y, x + 7, y + 2, col);                         // ears
        c.fill(x + 8, y, x + 9, y + 2, col);
        c.fill(x + 2, y + 2, x + 6, y + 5, col);                     // wings
        c.fill(x + 9, y + 2, x + 13, y + 5, col);
        c.fill(x, y + 4, x + 3, y + 7, col);
        c.fill(x + 12, y + 4, x + 15, y + 7, col);
    }

    private void drawWeb(DrawContext c, int cx, int cy) {
        int col = 0x55FFFFFF;
        double[] slopes = {0.0, 0.35, 0.8, 1.6, 3.5, 99};
        for (double s : slopes) {
            for (int t = 0; t < 44; t++) {
                int px = cx - t, py = (s == 99) ? cy - t * 2 : cy - (int) (t * s);
                if (s == 99) px = cx - t / 6;
                c.fill(px, py, px + 1, py + 1, col);
            }
        }
        for (int r = 14; r <= 40; r += 13) {
            for (int a = 0; a <= 90; a += 3) {
                double rad = Math.toRadians(a);
                int px = cx - (int) (Math.cos(rad) * r), py = cy - (int) (Math.sin(rad) * r);
                c.fill(px, py, px + 1, py + 1, col);
            }
        }
    }
}
