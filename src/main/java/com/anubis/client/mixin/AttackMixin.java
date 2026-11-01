package com.anubis.client.mixin;

import com.anubis.client.module.ModuleManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fast Crystal: same idea as a crystal optimizer. The attack packet is still sent normally,
 *  we only remove the crystal on YOUR screen right away instead of waiting for the server. */
@Mixin(ClientPlayerInteractionManager.class)
public class AttackMixin {
    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void anubis$fastCrystal(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (ModuleManager.FAST_CRYSTAL != null && ModuleManager.FAST_CRYSTAL.enabled
                && target instanceof EndCrystalEntity) {
            target.remove(Entity.RemovalReason.KILLED);
        }
    }
}
