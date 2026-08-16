package com.atomics.client.mixin;

import com.atomics.client.AutoVillagerTrader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void atomics_client$suppressAutoTraderScreen(Screen screen, CallbackInfo ci) {
        if (AutoVillagerTrader.shouldSuppressScreen(screen)) {
            ci.cancel();
        }
    }
}
