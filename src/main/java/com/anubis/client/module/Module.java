package com.anubis.client.module;

import com.anubis.client.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class Module {
    public final String name, description;
    public final Category category;
    public boolean enabled;

    public boolean hasSlider;
    public double min, max, value;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    public Module slider(double min, double max, double def) {
        hasSlider = true; this.min = min; this.max = max; this.value = def; return this;
    }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
        if (!Config.loading) Config.save();
    }
    public void toggle() { setEnabled(!enabled); }

    public void setValue(double v) {
        value = Math.max(min, Math.min(max, v));
        if (enabled) onValueChanged();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onValueChanged() {}
    public void onTick(MinecraftClient mc) {}
    /** One line for the info panel (top-left), or null. */
    public String hudLine(MinecraftClient mc) { return null; }
    /** Custom drawing. */
    public void onHud(DrawContext ctx, MinecraftClient mc, int w, int h) {}
}
