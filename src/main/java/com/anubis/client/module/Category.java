package com.anubis.client.module;

public enum Category {
    CRYSTALS("Crystals", "Crystal PvP helpers."),
    ANCHOR("Anchor", "Respawn anchor info & visuals."),
    INV("Inv", "Know what you're holding."),
    ESSENTIALS("Essentials", "Visibility & comfort for PvP.");

    public final String label, subtitle;
    Category(String l, String s) { label = l; subtitle = s; }
}
