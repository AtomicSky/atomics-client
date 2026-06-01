package com.atomics.client.mixin;

import com.atomics.client.PvpStatsManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void atomics_client$recordDeathRecap(DamageSource damageSource, CallbackInfo ci) {
        PvpStatsManager.recordDeath((LivingEntity) (Object) this, damageSource);
    }

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void atomics_client$recordGapConsumed(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack stack = entity.getUseItem();
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            PvpStatsManager.recordGapConsumed(entity);
        }
    }
}
