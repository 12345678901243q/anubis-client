package com.anubis.client.module;

import com.anubis.client.AnubisClient;
import com.anubis.client.Config;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    public static Module FAST_CRYSTAL, LOW_FIRE;

    private static final int ORANGE = 0xFFFF7A18, PURPLE = 0xFFB07CFF, WHITE = 0xFFFFFFFF;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    
    private static void setParticles(int i) {
        var opt = mc().options.getParticles();
        var all = opt.getValue().getDeclaringClass().getEnumConstants();
        opt.setValue(all[i]);
    }

    private static void add(Module m) { ALL.add(m); }

    public static List<Module> in(Category c) {
        List<Module> l = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) l.add(m);
        return l;
    }

    // ---------- helpers ----------
    private static Module toggle(String n, String d, Category c, Runnable on, Runnable off) {
        return new Module(n, d, c) {
            @Override public void onEnable() { on.run(); }
            @Override public void onDisable() { off.run(); }
        };
    }

    static int count(MinecraftClient mc, Item... items) {
        if (mc.player == null) return 0;
        int n = 0;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            for (Item it : items) if (s.isOf(it)) n += s.getCount();
        }
        return n;
    }

    private static Module counter(String n, String d, Category c, String label, Item... items) {
        return new Module(n, d, c) {
            @Override public String hudLine(MinecraftClient mc) { return label + ": " + count(mc, items); }
        };
    }

    // ---------- init ----------
    public static void init() {
        // ===== CRYSTALS =====
        FAST_CRYSTAL = new Module("Fast Crystal",
                "Crystal optimizer: breaks crystals instantly on your screen.", Category.CRYSTALS) {};
        add(FAST_CRYSTAL);

        add(new Module("Spooky Crystals", "Halloween flames & witch magic around crystals.", Category.CRYSTALS) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.world == null || mc.player == null || mc.player.age % 3 != 0) return;
                Random r = mc.world.random;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof EndCrystalEntity && e.squaredDistanceTo(mc.player) < 576) {
                        mc.particleManager.addParticle(r.nextInt(3) == 0 ? ParticleTypes.FLAME : ParticleTypes.WITCH,
                                e.getX() + (r.nextDouble() - 0.5), e.getY() + r.nextDouble() * 2.0,
                                e.getZ() + (r.nextDouble() - 0.5), 0, 0.03, 0);
                    }
                }
            }
        });
        add(counter("Crystal Counter", "Shows how many end crystals you carry.", Category.CRYSTALS,
                "Crystals", Items.END_CRYSTAL));
        add(counter("Obsidian Counter", "Shows how much obsidian you carry.", Category.CRYSTALS,
                "Obsidian", Items.OBSIDIAN));

        // ===== ANCHOR =====
        add(new Module("Anchor Charge", "Shows the charge of the anchor you look at.", Category.ANCHOR) {
            @Override public void onHud(DrawContext ctx, MinecraftClient mc, int w, int h) {
                if (mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult hit)
                        || hit.getType() != HitResult.Type.BLOCK) return;
                BlockState st = mc.world.getBlockState(hit.getBlockPos());
                if (!st.isOf(Blocks.RESPAWN_ANCHOR)) return;
                int ch = st.get(RespawnAnchorBlock.CHARGES);
                String t = "Anchor charge: " + ch + "/4";
                ctx.drawText(mc.textRenderer, t, w / 2 - mc.textRenderer.getWidth(t) / 2, h / 2 + 12,
                        ch > 0 ? ORANGE : PURPLE, true);
            }
        });
        add(new Module("Spooky Anchors", "Soul fire & witch magic around nearby anchors.", Category.ANCHOR) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.world == null || mc.player == null || mc.player.age % 8 != 0) return;
                Random r = mc.world.random;
                BlockPos c = mc.player.getBlockPos();
                BlockPos.Mutable p = new BlockPos.Mutable();
                for (int x = -8; x <= 8; x++) for (int y = -8; y <= 8; y++) for (int z = -8; z <= 8; z++) {
                    p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    if (mc.world.getBlockState(p).isOf(Blocks.RESPAWN_ANCHOR)) {
                        mc.particleManager.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                                p.getX() + r.nextDouble(), p.getY() + 1.0, p.getZ() + r.nextDouble(), 0, 0.04, 0);
                        mc.particleManager.addParticle(ParticleTypes.WITCH,
                                p.getX() + r.nextDouble(), p.getY() + r.nextDouble(), p.getZ() + r.nextDouble(), 0, 0.02, 0);
                    }
                }
            }
        });
        add(counter("Anchor Counter", "Shows how many respawn anchors you carry.", Category.ANCHOR,
                "Anchors", Items.RESPAWN_ANCHOR));
        add(counter("Glowstone Counter", "Shows how much glowstone you carry.", Category.ANCHOR,
                "Glowstone", Items.GLOWSTONE));

        // ===== INV =====
        add(new Module("Armor HUD", "Armor pieces with durability on the right.", Category.INV) {
            @Override public void onHud(DrawContext ctx, MinecraftClient mc, int w, int h) {
                if (mc.player == null) return;
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                int x = w - 62, y = h / 2 - 40;
                for (EquipmentSlot s : slots) {
                    ItemStack st = mc.player.getEquippedStack(s);
                    if (!st.isEmpty()) {
                        ctx.fill(x - 3, y - 2, x + 58, y + 18, 0x990B0612);
                        ctx.drawItem(st, x, y);
                        if (st.getMaxDamage() > 0) {
                            int pct = (int) (100.0 * (st.getMaxDamage() - st.getDamage()) / st.getMaxDamage());
                            int col = pct > 60 ? 0xFF55FF55 : pct > 30 ? 0xFFFFAA00 : 0xFFFF5555;
                            ctx.drawText(mc.textRenderer, pct + "%", x + 22, y + 4, col, true);
                        }
                    }
                    y += 22;
                }
            }
        });
        add(counter("Totem Counter", "Shows your totems of undying.", Category.INV, "Totems", Items.TOTEM_OF_UNDYING));
        add(counter("Pearl Counter", "Shows your ender pearls.", Category.INV, "Pearls", Items.ENDER_PEARL));
        add(counter("Gapple Counter", "Shows your golden apples.", Category.INV, "Gapples",
                Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE));

        // ===== ESSENTIALS =====
        LOW_FIRE = new Module("Low Fire", "Hides the fire overlay so you can see.", Category.ESSENTIALS) {};
        add(LOW_FIRE);

        add(new Module("FOV Changer", "Set your FOV (70-110).", Category.ESSENTIALS) {
            @Override public void onEnable() {
                if (Config.origFov < 0) Config.origFov = mc().options.getFov().getValue();
                apply();
            }
            @Override public void onDisable() {
                if (Config.origFov > 0) mc().options.getFov().setValue(Config.origFov);
                Config.origFov = -1;
            }
            @Override public void onValueChanged() { apply(); }
            private void apply() { mc().options.getFov().setValue((int) Math.round(value)); }
        }.slider(70, 110, 100));

        add(toggle("FOV Effects Off", "No FOV change when sprinting or with speed.", Category.ESSENTIALS,
                () -> mc().options.getFovEffectScale().setValue(0.0),
                () -> mc().options.getFovEffectScale().setValue(1.0)));
        add(toggle("Damage Tilt Off", "No camera shake when you take damage.", Category.ESSENTIALS,
                () -> mc().options.getDamageTiltStrength().setValue(0.0),
                () -> mc().options.getDamageTiltStrength().setValue(1.0)));
                add(toggle("View Bobbing Off", "Stops the walking camera bob.", Category.ESSENTIALS,
                () -> mc().options.getBobView().setValue(false),
                () -> mc().options.getBobView().setValue(true)));
        add(toggle("Auto Jump", "Vanilla auto-jump up blocks.", Category.ESSENTIALS,
                () -> mc().options.getAutoJump().setValue(true),
                () -> mc().options.getAutoJump().setValue(false)));
        add(toggle("Low Particles", "Minimal particles for a clearer screen.", Category.ESSENTIALS,
                () -> setParticles(2),
                () -> setParticles(0)));

        add(new Module("Zoom", "Hold C to zoom in.", Category.ESSENTIALS) {
            private boolean zooming; private int saved;
            @Override public void onTick(MinecraftClient mc) {
                boolean down = AnubisClient.zoomKey.isPressed() && mc.currentScreen == null;
                if (down && !zooming) { saved = mc.options.getFov().getValue(); mc.options.getFov().setValue(30); zooming = true; }
                else if (!down && zooming) { mc.options.getFov().setValue(saved); zooming = false; }
            }
            @Override public void onDisable() {
                if (zooming) { mc().options.getFov().setValue(saved); zooming = false; }
            }
        });

        add(new Module("FPS Display", "Shows your FPS.", Category.ESSENTIALS) {
            @Override public String hudLine(MinecraftClient mc) { return "FPS: " + mc.getCurrentFps(); }
        });
        add(new Module("Ping Display", "Shows your ping.", Category.ESSENTIALS) {
            @Override public String hudLine(MinecraftClient mc) {
                if (mc.player == null || mc.getNetworkHandler() == null) return null;
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                return e == null ? null : "Ping: " + e.getLatency() + "ms";
            }
        });
        add(new Module("Coordinates", "Shows your XYZ.", Category.ESSENTIALS) {
            @Override public String hudLine(MinecraftClient mc) {
                if (mc.player == null) return null;
                return "XYZ: " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
            }
        });
    }
}
