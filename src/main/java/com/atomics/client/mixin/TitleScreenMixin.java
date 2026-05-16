package com.atomics.client.mixin;

import com.atomics.client.gui.AtomicsClientScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void atomics_client$addMenuButton(CallbackInfo ci) {
        int buttonWidth = 98;
        int x = this.width / 2 + 104;
        int y = this.height / 4 + 48 + 48;
        addDrawableChild(ButtonWidget.builder(Text.literal("Atomics"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new AtomicsClientScreen(this));
        }).dimensions(x, y, buttonWidth, 20).build());
    }
}
