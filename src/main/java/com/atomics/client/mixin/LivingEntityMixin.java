package com.atomics.client.mixin;

import com.atomics.client.PvpStatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void atomics_client$recordDeathRecap(DamageSource damageSource, CallbackInfo ci) {
        PvpStatsManager.recordDeath((LivingEntity) (Object) this, damageSource);
    }

    @Inject(method = "consumeItem", at = @At("HEAD"))
    private void atomics_client$recordGapConsumed(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack stack = entity.getActiveItem();
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            PvpStatsManager.recordGapConsumed(entity);
        }
    }
}
