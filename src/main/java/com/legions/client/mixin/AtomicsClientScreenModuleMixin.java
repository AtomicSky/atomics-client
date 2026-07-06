package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import com.legions.client.gui.LegionsTeamCountOverlayLayoutScreen;
import com.legions.client.gui.LegionsTeamHudLayoutScreen;
import com.legions.client.gui.atomics.LegionsAtomicsIntSlider;
import com.legions.client.gui.atomics.LegionsAtomicsSectionHeaderWidget;
import com.legions.client.gui.atomics.LegionsAtomicsToggleWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

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
    private static final String LEGIONS_FEATURE_KEY = "legions.client";

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

    @Inject(method = "shouldShowFeature", at = @At("HEAD"), cancellable = true, remap = false)
    private void legions_client$hideAtomicsTeamCountFeature(String key, String title, String[] terms, CallbackInfoReturnable<Boolean> cir) {
        if ("pvp.team_count_overlay".equals(key)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private int legions_client$buildLegionsModule(int y) {
        int leftX = legions_client$getIntField("leftX", 18);
        int leftWidth = legions_client$getIntField("leftWidth", Math.max(260, this.width - 36));
        int controlWidth = Math.max(80, leftWidth - LEGIONS_RESET_WIDTH - 6);
        Integer nativeY = legions_client$buildNativeLegionsModule(y, leftX, controlWidth);
        if (nativeY != null) {
            return nativeY;
        }

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
        if (LegionsClient.CONFIG.teamPingEnabled) {
            y = legions_client$addIntSlider(leftX, y, controlWidth, "Ping Seconds", 3, 10, LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Team Count Overlay", () -> LegionsClient.CONFIG.teamCountOverlayEnabled, value -> LegionsClient.CONFIG.teamCountOverlayEnabled = value);
        if (LegionsClient.CONFIG.teamCountOverlayEnabled) {
            y = legions_client$addMoveTeamCountButton(leftX, y, controlWidth);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Team HUD", () -> LegionsClient.CONFIG.teamHudEnabled, value -> LegionsClient.CONFIG.teamHudEnabled = value);
        if (LegionsClient.CONFIG.teamHudEnabled) {
            y = legions_client$addMoveTeamHudButton(leftX, y, controlWidth);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Limit Opponents Shown", () -> LegionsClient.CONFIG.opponentLimitEnabled, value -> LegionsClient.CONFIG.opponentLimitEnabled = value);
        if (LegionsClient.CONFIG.opponentLimitEnabled) {
            y = legions_client$addIntSlider(leftX, y, controlWidth, "Opponents Shown", 1, 12, LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Player Render Optimization", () -> LegionsClient.CONFIG.playerRenderOptimizationEnabled, value -> LegionsClient.CONFIG.playerRenderOptimizationEnabled = value);
        if (LegionsClient.CONFIG.playerRenderOptimizationEnabled) {
            y = legions_client$addIntSlider(leftX, y, controlWidth, "Render Distance", 16, 160, LegionsClient.CONFIG.playerRenderDistance, value -> LegionsClient.CONFIG.playerRenderDistance = value);
        }

        return y + 10;
    }

    @Unique
    private Integer legions_client$buildNativeLegionsModule(int y, int leftX, int controlWidth) {
        try {
            Class<?> screenClass = this.getClass();
            Class<?> toggleSetterType = Class.forName("com.atomics.client.gui.AtomicsClientScreen$ToggleSetter");
            Class<?> intSetterType = Class.forName("com.atomics.client.gui.AtomicsClientScreen$IntSetter");
            Method addFeatureSection = legions_client$getMethod(screenClass, "addFeatureSection", int.class, String.class, String.class);
            Method isFeatureCollapsed = legions_client$getMethod(screenClass, "isFeatureCollapsed", String.class);
            Method addToggle = legions_client$getMethod(screenClass, "addToggle",
                    int.class, int.class, int.class, String.class, boolean.class, boolean.class,
                    BooleanSupplier.class, toggleSetterType, boolean.class);
            Method addIntSlider = legions_client$getMethod(screenClass, "addIntSlider",
                    int.class, int.class, int.class, String.class, int.class, int.class, int.class,
                    int.class, int.class, intSetterType);
            Method addWideButton = legions_client$getMethod(screenClass, "addWideButton",
                    int.class, int.class, int.class, String.class, ButtonWidget.PressAction.class);

            int rowY = (Integer) addFeatureSection.invoke(this, y, LEGIONS_FEATURE_KEY, "Legions");
            if ((Boolean) isFeatureCollapsed.invoke(this, LEGIONS_FEATURE_KEY)) {
                return rowY + 10;
            }

            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Enable Legions Client", true, () -> LegionsClient.CONFIG.enabled, value -> LegionsClient.CONFIG.enabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Rating Nametags", true, () -> LegionsClient.CONFIG.ratingNametagsEnabled, value -> LegionsClient.CONFIG.ratingNametagsEnabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Foe Outlines", false, () -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled, value -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Spectator Glow", true, () -> LegionsClient.CONFIG.spectatorGlowEnabled, value -> LegionsClient.CONFIG.spectatorGlowEnabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Warning Particles", true, () -> LegionsClient.CONFIG.warningParticlesEnabled, value -> LegionsClient.CONFIG.warningParticlesEnabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Team Ping", true, () -> LegionsClient.CONFIG.teamPingEnabled, value -> LegionsClient.CONFIG.teamPingEnabled = value);
            if (LegionsClient.CONFIG.teamPingEnabled) {
                rowY = legions_client$addNativeIntSlider(addIntSlider, intSetterType, leftX, rowY, controlWidth,
                        "Ping Seconds", LegionsClient.CONFIG.pingDurationSeconds, 3, 10, 1, 10,
                        () -> LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value);
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Team Count Overlay", false, () -> LegionsClient.CONFIG.teamCountOverlayEnabled, value -> LegionsClient.CONFIG.teamCountOverlayEnabled = value);
            if (LegionsClient.CONFIG.teamCountOverlayEnabled) {
                rowY = legions_client$addNativeWideButton(addWideButton, leftX, rowY, controlWidth, "Move Team Count Overlay",
                        button -> this.client.setScreen(new LegionsTeamCountOverlayLayoutScreen(this)));
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Team HUD", true, () -> LegionsClient.CONFIG.teamHudEnabled, value -> LegionsClient.CONFIG.teamHudEnabled = value);
            if (LegionsClient.CONFIG.teamHudEnabled) {
                rowY = legions_client$addNativeWideButton(addWideButton, leftX, rowY, controlWidth, "Move Team HUD",
                        button -> this.client.setScreen(new LegionsTeamHudLayoutScreen(this)));
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Limit Opponents Shown", true, () -> LegionsClient.CONFIG.opponentLimitEnabled, value -> LegionsClient.CONFIG.opponentLimitEnabled = value);
            if (LegionsClient.CONFIG.opponentLimitEnabled) {
                rowY = legions_client$addNativeIntSlider(addIntSlider, intSetterType, leftX, rowY, controlWidth,
                        "Opponents Shown", LegionsClient.CONFIG.opponentLimit, 1, 12, 1, 5,
                        () -> LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value);
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Player Render Optimization", true, () -> LegionsClient.CONFIG.playerRenderOptimizationEnabled, value -> LegionsClient.CONFIG.playerRenderOptimizationEnabled = value);
            if (LegionsClient.CONFIG.playerRenderOptimizationEnabled) {
                rowY = legions_client$addNativeIntSlider(addIntSlider, intSetterType, leftX, rowY, controlWidth,
                        "Render Distance", LegionsClient.CONFIG.playerRenderDistance, 16, 160, 8, 64,
                        () -> LegionsClient.CONFIG.playerRenderDistance, value -> LegionsClient.CONFIG.playerRenderDistance = value);
            }

            return rowY + 10;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Falling back to Legions Atomics-style widgets.", e);
            return null;
        }
    }

    @Unique
    private int legions_client$addNativeToggle(Method addToggle, Class<?> toggleSetterType, int x, int y, int width,
                                               String label, boolean defaultValue, BooleanSupplier getter,
                                               Consumer<Boolean> setter) throws ReflectiveOperationException {
        addToggle.invoke(this, x, y, width, label, getter.getAsBoolean(), defaultValue, getter,
                legions_client$toggleSetter(toggleSetterType, getter, setter), false);
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addNativeIntSlider(Method addIntSlider, Class<?> intSetterType, int x, int y, int width,
                                                  String label, int current, int min, int max, int step, int defaultValue,
                                                  IntSupplier getter, IntConsumer setter) throws ReflectiveOperationException {
        addIntSlider.invoke(this, x, y, width, label, current, min, max, step, defaultValue,
                legions_client$intSetter(intSetterType, getter, setter));
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addNativeWideButton(Method addWideButton, int x, int y, int width, String label, ButtonWidget.PressAction action) throws ReflectiveOperationException {
        addWideButton.invoke(this, x, y, width, label, action);
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private Object legions_client$toggleSetter(Class<?> setterType, BooleanSupplier getter, Consumer<Boolean> setter) {
        return Proxy.newProxyInstance(setterType.getClassLoader(), new Class<?>[]{setterType}, (proxy, method, args) -> {
            if ("set".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Boolean value) {
                if (getter.getAsBoolean() != value) {
                    setter.accept(value);
                    LegionsClient.saveConfig();
                    clearAndInit();
                }
                return null;
            }
            return legions_client$proxyObjectMethod(proxy, method, args);
        });
    }

    @Unique
    private Object legions_client$intSetter(Class<?> setterType, IntSupplier getter, IntConsumer setter) {
        return Proxy.newProxyInstance(setterType.getClassLoader(), new Class<?>[]{setterType}, (proxy, method, args) -> {
            if ("set".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Integer value) {
                if (getter.getAsInt() != value) {
                    setter.accept(value);
                    LegionsClient.CONFIG.normalize();
                    LegionsClient.saveConfig();
                }
                return null;
            }
            return legions_client$proxyObjectMethod(proxy, method, args);
        });
    }

    @Unique
    private Object legions_client$proxyObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "LegionsClientAtomicsProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> args != null && args.length == 1 && proxy == args[0];
            default -> null;
        };
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
    private int legions_client$addMoveTeamHudButton(int x, int y, int width) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Move Team HUD"),
                    button -> this.client.setScreen(new LegionsTeamHudLayoutScreen(this)))
                    .dimensions(x, y, width, LEGIONS_BUTTON_HEIGHT).build());
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addMoveTeamCountButton(int x, int y, int width) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Move Team Count Overlay"),
                    button -> this.client.setScreen(new LegionsTeamCountOverlayLayoutScreen(this)))
                    .dimensions(x, y, width, LEGIONS_BUTTON_HEIGHT).build());
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
        String terms = "legions lc rating nametag foe outline spectator glow warning particle team ping team hud team counter team count player count scoreboard scoreboard teams left players left count move opponent opponents shown limit hidden hide render optimization distance fps performance opacity";
        return legions_client$matchesSearch(query, terms);
    }

    @Unique
    private boolean legions_client$matchesSearch(String query, String terms) {
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && !terms.contains(token)) {
                return false;
            }
        }
        return true;
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

    @Unique
    private static Method legions_client$getMethod(Class<?> owner, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}
