package com.atomics.client.mixin;

import com.atomics.client.gui.AtomicsClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void atomics_client$addMenuButton(CallbackInfo ci) {
        int buttonWidth = 98;
        int x = this.width / 2 + 104;
        int y = this.height / 4 + 48 + 48;
        addRenderableWidget(Button.builder(Component.literal("Atomics"), button -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new AtomicsClientScreen(this));
        }).bounds(x, y, buttonWidth, 20).build());
    }
}
