package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import com.legions.client.gui.atomics.LegionsAtomicsIntSlider;
import com.legions.client.gui.atomics.LegionsAtomicsSectionHeaderWidget;
import com.legions.client.gui.atomics.LegionsAtomicsToggleWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@Pseudo
@Mixin(targets = "com.atomics.client.gui.AtomicsClientScreen")
public abstract class AtomicsClientScreenModuleMixin extends Screen {
    @Unique
    private static final int LEGIONS_ROW_HEIGHT = 28;
    @Unique
    private static final int LEGIONS_SECTION_HEIGHT = 24;
    @Unique
    private static final int LEGIONS_RESET_WIDTH = 24;
    @Unique
    private static final int LEGIONS_BUTTON_HEIGHT = 22;

    @Unique
    private boolean legions_client$sectionCollapsed;

    protected AtomicsClientScreenModuleMixin(Text title) {
        super(title);
    }

    @Inject(method = "buildPvpSettings", at = @At("RETURN"), cancellable = true, remap = false)
    private void legions_client$addLegionsModule(int y, CallbackInfoReturnable<Integer> cir) {
        if (!LegionsClient.isAtomicsClientLoaded() || LegionsClient.CONFIG == null || !legions_client$shouldShowModule()) {
            return;
        }

        cir.setReturnValue(legions_client$buildLegionsModule(cir.getReturnValue()));
    }

    @Unique
    private int legions_client$buildLegionsModule(int y) {
        int leftX = legions_client$getIntField("leftX", 18);
        int leftWidth = legions_client$getIntField("leftWidth", Math.max(260, this.width - 36));
        int controlWidth = Math.max(80, leftWidth - LEGIONS_RESET_WIDTH - 6);
        boolean searchTab = legions_client$isSearchTab();
        boolean collapsed = searchTab ? legions_client$searchQuery().isBlank() : legions_client$sectionCollapsed;

        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(new LegionsAtomicsSectionHeaderWidget(this.textRenderer, leftX, y, leftWidth, LEGIONS_BUTTON_HEIGHT, "Legions", collapsed, () -> {
                legions_client$sectionCollapsed = !legions_client$sectionCollapsed;
                clearAndInit();
            }));
        }
        y += LEGIONS_SECTION_HEIGHT;

        if (collapsed) {
            return y + 10;
        }

        y = legions_client$addToggle(leftX, y, controlWidth, "Enable Legions Client", () -> LegionsClient.CONFIG.enabled, value -> LegionsClient.CONFIG.enabled = value);
        y = legions_client$addToggle(leftX, y, controlWidth, "Rating Nametags", () -> LegionsClient.CONFIG.ratingNametagsEnabled, value -> LegionsClient.CONFIG.ratingNametagsEnabled = value);
        y = legions_client$addToggle(leftX, y, controlWidth, "Foe Outlines", () -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled, value -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled = value);
        y = legions_client$addToggle(leftX, y, controlWidth, "Spectator Glow", () -> LegionsClient.CONFIG.spectatorGlowEnabled, value -> LegionsClient.CONFIG.spectatorGlowEnabled = value);
        y = legions_client$addToggle(leftX, y, controlWidth, "Warning Particles", () -> LegionsClient.CONFIG.warningParticlesEnabled, value -> LegionsClient.CONFIG.warningParticlesEnabled = value);
        y = legions_client$addToggle(leftX, y, controlWidth, "Team Ping", () -> LegionsClient.CONFIG.teamPingEnabled, value -> LegionsClient.CONFIG.teamPingEnabled = value);
        y = legions_client$addIntSlider(leftX, y, controlWidth, "Opponents Shown", 1, 12, LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value);
        y = legions_client$addIntSlider(leftX, y, controlWidth, "Ping Seconds", 3, 10, LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value);

        return y + 10;
    }

    @Unique
    private int legions_client$addToggle(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(new LegionsAtomicsToggleWidget(this.textRenderer, x, y, width, LEGIONS_BUTTON_HEIGHT, label, getter.getAsBoolean(), () -> {
                boolean value = !getter.getAsBoolean();
                setter.accept(value);
                LegionsClient.saveConfig();
                clearAndInit();
            }));
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addIntSlider(int x, int y, int width, String label, int min, int max, int initial, IntConsumer setter) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(new LegionsAtomicsIntSlider(x, y, width, LEGIONS_BUTTON_HEIGHT, label, min, max, initial, setter));
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private boolean legions_client$shouldShowModule() {
        if (!legions_client$isSearchTab()) {
            return true;
        }
        String query = legions_client$searchQuery();
        if (query.isBlank()) {
            return true;
        }
        String terms = "legions lc rating nametag foe outline spectator glow warning particle team ping opponent opponents shown";
        return terms.contains(query);
    }

    @Unique
    private boolean legions_client$isSearchTab() {
        Object selectedTab = legions_client$getField("selectedTab");
        return selectedTab instanceof Enum<?> tab && "SEARCH".equals(tab.name());
    }

    @Unique
    private String legions_client$searchQuery() {
        Object value = legions_client$getField("settingsSearch");
        return value instanceof String text ? text.trim().toLowerCase(Locale.ROOT) : "";
    }

    @Unique
    private boolean legions_client$isWidgetVisible(int y) {
        int contentTop = legions_client$getIntField("contentTop", 0);
        int contentBottom = legions_client$getIntField("contentBottom", this.height);
        return y >= contentTop && y + LEGIONS_BUTTON_HEIGHT <= contentBottom - 6;
    }

    @Unique
    private int legions_client$getIntField(String name, int fallback) {
        Object value = legions_client$getField(name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    @Unique
    private Object legions_client$getField(String name) {
        try {
            Field field = this.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(this);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
