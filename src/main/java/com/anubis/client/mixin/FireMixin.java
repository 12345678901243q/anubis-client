package com.anubis.client.mixin;

import com.anubis.client.module.ModuleManager;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Fire: hides the big fire overlay that blocks your view. */
@Mixin(InGameOverlayRenderer.class)
public class FireMixin {
    // NOTE: if the build complains about static/non-static here, just remove or add the `static` keyword.
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void anubis$lowFire(CallbackInfo ci) {
        if (ModuleManager.LOW_FIRE != null && ModuleManager.LOW_FIRE.enabled) ci.cancel();
    }
}
