package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import com.legions.client.LegionsFeatures;
import com.legions.client.config.LegionsConfig;
import com.legions.client.gui.LegionsTeamCountOverlayLayoutScreen;
import com.legions.client.gui.LegionsTeamHudLayoutScreen;
import com.legions.client.gui.atomics.LegionsAtomicsIntSlider;
import com.legions.client.gui.atomics.LegionsAtomicsSectionHeaderWidget;
import com.legions.client.gui.atomics.LegionsAtomicsSubHeaderWidget;
import com.legions.client.gui.atomics.LegionsAtomicsToggleWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
    private static final LegionsConfig LEGIONS_DEFAULT_CONFIG = new LegionsConfig().normalize();
    @Unique
    private static final String[] FOE_RENDER_STYLE_LABELS = {"Full", "Outline", "Outline + Full", "Pulse"};

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

        y = legions_client$addSubHeader(leftX, y, controlWidth, "General");
        y = legions_client$addToggle(leftX, y, controlWidth, "Enable Legions Client", () -> LegionsClient.CONFIG.enabled, value -> LegionsClient.CONFIG.enabled = value, LEGIONS_DEFAULT_CONFIG.enabled);
        y = legions_client$addTextField(leftX, y, controlWidth, "Server IPs", LegionsFeatures.serverAddressesText(), "legions, play.example.net", LegionsFeatures::setServerAddressesText, String.join(", ", LEGIONS_DEFAULT_CONFIG.allowedServerAddresses));

        y = legions_client$addSubHeader(leftX, y, controlWidth, "Player Info");
        y = legions_client$addToggle(leftX, y, controlWidth, "Rating Nametags", () -> LegionsClient.CONFIG.ratingNametagsEnabled, value -> LegionsClient.CONFIG.ratingNametagsEnabled = value, LEGIONS_DEFAULT_CONFIG.ratingNametagsEnabled);
        y = legions_client$addToggle(leftX, y, controlWidth, "Foe Outlines", () -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled, value -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled = value, LEGIONS_DEFAULT_CONFIG.automaticFoeOutlinesEnabled);
        if (LegionsClient.CONFIG.automaticFoeOutlinesEnabled) {
            y = legions_client$addCycle(leftX, y, controlWidth, "Foe Render Style", () -> LegionsClient.CONFIG.automaticFoeRenderStyle, value -> LegionsClient.CONFIG.automaticFoeRenderStyle = value, LEGIONS_DEFAULT_CONFIG.automaticFoeRenderStyle, FOE_RENDER_STYLE_LABELS);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Spectator Glow", () -> LegionsClient.CONFIG.spectatorGlowEnabled, value -> LegionsClient.CONFIG.spectatorGlowEnabled = value, LEGIONS_DEFAULT_CONFIG.spectatorGlowEnabled);
        y = legions_client$addToggle(leftX, y, controlWidth, "Warning Particles", () -> LegionsClient.CONFIG.warningParticlesEnabled, value -> LegionsClient.CONFIG.warningParticlesEnabled = value, LEGIONS_DEFAULT_CONFIG.warningParticlesEnabled);

        y = legions_client$addSubHeader(leftX, y, controlWidth, "Team Ping");
        y = legions_client$addToggle(leftX, y, controlWidth, "Team Ping", () -> LegionsClient.CONFIG.teamPingEnabled, value -> LegionsClient.CONFIG.teamPingEnabled = value, LEGIONS_DEFAULT_CONFIG.teamPingEnabled);
        if (LegionsClient.CONFIG.teamPingEnabled) {
            y = legions_client$addToggle(leftX, y, controlWidth, "Ping Last Attacked", () -> LegionsClient.CONFIG.pingLastAttackedPlayerEnabled, value -> LegionsClient.CONFIG.pingLastAttackedPlayerEnabled = value, LEGIONS_DEFAULT_CONFIG.pingLastAttackedPlayerEnabled);
            y = legions_client$addToggle(leftX, y, controlWidth, "Block Ping Distance", () -> LegionsClient.CONFIG.blockPingDistanceLabelEnabled, value -> LegionsClient.CONFIG.blockPingDistanceLabelEnabled = value, LEGIONS_DEFAULT_CONFIG.blockPingDistanceLabelEnabled);
            y = legions_client$addIntSlider(leftX, y, controlWidth, "Ping Seconds", 1, 25, LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value, LEGIONS_DEFAULT_CONFIG.pingDurationSeconds);
        }

        y = legions_client$addSubHeader(leftX, y, controlWidth, "Overlays");
        y = legions_client$addToggle(leftX, y, controlWidth, "Team Count Overlay", () -> LegionsClient.CONFIG.teamCountOverlayEnabled, value -> LegionsClient.CONFIG.teamCountOverlayEnabled = value, LEGIONS_DEFAULT_CONFIG.teamCountOverlayEnabled);
        if (LegionsClient.CONFIG.teamCountOverlayEnabled) {
            y = legions_client$addMoveTeamCountButton(leftX, y, controlWidth);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Team HUD", () -> LegionsClient.CONFIG.teamHudEnabled, value -> LegionsClient.CONFIG.teamHudEnabled = value, LEGIONS_DEFAULT_CONFIG.teamHudEnabled);
        if (LegionsClient.CONFIG.teamHudEnabled) {
            y = legions_client$addMoveTeamHudButton(leftX, y, controlWidth);
        }

        y = legions_client$addSubHeader(leftX, y, controlWidth, "Player Visibility");
        y = legions_client$addToggle(leftX, y, controlWidth, "Limit Opponents Shown", () -> LegionsClient.CONFIG.opponentLimitEnabled, value -> LegionsClient.CONFIG.opponentLimitEnabled = value, LEGIONS_DEFAULT_CONFIG.opponentLimitEnabled);
        if (LegionsClient.CONFIG.opponentLimitEnabled) {
            y = legions_client$addIntSlider(leftX, y, controlWidth, "Opponents Shown", 1, 20, LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value, LEGIONS_DEFAULT_CONFIG.opponentLimit);
        }
        y = legions_client$addToggle(leftX, y, controlWidth, "Player Render Optimization", () -> LegionsClient.CONFIG.playerRenderOptimizationEnabled, value -> LegionsClient.CONFIG.playerRenderOptimizationEnabled = value, LEGIONS_DEFAULT_CONFIG.playerRenderOptimizationEnabled);
        if (LegionsClient.CONFIG.playerRenderOptimizationEnabled) {
            y = legions_client$addIntSlider(leftX, y, controlWidth, "Render Distance (Blocks)", 16, 160, LegionsClient.CONFIG.playerRenderDistance, value -> LegionsClient.CONFIG.playerRenderDistance = value, LEGIONS_DEFAULT_CONFIG.playerRenderDistance);
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
            Method addTextField = legions_client$getMethod(screenClass, "addTextField",
                    int.class, int.class, int.class, String.class, String.class, String.class, Consumer.class);
            Method addWideButton = legions_client$getMethod(screenClass, "addWideButton",
                    int.class, int.class, int.class, String.class, ButtonWidget.PressAction.class);

            int rowY = (Integer) addFeatureSection.invoke(this, y, LEGIONS_FEATURE_KEY, "Legions");
            if ((Boolean) isFeatureCollapsed.invoke(this, LEGIONS_FEATURE_KEY)) {
                return rowY + 10;
            }

            rowY = legions_client$addSubHeader(leftX, rowY, controlWidth, "General");
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Enable Legions Client", LEGIONS_DEFAULT_CONFIG.enabled, () -> LegionsClient.CONFIG.enabled, value -> LegionsClient.CONFIG.enabled = value);
            rowY = legions_client$addNativeTextField(addTextField, leftX, rowY, controlWidth,
                    "Server IPs", LegionsFeatures.serverAddressesText(), "legions, play.example.net", LegionsFeatures::setServerAddressesText);

            rowY = legions_client$addSubHeader(leftX, rowY, controlWidth, "Player Info");
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Rating Nametags", LEGIONS_DEFAULT_CONFIG.ratingNametagsEnabled, () -> LegionsClient.CONFIG.ratingNametagsEnabled, value -> LegionsClient.CONFIG.ratingNametagsEnabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Foe Outlines", LEGIONS_DEFAULT_CONFIG.automaticFoeOutlinesEnabled, () -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled, value -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled = value);
            if (LegionsClient.CONFIG.automaticFoeOutlinesEnabled) {
                rowY = legions_client$addNativeCycle(addWideButton, leftX, rowY, controlWidth,
                        "Foe Render Style", () -> LegionsClient.CONFIG.automaticFoeRenderStyle, value -> LegionsClient.CONFIG.automaticFoeRenderStyle = value, FOE_RENDER_STYLE_LABELS);
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Spectator Glow", LEGIONS_DEFAULT_CONFIG.spectatorGlowEnabled, () -> LegionsClient.CONFIG.spectatorGlowEnabled, value -> LegionsClient.CONFIG.spectatorGlowEnabled = value);
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Warning Particles", LEGIONS_DEFAULT_CONFIG.warningParticlesEnabled, () -> LegionsClient.CONFIG.warningParticlesEnabled, value -> LegionsClient.CONFIG.warningParticlesEnabled = value);

            rowY = legions_client$addSubHeader(leftX, rowY, controlWidth, "Team Ping");
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Team Ping", LEGIONS_DEFAULT_CONFIG.teamPingEnabled, () -> LegionsClient.CONFIG.teamPingEnabled, value -> LegionsClient.CONFIG.teamPingEnabled = value);
            if (LegionsClient.CONFIG.teamPingEnabled) {
                rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                        "Ping Last Attacked", LEGIONS_DEFAULT_CONFIG.pingLastAttackedPlayerEnabled, () -> LegionsClient.CONFIG.pingLastAttackedPlayerEnabled, value -> LegionsClient.CONFIG.pingLastAttackedPlayerEnabled = value);
                rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                        "Block Ping Distance", LEGIONS_DEFAULT_CONFIG.blockPingDistanceLabelEnabled, () -> LegionsClient.CONFIG.blockPingDistanceLabelEnabled, value -> LegionsClient.CONFIG.blockPingDistanceLabelEnabled = value);
                rowY = legions_client$addNativeIntSlider(addIntSlider, intSetterType, leftX, rowY, controlWidth,
                        "Ping Seconds", LegionsClient.CONFIG.pingDurationSeconds, 1, 25, 1, LEGIONS_DEFAULT_CONFIG.pingDurationSeconds,
                        () -> LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value);
            }

            rowY = legions_client$addSubHeader(leftX, rowY, controlWidth, "Overlays");
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Team Count Overlay", LEGIONS_DEFAULT_CONFIG.teamCountOverlayEnabled, () -> LegionsClient.CONFIG.teamCountOverlayEnabled, value -> LegionsClient.CONFIG.teamCountOverlayEnabled = value);
            if (LegionsClient.CONFIG.teamCountOverlayEnabled) {
                rowY = legions_client$addNativeWideButton(addWideButton, leftX, rowY, controlWidth, "Move Team Count Overlay",
                        button -> this.client.setScreen(new LegionsTeamCountOverlayLayoutScreen(this)));
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Team HUD", LEGIONS_DEFAULT_CONFIG.teamHudEnabled, () -> LegionsClient.CONFIG.teamHudEnabled, value -> LegionsClient.CONFIG.teamHudEnabled = value);
            if (LegionsClient.CONFIG.teamHudEnabled) {
                rowY = legions_client$addNativeWideButton(addWideButton, leftX, rowY, controlWidth, "Move Team HUD",
                        button -> this.client.setScreen(new LegionsTeamHudLayoutScreen(this)));
            }

            rowY = legions_client$addSubHeader(leftX, rowY, controlWidth, "Player Visibility");
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Limit Opponents Shown", LEGIONS_DEFAULT_CONFIG.opponentLimitEnabled, () -> LegionsClient.CONFIG.opponentLimitEnabled, value -> LegionsClient.CONFIG.opponentLimitEnabled = value);
            if (LegionsClient.CONFIG.opponentLimitEnabled) {
                rowY = legions_client$addNativeIntSlider(addIntSlider, intSetterType, leftX, rowY, controlWidth,
                        "Opponents Shown", LegionsClient.CONFIG.opponentLimit, 1, 20, 1, LEGIONS_DEFAULT_CONFIG.opponentLimit,
                        () -> LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value);
            }
            rowY = legions_client$addNativeToggle(addToggle, toggleSetterType, leftX, rowY, controlWidth,
                    "Player Render Optimization", LEGIONS_DEFAULT_CONFIG.playerRenderOptimizationEnabled, () -> LegionsClient.CONFIG.playerRenderOptimizationEnabled, value -> LegionsClient.CONFIG.playerRenderOptimizationEnabled = value);
            if (LegionsClient.CONFIG.playerRenderOptimizationEnabled) {
                rowY = legions_client$addNativeIntSlider(addIntSlider, intSetterType, leftX, rowY, controlWidth,
                        "Render Distance (Blocks)", LegionsClient.CONFIG.playerRenderDistance, 16, 160, 8, LEGIONS_DEFAULT_CONFIG.playerRenderDistance,
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
    private int legions_client$addNativeTextField(Method addTextField, int x, int y, int width, String label,
                                                 String value, String placeholder, Consumer<String> setter) throws ReflectiveOperationException {
        addTextField.invoke(this, x, y, width, label, value, placeholder, setter);
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addNativeWideButton(Method addWideButton, int x, int y, int width, String label, ButtonWidget.PressAction action) throws ReflectiveOperationException {
        addWideButton.invoke(this, x, y, width, label, action);
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addNativeCycle(Method addWideButton, int x, int y, int width, String label,
                                             IntSupplier getter, IntConsumer setter, String[] labels) throws ReflectiveOperationException {
        addWideButton.invoke(this, x, y, width, cycleText(label, getter.getAsInt(), labels).getString(), (ButtonWidget.PressAction) button -> {
            int value = Math.floorMod(getter.getAsInt() + 1, labels.length);
            setter.accept(value);
            LegionsClient.CONFIG.normalize();
            LegionsClient.saveConfig();
            clearAndInit();
        });
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
    private int legions_client$addToggle(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter, boolean defaultValue) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(new LegionsAtomicsToggleWidget(this.textRenderer, x, y, width, LEGIONS_BUTTON_HEIGHT, label, getter.getAsBoolean(), () -> {
                boolean value = !getter.getAsBoolean();
                setter.accept(value);
                LegionsClient.saveConfig();
                clearAndInit();
            }));
            legions_client$addResetButton(x, y, width, getter.getAsBoolean() != defaultValue, () -> {
                setter.accept(defaultValue);
                LegionsClient.saveConfig();
                clearAndInit();
            });
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addCycle(int x, int y, int width, String label, IntSupplier getter,
                                        IntConsumer setter, int defaultValue, String[] labels) {
        if (legions_client$isWidgetVisible(y) && labels.length > 0) {
            addDrawableChild(ButtonWidget.builder(cycleText(label, getter.getAsInt(), labels), button -> {
                int value = Math.floorMod(getter.getAsInt() + 1, labels.length);
                setter.accept(value);
                LegionsClient.CONFIG.normalize();
                LegionsClient.saveConfig();
                clearAndInit();
            }).dimensions(x, y, width, LEGIONS_BUTTON_HEIGHT).build());
            legions_client$addResetButton(x, y, width, getter.getAsInt() != defaultValue, () -> {
                setter.accept(defaultValue);
                LegionsClient.CONFIG.normalize();
                LegionsClient.saveConfig();
                clearAndInit();
            });
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addSubHeader(int x, int y, int width, String label) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(new LegionsAtomicsSubHeaderWidget(this.textRenderer, x, y, width, LEGIONS_BUTTON_HEIGHT, label));
        }
        return y + LEGIONS_SECTION_HEIGHT;
    }

    @Unique
    private int legions_client$addIntSlider(int x, int y, int width, String label, int min, int max, int initial, IntConsumer setter, int defaultValue) {
        if (legions_client$isWidgetVisible(y)) {
            addDrawableChild(new LegionsAtomicsIntSlider(x, y, width, LEGIONS_BUTTON_HEIGHT, label, min, max, initial, setter));
            legions_client$addResetButton(x, y, width, initial != defaultValue, () -> {
                setter.accept(defaultValue);
                LegionsClient.CONFIG.normalize();
                LegionsClient.saveConfig();
                clearAndInit();
            });
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private int legions_client$addTextField(int x, int y, int width, String label, String value, String placeholder,
                                           Consumer<String> setter, String defaultValue) {
        if (legions_client$isWidgetVisible(y)) {
            TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, width, LEGIONS_BUTTON_HEIGHT, Text.literal(label));
            field.setText(value);
            field.setPlaceholder(Text.literal(placeholder));
            field.setChangedListener(nextValue -> {
                setter.accept(nextValue);
                LegionsClient.saveConfig();
            });
            addDrawableChild(field);
            legions_client$addResetButton(x, y, width, !value.equals(defaultValue), () -> {
                setter.accept(defaultValue);
                clearAndInit();
            });
        }
        return y + LEGIONS_ROW_HEIGHT;
    }

    @Unique
    private void legions_client$addResetButton(int x, int y, int width, boolean visible, Runnable action) {
        if (visible) {
            addDrawableChild(ButtonWidget.builder(Text.literal("R"), button -> action.run())
                    .dimensions(x + width + 6, y, LEGIONS_RESET_WIDTH, LEGIONS_BUTTON_HEIGHT).build());
        }
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
        String terms = "legions lc rating nametag foe outline render style full pulse spectator glow warning particle team ping last attacked attack block distance label team hud team counter team count player count scoreboard scoreboard teams left players left count move opponent opponents shown limit hidden hide render optimization distance fps performance opacity";
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

    @Unique
    private static Text cycleText(String label, int value, String[] labels) {
        return Text.literal(label + "        " + labels[Math.floorMod(value, labels.length)]);
    }
}
