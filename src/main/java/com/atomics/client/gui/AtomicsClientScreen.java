package com.atomics.client.gui;

import com.atomics.client.TotemPopEffects;
import com.atomics.client.AtomicsClient;
import com.atomics.client.DualSpectateCamera;
import com.atomics.client.AutoVillagerTradeCatalog;
import com.atomics.client.config.TpsConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class AtomicsClientScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 96;
    private static final int FOOTER_HEIGHT = 32;
    private static final int OUTER_MARGIN = 18;
    private static final int COLUMN_GAP = 16;
    private static final int ROW_HEIGHT = 28;
    private static final int SECTION_HEIGHT = 24;
    private static final int RESET_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 22;
    private static final int PREVIEW_MIN_WIDTH = 300;
    private static final int PANEL_BORDER = AtomicsGuiStyle.PANEL_BORDER;
    private static final int ACCENT = AtomicsGuiStyle.ACCENT;
    private static final int ACCENT_SOFT = AtomicsGuiStyle.ACCENT_SOFT;
    private static final int TEXT_MAIN = AtomicsGuiStyle.TEXT;
    private static final int TEXT_MUTED = AtomicsGuiStyle.TEXT_MUTED;

    private final Screen parent;
    private final List<DrawLabel> labels = new ArrayList<>();

    private Tab selectedTab = Tab.TOTEM;
    private boolean stateLoaded;
    private boolean enabled;
    private boolean particlesEnabled;
    private boolean soundsEnabled;
    private boolean handScaleEnabled;
    private boolean droppedScaleEnabled;
    private boolean popScaleEnabled;
    private boolean retextureEnabled;
    private boolean totemOverlayEnabled;
    private boolean autoPreview;

    private String replacementItemId;
    private float handScale;
    private float droppedScale;
    private float popScale;
    private int animationTicks;
    private float totemOverlayHue;
    private float totemOverlayAlpha;
    private String projectileTrailParticleId;

    private boolean shieldDownEnabled;
    private boolean shieldUpEnabled;
    private boolean shieldWarningOverlayEnabled;
    private boolean fireOverlayEnabled;
    private boolean emptyBucketOverlayEnabled;
    private boolean winOddsEnabled;
    private boolean totemPopNametagEnabled;
    private boolean opponentStatsNametagEnabled;
    private boolean pingNametagEnabled;
    private boolean dualSpectateEnabled;
    private boolean dualSpectateAutoFill;
    private boolean dualSpectateForceThirdPerson;
    private boolean dualSpectateOverheadEnabled;
    private boolean friendFoeOverlayEnabled;
    private boolean teamCountOverlayEnabled;
    private boolean teamCountOverlayServerFilterEnabled;
    private boolean reachDisplayEnabled;
    private boolean opponentInfoEnabled;
    private boolean fullBrightEnabled;
    private boolean armorHudEnabled;
    private boolean armorHudAutoPosition;
    private boolean armorDurabilityWarningEnabled;
    private boolean timeChangerEnabled;
    private boolean tntTimerEnabled;
    private boolean projectileTrailEnabled;
    private boolean foodOverlayEnabled;
    private boolean partialStatusIconsEnabled;
    private boolean streamerModeEnabled;
    private boolean zoomEnabled;
    private boolean freelookEnabled;
    private boolean freelookToggleMode;
    private boolean chatMacrosEnabled;
    private boolean autoVillagerTraderEnabled;
    private boolean autoVillagerTraderCheckChests;
    private boolean autoVillagerTraderAutoCloseMerchant;
    private boolean villagerLevelerAutoDropItems;
    private String settingsSearch = "";
    private final List<String> macroMessages = new ArrayList<>();
    private final List<String> nametagItemOrder = new ArrayList<>();
    private final List<String> nametagItemsBeforeName = new ArrayList<>();
    private String opponentStatsNametagFormat;
    private String friendFoeOverlayStyle;
    private String dualSpectatePlayerOne;
    private String dualSpectatePlayerTwo;
    private String teamCountOverlayAllowedServers;
    private String autoVillagerTraderProfession;
    private String autoVillagerTraderTrade;
    private String autoVillagerTraderEnchantment;
    private int timeOfDay;
    private int tntTimerRange;
    private int armorDurabilityWarningPercent;
    private int armorHudX;
    private int armorHudY;
    private int teamCountOverlayX;
    private int teamCountOverlayY;
    private boolean armorHudVertical;
    private int armorHudSpacing;
    private boolean armorHudHotbarBorder;
    private String armorHudDurabilityMode;
    private int friendOverlayR;
    private int friendOverlayG;
    private int friendOverlayB;
    private int foeOverlayR;
    private int foeOverlayG;
    private int foeOverlayB;
    private int shieldWarningOverlayR;
    private int shieldWarningOverlayG;
    private int shieldWarningOverlayB;
    private int emptyBucketOverlayR;
    private int emptyBucketOverlayG;
    private int emptyBucketOverlayB;
    private int projectileTrailParticleCount;
    private float dualSpectatePadding;
    private float dualSpectateMinDistance;
    private float dualSpectateMaxDistance;
    private float dualSpectateOverheadGroupDistance;
    private float dualSpectateMaxYDifference;
    private float friendOverlayAlpha;
    private float foeOverlayAlpha;
    private float shieldWarningOverlayAlpha;
    private float emptyBucketOverlayAlpha;
    private float zoomMultiplier;
    private float shieldDownX;
    private float shieldDownY;
    private float shieldDownZ;
    private float shieldDownRotX;
    private float shieldDownRotY;
    private float shieldDownRotZ;
    private float shieldUpX;
    private float shieldUpY;
    private float shieldUpZ;
    private float shieldUpRotX;
    private float shieldUpRotY;
    private float shieldUpRotZ;
    private float fireOverlayHeight;
    private double projectileTrailSpread;
    private double projectileTrailSpeed;
    private double autoVillagerTraderRange;

    private boolean initializing;
    private boolean settingsSearchFocused;
    private boolean searchRefreshQueued;
    private String openDropdownKey;
    private KeyBinding listeningKeyBinding;
    private String listeningKeyLabel;
    private final List<String> collapsedSections = new ArrayList<>();
    private int scrollOffset;
    private int maxScroll;
    private int previewCooldown;
    private int dualSpectateAutoFillRefreshTicks;
    private Text status = Text.empty();
    private TextFieldWidget settingsSearchField;

    private int contentTop;
    private int contentBottom;
    private int leftX;
    private int leftWidth;
    private int previewX;
    private int previewWidth;

    public AtomicsClientScreen(Screen parent) {
        super(Text.literal("Atomics Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        initializing = true;
        labels.clear();
        if (!stateLoaded) loadStateFromConfig();

        contentTop = TOP_BAR_HEIGHT + 8;
        contentBottom = this.height - FOOTER_HEIGHT;
        leftX = OUTER_MARGIN;
        if (!selectedTab.hasPreview) {
            // Full-list tabs use the whole settings area.
            previewWidth = 0;
            leftWidth = Math.max(260, this.width - OUTER_MARGIN * 2);
            previewX = this.width + 100;
        } else {
            previewWidth = Math.max(PREVIEW_MIN_WIDTH, Math.min(420, this.width / 3));
            leftWidth = Math.max(260, this.width - OUTER_MARGIN * 2 - COLUMN_GAP - previewWidth);
            previewX = leftX + leftWidth + COLUMN_GAP;
        }

        addTopTabs();

        int y = contentTop - scrollOffset;
        if (selectedTab == Tab.TOTEM) {
            y = buildTotemSettings(y);
        } else if (selectedTab == Tab.PVP) {
            y = buildPvpSettings(y);
        } else if (selectedTab == Tab.STATS) {
            y = buildStatsDashboard(y);
        } else if (selectedTab == Tab.MISC) {
            y = buildMiscSettings(y);
        } else if (selectedTab == Tab.TOOLS) {
            y = buildToolsSettings(y);
        } else if (selectedTab == Tab.KEYBINDS) {
            y = buildKeybindSettings(y);
        } else if (selectedTab == Tab.SEARCH) {
            y = buildSearchSettings(y);
        } else {
            y = buildPlaceholderSettings(y, selectedTab.label, "No settings available yet.");
        }

        int contentHeight = y + scrollOffset - contentTop + 12;
        maxScroll = Math.max(0, contentHeight - (contentBottom - contentTop));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        if (selectedTab == Tab.SEARCH) {
            addSettingsSearchField();
        } else {
            settingsSearchFocused = false;
            settingsSearchField = null;
        }

        addFooterButtons();
        initializing = false;
    }

    private void addFooterButtons() {
        int footerY = this.height - 28;
        int gap = 8;
        int buttonCount = selectedTab == Tab.TOTEM ? 4 : 3;
        int buttonW = Math.min(120, (this.width - OUTER_MARGIN * 2 - gap * (buttonCount - 1)) / buttonCount);
        int totalW = buttonW * buttonCount + gap * (buttonCount - 1);
        int startX = this.width / 2 - totalW / 2;
        int x = startX;
        if (selectedTab == Tab.TOTEM) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Preview Now"), b -> previewNow()).dimensions(x, footerY, buttonW, BUTTON_HEIGHT).build());
            x += buttonW + gap;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save()).dimensions(x, footerY, buttonW, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset All"), b -> resetAll()).dimensions(x + buttonW + gap, footerY, buttonW, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(x + (buttonW + gap) * 2, footerY, buttonW, BUTTON_HEIGHT).build());
    }

    private void addTopTabs() {
        Tab[] tabs = Tab.values();
        int available = Math.max(1, this.width - OUTER_MARGIN * 2);
        int gap = available < 560 ? 4 : 8;
        int tabW = Math.max(58, Math.min(104, (available - gap * (tabs.length - 1)) / tabs.length));
        int tabH = 26;
        int total = tabW * tabs.length + gap * (tabs.length - 1);
        int x = this.width / 2 - total / 2;
        int y = 38;
        for (int i = 0; i < tabs.length; i++) {
            addTabButton(tabs[i], x + (tabW + gap) * i, y, tabW, tabH);
        }
    }

    private void addTabButton(Tab tab, int x, int y, int w, int h) {
        addDrawableChild(new TabButtonWidget(x, y, w, h, tab.label, selectedTab == tab, () -> {
            selectedTab = tab;
            scrollOffset = 0;
            clearAndInit();
        }));
    }

    private void addSettingsSearchField() {
        int width = Math.min(320, Math.max(180, this.width / 3));
        int x = this.width / 2 - width / 2;
        int y = 68;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, width, BUTTON_HEIGHT, Text.literal("Search settings"));
        settingsSearchField = field;
        field.setText(settingsSearch == null ? "" : settingsSearch);
        field.setPlaceholder(Text.literal("Search settings").formatted(Formatting.DARK_GRAY));
        field.setChangedListener(value -> {
            settingsSearch = value;
            scrollOffset = 0;
            settingsSearchFocused = true;
            searchRefreshQueued = true;
        });
        addDrawableChild(field);
        if (settingsSearchFocused) focusSettingsSearchField();
    }

    private void focusSettingsSearchField() {
        if (settingsSearchField == null) return;
        settingsSearchField.setFocused(true);
        setFocused(settingsSearchField);
    }

    private int buildTotemSettings(int y) {
        int controlWidth = leftWidth - RESET_WIDTH - 6;
        int startY = y;

        if (shouldShowFeature("totem.general", "General", "mod enabled", "auto preview")) {
            y = addFeatureSection(y, "totem.general", "General");
            if (!isFeatureCollapsed("totem.general")) {
                addToggle(leftX, y, controlWidth, "Mod Enabled", enabled, true, () -> enabled, value -> enabled = value, false); y += ROW_HEIGHT;
                addToggle(leftX, y, controlWidth, "Auto Preview", autoPreview, false, () -> autoPreview, value -> autoPreview = value, false); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("totem.pop_overlay", "Pop Overlay Size", "overlay scale", "duration")) {
            y = addFeatureSection(y, "totem.pop_overlay", "Pop Overlay Size");
            if (!isFeatureCollapsed("totem.pop_overlay")) {
                addToggle(leftX, y, controlWidth, "Enable Pop Overlay Size", popScaleEnabled, false, () -> popScaleEnabled, value -> popScaleEnabled = value, true); y += ROW_HEIGHT;
                if (popScaleEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Overlay Scale", popScale, 0.01, 3.0, 0.01, TpsConfig.DEFAULT_POP_SCALE, value -> popScale = (float) value, value -> formatDecimal(value, 2) + "x"); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Overlay Duration", animationTicks, 5, 120, 1, TpsConfig.DEFAULT_POP_ANIMATION_TICKS, value -> animationTicks = value); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("totem.held_size", "Held Totem Size", "held scale")) {
            y = addFeatureSection(y, "totem.held_size", "Held Totem Size");
            if (!isFeatureCollapsed("totem.held_size")) {
                addToggle(leftX, y, controlWidth, "Enable Held Totem Size", handScaleEnabled, false, () -> handScaleEnabled, value -> handScaleEnabled = value, true); y += ROW_HEIGHT;
                if (handScaleEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Held Scale", handScale, 0.01, 3.0, 0.01, TpsConfig.DEFAULT_HAND_SCALE, value -> handScale = (float) value, value -> formatDecimal(value, 2) + "x"); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("totem.dropped_size", "Dropped Totem Size", "dropped scale")) {
            y = addFeatureSection(y, "totem.dropped_size", "Dropped Totem Size");
            if (!isFeatureCollapsed("totem.dropped_size")) {
                addToggle(leftX, y, controlWidth, "Enable Dropped Totem Size", droppedScaleEnabled, false, () -> droppedScaleEnabled, value -> droppedScaleEnabled = value, true); y += ROW_HEIGHT;
                if (droppedScaleEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Dropped Scale", droppedScale, 0.01, 3.0, 0.01, TpsConfig.DEFAULT_DROPPED_SCALE, value -> droppedScale = (float) value, value -> formatDecimal(value, 2) + "x"); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("totem.particles", "Particles", "totem particles", "particle list")) {
            y = addFeatureSection(y, "totem.particles", "Particles");
            if (!isFeatureCollapsed("totem.particles")) {
                addToggle(leftX, y, controlWidth, "Enable Particles", particlesEnabled, true, () -> particlesEnabled, value -> particlesEnabled = value, true); y += ROW_HEIGHT;
                if (particlesEnabled) {
                    addWideButton(leftX, y, controlWidth, "Edit Particle List (" + getParticleCount() + ")", b -> this.client.setScreen(new ParticleListScreen(this))); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("totem.sounds", "Sounds", "totem sounds", "sound list")) {
            y = addFeatureSection(y, "totem.sounds", "Sounds");
            if (!isFeatureCollapsed("totem.sounds")) {
                addToggle(leftX, y, controlWidth, "Enable Sounds", soundsEnabled, true, () -> soundsEnabled, value -> soundsEnabled = value, true); y += ROW_HEIGHT;
                if (soundsEnabled) {
                    addWideButton(leftX, y, controlWidth, "Edit Sound List (" + getSoundCount() + ")", b -> this.client.setScreen(new SoundListScreen(this))); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("totem.retexture", "Retexture", "replacement item")) {
            y = addFeatureSection(y, "totem.retexture", "Retexture");
            if (!isFeatureCollapsed("totem.retexture")) {
                addToggle(leftX, y, controlWidth, "Enable Retexture", retextureEnabled, false, () -> retextureEnabled, value -> retextureEnabled = value, true); y += ROW_HEIGHT;
                if (retextureEnabled) {
                    addTextField(leftX, y, controlWidth, "Replacement Item ID", replacementItemId, TpsConfig.DEFAULT_RETEXTURE_ITEM_ID, value -> replacementItemId = value); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("totem.color_overlay", "Totem Texture Color Overlay", "hue", "opacity")) {
            y = addFeatureSection(y, "totem.color_overlay", "Totem Texture Color Overlay");
            if (!isFeatureCollapsed("totem.color_overlay")) {
                addToggle(leftX, y, controlWidth, "Enable Totem Color Overlay", totemOverlayEnabled, TpsConfig.DEFAULT_TOTEM_COLOR_OVERLAY_ENABLED, () -> totemOverlayEnabled, value -> totemOverlayEnabled = value, true); y += ROW_HEIGHT;
                if (totemOverlayEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Hue Adjustment", totemOverlayHue, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_TOTEM_OVERLAY_HUE, value -> totemOverlayHue = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Overlay Opacity", totemOverlayAlpha, 0.0, 1.0, 0.025, TpsConfig.DEFAULT_TOTEM_OVERLAY_ALPHA, value -> totemOverlayAlpha = (float) value, value -> formatPercent(value)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (selectedTab != Tab.SEARCH && !normalizeSearch(settingsSearch).isEmpty() && y == startY) {
            labels.add(new DrawLabel("No settings matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }
        return y;
    }

    private int buildMiscSettings(int y) {
        int controlWidth = leftWidth - RESET_WIDTH - 6;
        int startY = y;

        if (shouldShowFeature("misc.shield_down", "Shield Position - Down", "shield down", "hand position")) {
            y = addFeatureSection(y, "misc.shield_down", "Shield Position - Down");
            if (!isFeatureCollapsed("misc.shield_down")) {
                addToggle(leftX, y, controlWidth, "Enable Shield Down Adjustments", shieldDownEnabled, TpsConfig.DEFAULT_MISC_SHIELD_DOWN_ENABLED, () -> shieldDownEnabled, value -> shieldDownEnabled = value, true); y += ROW_HEIGHT;
                if (shieldDownEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Shield Down X", shieldDownX, -2.0, 2.0, 0.025, TpsConfig.DEFAULT_SHIELD_DOWN_X, value -> shieldDownX = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Down Y", shieldDownY, -2.0, 2.0, 0.025, TpsConfig.DEFAULT_SHIELD_DOWN_Y, value -> shieldDownY = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Down Z", shieldDownZ, -2.0, 2.0, 0.025, TpsConfig.DEFAULT_SHIELD_DOWN_Z, value -> shieldDownZ = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Down Pitch", shieldDownRotX, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_SHIELD_DOWN_ROT_X, value -> shieldDownRotX = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Down Yaw", shieldDownRotY, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_SHIELD_DOWN_ROT_Y, value -> shieldDownRotY = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Down Roll", shieldDownRotZ, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_SHIELD_DOWN_ROT_Z, value -> shieldDownRotZ = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("misc.shield_up", "Shield Position - Up / Blocking", "shield up", "blocking")) {
            y = addFeatureSection(y, "misc.shield_up", "Shield Position - Up / Blocking");
            if (!isFeatureCollapsed("misc.shield_up")) {
                addToggle(leftX, y, controlWidth, "Enable Shield Up Adjustments", shieldUpEnabled, TpsConfig.DEFAULT_MISC_SHIELD_UP_ENABLED, () -> shieldUpEnabled, value -> shieldUpEnabled = value, true); y += ROW_HEIGHT;
                if (shieldUpEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Shield Up X", shieldUpX, -2.0, 2.0, 0.025, TpsConfig.DEFAULT_SHIELD_UP_X, value -> shieldUpX = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Up Y", shieldUpY, -2.0, 2.0, 0.025, TpsConfig.DEFAULT_SHIELD_UP_Y, value -> shieldUpY = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Up Z", shieldUpZ, -2.0, 2.0, 0.025, TpsConfig.DEFAULT_SHIELD_UP_Z, value -> shieldUpZ = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Up Pitch", shieldUpRotX, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_SHIELD_UP_ROT_X, value -> shieldUpRotX = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Up Yaw", shieldUpRotY, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_SHIELD_UP_ROT_Y, value -> shieldUpRotY = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Shield Up Roll", shieldUpRotZ, -180.0, 180.0, 1.0, TpsConfig.DEFAULT_SHIELD_UP_ROT_Z, value -> shieldUpRotZ = (float) value, value -> formatDegrees(value)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("misc.empty_bucket", "Empty Bucket Overlay", "bucket", "empty bucket")) {
            y = addFeatureSection(y, "misc.empty_bucket", "Empty Bucket Overlay");
            if (!isFeatureCollapsed("misc.empty_bucket")) {
                addToggle(leftX, y, controlWidth, "Enable Empty Bucket Overlay", emptyBucketOverlayEnabled, TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_ENABLED, () -> emptyBucketOverlayEnabled, value -> emptyBucketOverlayEnabled = value, true); y += ROW_HEIGHT;
                if (emptyBucketOverlayEnabled) {
                    addIntSlider(leftX, y, controlWidth, "Bucket Red", emptyBucketOverlayR, 0, 255, 1, TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_R, value -> emptyBucketOverlayR = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Bucket Green", emptyBucketOverlayG, 0, 255, 1, TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_G, value -> emptyBucketOverlayG = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Bucket Blue", emptyBucketOverlayB, 0, 255, 1, TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_B, value -> emptyBucketOverlayB = value); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Bucket Opacity", emptyBucketOverlayAlpha, 0.0, 1.0, 0.025, TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_ALPHA, value -> emptyBucketOverlayAlpha = (float) value, value -> formatPercent(value)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("misc.inventory_sorter", "Inventory Sorter", "kit sort", "server inventory", "sort inventory")) {
            y = addFeatureSection(y, "misc.inventory_sorter", "Inventory Sorter");
            if (!isFeatureCollapsed("misc.inventory_sorter")) {
                addWideButton(leftX, y, controlWidth, "Edit Kit Sorts (" + getInventorySortKitCount() + ")", b -> this.client.setScreen(new InventorySortScreen(this))); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("misc.auto_villager_trader", "Auto Villager Trader", "villager", "trade", "chest", "survival", "leveler", "drop")) {
            y = addFeatureSection(y, "misc.auto_villager_trader", "Auto Villager Trader");
            if (!isFeatureCollapsed("misc.auto_villager_trader")) {
                addToggle(leftX, y, controlWidth, "Enable Auto Trader", autoVillagerTraderEnabled, TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_ENABLED, () -> autoVillagerTraderEnabled, value -> autoVillagerTraderEnabled = value, true); y += ROW_HEIGHT;
                if (autoVillagerTraderEnabled) {
                    normalizeAutoVillagerTraderSelection();
                    y = addDropdown(leftX, y, controlWidth, "auto_trader.profession", "Villager Type", AutoVillagerTradeCatalog.professions(), autoVillagerTraderProfession, TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_PROFESSION, value -> {
                        autoVillagerTraderProfession = value;
                        autoVillagerTraderTrade = AutoVillagerTradeCatalog.profession(value).trades().get(0).id();
                    });
                    y = addDropdown(leftX, y, controlWidth, "auto_trader.trade", "Trade Type", AutoVillagerTradeCatalog.profession(autoVillagerTraderProfession).trades(), autoVillagerTraderTrade, AutoVillagerTradeCatalog.profession(autoVillagerTraderProfession).trades().get(0).id(), value -> autoVillagerTraderTrade = value);
                    if (AutoVillagerTradeCatalog.requiresEnchantment(autoVillagerTraderProfession, autoVillagerTraderTrade)) {
                        addTextField(leftX, y, controlWidth, "Enchantment", autoVillagerTraderEnchantment, TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_ENCHANTMENT, value -> autoVillagerTraderEnchantment = value); y += ROW_HEIGHT;
                    }
                    addToggle(leftX, y, controlWidth, "Check Reachable Chests", autoVillagerTraderCheckChests, TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_CHECK_CHESTS, () -> autoVillagerTraderCheckChests, value -> autoVillagerTraderCheckChests = value, true); y += ROW_HEIGHT;
                    addToggle(leftX, y, controlWidth, "Auto Exit Villager UI", autoVillagerTraderAutoCloseMerchant, TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_AUTO_CLOSE_MERCHANT, () -> autoVillagerTraderAutoCloseMerchant, value -> autoVillagerTraderAutoCloseMerchant = value, true); y += ROW_HEIGHT;
                }
                addToggle(leftX, y, controlWidth, "Auto Drop Leveler Items", villagerLevelerAutoDropItems, TpsConfig.DEFAULT_VILLAGER_LEVELER_AUTO_DROP_ITEMS, () -> villagerLevelerAutoDropItems, value -> villagerLevelerAutoDropItems = value, true); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (selectedTab != Tab.SEARCH && !normalizeSearch(settingsSearch).isEmpty() && y == startY) {
            labels.add(new DrawLabel("No settings matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }

        return y;
    }

    private int buildToolsSettings(int y) {
        int controlWidth = leftWidth - RESET_WIDTH - 6;
        if (shouldShowFeature("tools.full_bright", "Full Bright", "night vision", "brightness")) {
            y = addFeatureSection(y, "tools.full_bright", "Full Bright");
            if (!isFeatureCollapsed("tools.full_bright")) {
                addToggle(leftX, y, controlWidth, "Enable Full Bright", fullBrightEnabled, TpsConfig.DEFAULT_FULL_BRIGHT_ENABLED, () -> fullBrightEnabled, value -> fullBrightEnabled = value, false); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("tools.time_changer", "Time Changer", "day", "night", "sunset")) {
            y = addFeatureSection(y, "tools.time_changer", "Time Changer");
            if (!isFeatureCollapsed("tools.time_changer")) {
                addToggle(leftX, y, controlWidth, "Enable Time Changer", timeChangerEnabled, TpsConfig.DEFAULT_TIME_CHANGER_ENABLED, () -> timeChangerEnabled, value -> timeChangerEnabled = value, true); y += ROW_HEIGHT;
                if (timeChangerEnabled) {
                    addIntSlider(leftX, y, controlWidth, "Time Of Day", timeOfDay, 0, 24000, 100, TpsConfig.DEFAULT_TIME_OF_DAY, value -> timeOfDay = value); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("misc.fire_overlay", "Fire Overlay", "fire height", "low fire")) {
            y = addFeatureSection(y, "misc.fire_overlay", "Fire Overlay");
            if (!isFeatureCollapsed("misc.fire_overlay")) {
                addToggle(leftX, y, controlWidth, "Enable Fire Overlay Height", fireOverlayEnabled, TpsConfig.DEFAULT_MISC_FIRE_OVERLAY_ENABLED, () -> fireOverlayEnabled, value -> fireOverlayEnabled = value, true); y += ROW_HEIGHT;
                if (fireOverlayEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Fire Overlay Height", fireOverlayHeight, -1.0, 1.0, 0.025, TpsConfig.DEFAULT_FIRE_OVERLAY_HEIGHT, value -> fireOverlayHeight = (float) value, value -> formatSigned(value)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("tools.streamer_mode", "Streamer Mode", "hide names", "privacy")) {
            y = addFeatureSection(y, "tools.streamer_mode", "Streamer Mode");
            if (!isFeatureCollapsed("tools.streamer_mode")) {
                addToggle(leftX, y, controlWidth, "Enable Streamer Mode", streamerModeEnabled, TpsConfig.DEFAULT_STREAMER_MODE_ENABLED, () -> streamerModeEnabled, value -> streamerModeEnabled = value, false); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("tools.zoom", "Zoom", "fov", "optifine")) {
            y = addFeatureSection(y, "tools.zoom", "Zoom");
            if (!isFeatureCollapsed("tools.zoom")) {
                addToggle(leftX, y, controlWidth, "Enable Zoom", zoomEnabled, TpsConfig.DEFAULT_ZOOM_ENABLED, () -> zoomEnabled, value -> zoomEnabled = value, true); y += ROW_HEIGHT;
                if (zoomEnabled) {
                    addDoubleSlider(leftX, y, controlWidth, "Zoom Strength", zoomMultiplier, TpsConfig.MIN_ZOOM_MULTIPLIER, TpsConfig.MAX_ZOOM_MULTIPLIER, 0.1, TpsConfig.DEFAULT_ZOOM_MULTIPLIER, value -> zoomMultiplier = (float) value, value -> formatDecimal(value, 1) + "x"); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("tools.freelook", "Freelook", "third person", "camera", "look around")) {
            y = addFeatureSection(y, "tools.freelook", "Freelook");
            if (!isFeatureCollapsed("tools.freelook")) {
                addToggle(leftX, y, controlWidth, "Enable Freelook", freelookEnabled, TpsConfig.DEFAULT_FREELOOK_ENABLED, () -> freelookEnabled, value -> freelookEnabled = value, true); y += ROW_HEIGHT;
                if (freelookEnabled) {
                    addWideButton(leftX, y, controlWidth, "Activation: " + (freelookToggleMode ? "Toggle" : "Hold"), b -> {
                        freelookToggleMode = !freelookToggleMode;
                        changed();
                        clearAndInit();
                    }); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (selectedTab != Tab.SEARCH && !normalizeSearch(settingsSearch).isEmpty() && y == contentTop - scrollOffset) {
            labels.add(new DrawLabel("No settings matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }

        return y;
    }

    private int buildKeybindSettings(int y) {
        int controlWidth = leftWidth - RESET_WIDTH - 6;
        int startY = y;

        if (shouldShowFeature("tools.chat_macros", "Chat Macros", "messages", "commands", "macros")) {
            y = addFeatureSection(y, "tools.chat_macros", "Chat Macros");
            if (!isFeatureCollapsed("tools.chat_macros")) {
                addToggle(leftX, y, controlWidth, "Enable Chat Macros", chatMacrosEnabled, false, () -> chatMacrosEnabled, value -> chatMacrosEnabled = value, true); y += ROW_HEIGHT;
                if (chatMacrosEnabled) {
                    addMacroListButtons(leftX, y, controlWidth); y += ROW_HEIGHT;
                    for (int i = 0; i < macroMessages.size(); i++) {
                        final int index = i;
                        addTextField(leftX, y, controlWidth, "Macro " + (index + 1), macroMessages.get(index), "", value -> setMacroMessage(index, value)); y += ROW_HEIGHT;
                    }
                }
            }
            y += 10;
        }

        if (shouldShowFeature("keybinds.general", "General", "controls", "open menu", "zoom", "reset totem")) {
            y = addFeatureSection(y, "keybinds.general", "General");
            if (!isFeatureCollapsed("keybinds.general")) {
                addKeybindButton(leftX, y, controlWidth, "Open Menu", AtomicsClient.getOpenStudioKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Zoom", AtomicsClient.getZoomKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Freelook", AtomicsClient.getFreelookKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Reset Totem Counter", AtomicsClient.getResetTotemCounterKeyBinding()); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("keybinds.module_toggles", "Module Toggles", "dual spectate", "duel spectate", "full bright", "time changer", "projectile trail", "streamer mode", "auto trader", "villager", "leveler", "friend", "foe")) {
            y = addFeatureSection(y, "keybinds.module_toggles", "Module Toggles");
            if (!isFeatureCollapsed("keybinds.module_toggles")) {
                addKeybindButton(leftX, y, controlWidth, "Dual Spectate Camera", AtomicsClient.getToggleDualSpectateKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Full Bright", AtomicsClient.getToggleFullBrightKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Time Changer", AtomicsClient.getToggleTimeChangerKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Projectile Trail", AtomicsClient.getToggleProjectileTrailKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Streamer Mode", AtomicsClient.getToggleStreamerModeKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Auto Trader", AtomicsClient.getToggleAutoTraderKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Mark Villager Leveler", AtomicsClient.getMarkVillagerLevelerKeyBinding()); y += ROW_HEIGHT;
                addKeybindButton(leftX, y, controlWidth, "Cycle Friend/Foe Target", AtomicsClient.getCycleFriendFoeKeyBinding()); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("keybinds.chat_macros", "Chat Macros", "macros", "messages", "commands")) {
            y = addFeatureSection(y, "keybinds.chat_macros", "Chat Macros");
            if (!isFeatureCollapsed("keybinds.chat_macros")) {
                for (int i = 0; i < macroMessages.size(); i++) {
                    addKeybindButton(leftX, y, controlWidth, "Macro " + (i + 1), AtomicsClient.getMacroKeyBinding(i)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (selectedTab != Tab.SEARCH && !normalizeSearch(settingsSearch).isEmpty() && y == startY) {
            labels.add(new DrawLabel("No settings matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }

        return y;
    }

    private int buildPvpSettings(int y) {
        int controlWidth = leftWidth - RESET_WIDTH - 6;
        int startY = y;

        if (shouldShowFeature("pvp.reach", "Reach Display", "distance", "hit")) {
            y = addFeatureSection(y, "pvp.reach", "Reach Display");
            if (!isFeatureCollapsed("pvp.reach")) {
                addToggle(leftX, y, controlWidth, "Show Reach On Hit", reachDisplayEnabled, TpsConfig.DEFAULT_REACH_DISPLAY_ENABLED, () -> reachDisplayEnabled, value -> reachDisplayEnabled = value, false); y += ROW_HEIGHT;
                labels.add(new DrawLabel("Shows your distance to the target for a moment after a confirmed hit.", leftX + 8, y + 4, 0xAAAAAA));
                y += 22;
            }
            y += 10;
        }

        if (shouldShowFeature("misc.shield_warning_overlay", "Shield Warning Overlay", "shield warning", "shield delay", "disabled shield", "red shield")) {
            y = addFeatureSection(y, "misc.shield_warning_overlay", "Shield Warning Overlay");
            if (!isFeatureCollapsed("misc.shield_warning_overlay")) {
                addToggle(leftX, y, controlWidth, "Enable Shield Warning Overlay", shieldWarningOverlayEnabled, TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_ENABLED, () -> shieldWarningOverlayEnabled, value -> shieldWarningOverlayEnabled = value, true); y += ROW_HEIGHT;
                if (shieldWarningOverlayEnabled) {
                    addIntSlider(leftX, y, controlWidth, "Warning Red", shieldWarningOverlayR, 0, 255, 1, TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_R, value -> shieldWarningOverlayR = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Warning Green", shieldWarningOverlayG, 0, 255, 1, TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_G, value -> shieldWarningOverlayG = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Warning Blue", shieldWarningOverlayB, 0, 255, 1, TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_B, value -> shieldWarningOverlayB = value); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Warning Opacity", shieldWarningOverlayAlpha, 0.0, 1.0, 0.025, TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_ALPHA, value -> shieldWarningOverlayAlpha = (float) value, value -> formatPercent(value)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.friend_foe_overlay", "Friend/Foe Overlay", "friends", "foes", "highlight", "green", "red")) {
            y = addFeatureSection(y, "pvp.friend_foe_overlay", "Friend/Foe Overlay");
            if (!isFeatureCollapsed("pvp.friend_foe_overlay")) {
                addToggle(leftX, y, controlWidth, "Enable Friend/Foe Overlay", friendFoeOverlayEnabled, TpsConfig.DEFAULT_FRIEND_FOE_OVERLAY_ENABLED, () -> friendFoeOverlayEnabled, value -> friendFoeOverlayEnabled = value, true); y += ROW_HEIGHT;
                if (friendFoeOverlayEnabled) {
                    addWideButton(leftX, y, controlWidth, "Edit Player List (" + getFriendFoeCount() + ")", b -> this.client.setScreen(new FriendFoeListScreen(this))); y += ROW_HEIGHT;
                    addWideButton(leftX, y, controlWidth, "Render Style: " + friendFoeStyleLabel(friendFoeOverlayStyle), b -> {
                        friendFoeOverlayStyle = nextFriendFoeStyle(friendFoeOverlayStyle);
                        changed();
                        clearAndInit();
                    }); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Friend Red", friendOverlayR, 0, 255, 1, TpsConfig.DEFAULT_FRIEND_OVERLAY_R, value -> friendOverlayR = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Friend Green", friendOverlayG, 0, 255, 1, TpsConfig.DEFAULT_FRIEND_OVERLAY_G, value -> friendOverlayG = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Friend Blue", friendOverlayB, 0, 255, 1, TpsConfig.DEFAULT_FRIEND_OVERLAY_B, value -> friendOverlayB = value); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Friend Opacity", friendOverlayAlpha, 0.0, 1.0, 0.025, TpsConfig.DEFAULT_FRIEND_OVERLAY_ALPHA, value -> friendOverlayAlpha = (float) value, value -> formatPercent(value)); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Foe Red", foeOverlayR, 0, 255, 1, TpsConfig.DEFAULT_FOE_OVERLAY_R, value -> foeOverlayR = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Foe Green", foeOverlayG, 0, 255, 1, TpsConfig.DEFAULT_FOE_OVERLAY_G, value -> foeOverlayG = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Foe Blue", foeOverlayB, 0, 255, 1, TpsConfig.DEFAULT_FOE_OVERLAY_B, value -> foeOverlayB = value); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Foe Opacity", foeOverlayAlpha, 0.0, 1.0, 0.025, TpsConfig.DEFAULT_FOE_OVERLAY_ALPHA, value -> foeOverlayAlpha = (float) value, value -> formatPercent(value)); y += ROW_HEIGHT;
                    labels.add(new DrawLabel("Outline styles render visible model edges without showing through walls.", leftX + 8, y + 4, 0xAAAAAA));
                    y += 14;
                    labels.add(new DrawLabel("Use the keybind to cycle a looked-at player through Friend, Foe, and Neutral.", leftX + 8, y + 4, 0xAAAAAA));
                    y += 22;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.team_count_overlay", "Team Count Overlay", "scoreboard", "teams left", "team counts", "players left", "server", "ip")) {
            y = addFeatureSection(y, "pvp.team_count_overlay", "Team Count Overlay");
            if (!isFeatureCollapsed("pvp.team_count_overlay")) {
                addToggle(leftX, y, controlWidth, "Show Team Count Overlay", teamCountOverlayEnabled, TpsConfig.DEFAULT_TEAM_COUNT_OVERLAY_ENABLED, () -> teamCountOverlayEnabled, value -> teamCountOverlayEnabled = value, true); y += ROW_HEIGHT;
                if (teamCountOverlayEnabled) {
                    addWideButton(leftX, y, controlWidth, "Move Overlay", b -> this.client.setScreen(new TeamCountOverlayLayoutScreen(this, teamCountOverlayX, teamCountOverlayY))); y += ROW_HEIGHT;
                    addToggle(leftX, y, controlWidth, "Only On Listed Servers", teamCountOverlayServerFilterEnabled, TpsConfig.DEFAULT_TEAM_COUNT_OVERLAY_SERVER_FILTER_ENABLED, () -> teamCountOverlayServerFilterEnabled, value -> teamCountOverlayServerFilterEnabled = value, true); y += ROW_HEIGHT;
                    if (teamCountOverlayServerFilterEnabled) {
                        addTextField(leftX, y, controlWidth, "Server IPs", teamCountOverlayAllowedServers, "ip1, ip2", value -> teamCountOverlayAllowedServers = value); y += ROW_HEIGHT;
                    }
                    labels.add(new DrawLabel("Counts tab-list players by scoreboard team.", leftX + 8, y + 4, 0xAAAAAA));
                    y += 22;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("tools.tnt_timer", "TNT Timer", "fuse", "explosion")) {
            y = addFeatureSection(y, "tools.tnt_timer", "TNT Timer");
            if (!isFeatureCollapsed("tools.tnt_timer")) {
                addToggle(leftX, y, controlWidth, "Enable TNT Timer", tntTimerEnabled, TpsConfig.DEFAULT_TNT_TIMER_ENABLED, () -> tntTimerEnabled, value -> tntTimerEnabled = value, true); y += ROW_HEIGHT;
                if (tntTimerEnabled) {
                    addIntSlider(leftX, y, controlWidth, "Timer Range", tntTimerRange, 8, 128, 1, TpsConfig.DEFAULT_TNT_TIMER_RANGE, value -> tntTimerRange = value); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("tools.projectile_trail", "Projectile Trail", "arrows", "pearls", "snowballs")) {
            y = addFeatureSection(y, "tools.projectile_trail", "Projectile Trail");
            if (!isFeatureCollapsed("tools.projectile_trail")) {
                addToggle(leftX, y, controlWidth, "Enable Projectile Trail", projectileTrailEnabled, TpsConfig.DEFAULT_PROJECTILE_TRAIL_ENABLED, () -> projectileTrailEnabled, value -> projectileTrailEnabled = value, true); y += ROW_HEIGHT;
                if (projectileTrailEnabled) {
                    addTextField(leftX, y, controlWidth, "Trail Particle", projectileTrailParticleId, TpsConfig.DEFAULT_PROJECTILE_TRAIL_PARTICLE_ID, value -> projectileTrailParticleId = value); y += ROW_HEIGHT;
                    addIntSlider(leftX, y, controlWidth, "Particles Per Tick", projectileTrailParticleCount, 0, 20, 1, TpsConfig.DEFAULT_PROJECTILE_TRAIL_PARTICLE_COUNT, value -> projectileTrailParticleCount = value); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Trail Spread", projectileTrailSpread, 0.0, 1.5, 0.01, TpsConfig.DEFAULT_PROJECTILE_TRAIL_SPREAD, value -> projectileTrailSpread = value, value -> formatDecimal(value, 2)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Trail Speed", projectileTrailSpeed, 0.0, 1.0, 0.01, TpsConfig.DEFAULT_PROJECTILE_TRAIL_SPEED, value -> projectileTrailSpeed = value, value -> formatDecimal(value, 2)); y += ROW_HEIGHT;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.dual_spectate", "Dual Spectate Camera", "fight camera", "autofill", "third person", "overhead", "top down", "group distance", "y-level", "vertical")) {
            y = addFeatureSection(y, "pvp.dual_spectate", "Dual Spectate Camera");
            if (!isFeatureCollapsed("pvp.dual_spectate")) {
                addToggle(leftX, y, controlWidth, "Enable Dual Spectate Camera", dualSpectateEnabled, false, () -> dualSpectateEnabled, value -> dualSpectateEnabled = value, true); y += ROW_HEIGHT;
                if (dualSpectateEnabled) {
                    addToggle(leftX, y, controlWidth, "Auto Fill Nearest Pair", dualSpectateAutoFill, false, () -> dualSpectateAutoFill, value -> dualSpectateAutoFill = value, true); y += ROW_HEIGHT;
                    addTextField(leftX, y, controlWidth, "Player One", dualSpectatePlayerOne, "Username", value -> dualSpectatePlayerOne = value); y += ROW_HEIGHT;
                    addTextField(leftX, y, controlWidth, "Player Two", dualSpectatePlayerTwo, "Username", value -> dualSpectatePlayerTwo = value); y += ROW_HEIGHT;
                    addWideButton(leftX, y, controlWidth, "Autofill Nearest Pair", b -> autofillDualSpectatePlayers()); y += ROW_HEIGHT;
                    addToggle(leftX, y, controlWidth, "Force Third Person", dualSpectateForceThirdPerson, true, () -> dualSpectateForceThirdPerson, value -> dualSpectateForceThirdPerson = value, false); y += ROW_HEIGHT;
                    addToggle(leftX, y, controlWidth, "Overhead View", dualSpectateOverheadEnabled, TpsConfig.DEFAULT_DUAL_SPECTATE_OVERHEAD_ENABLED, () -> dualSpectateOverheadEnabled, value -> dualSpectateOverheadEnabled = value, true); y += ROW_HEIGHT;
                    if (dualSpectateOverheadEnabled) {
                        addDoubleSlider(leftX, y, controlWidth, "Overhead Group Distance", dualSpectateOverheadGroupDistance, 4.0, 80.0, 1.0, TpsConfig.DEFAULT_DUAL_SPECTATE_OVERHEAD_GROUP_DISTANCE, value -> dualSpectateOverheadGroupDistance = (float) value, value -> formatDecimal(value, 0)); y += ROW_HEIGHT;
                    }
                    addDoubleSlider(leftX, y, controlWidth, "Frame Padding", dualSpectatePadding, 1.0, 2.5, 0.05, 1.35, value -> dualSpectatePadding = (float) value, value -> formatDecimal(value, 2) + "x"); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Min Distance", dualSpectateMinDistance, 2.0, 30.0, 0.5, 6.0, value -> dualSpectateMinDistance = (float) value, value -> formatDecimal(value, 1)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Max Distance", dualSpectateMaxDistance, 10.0, 160.0, 1.0, 80.0, value -> dualSpectateMaxDistance = (float) value, value -> formatDecimal(value, 0)); y += ROW_HEIGHT;
                    addDoubleSlider(leftX, y, controlWidth, "Max Y Difference", dualSpectateMaxYDifference, 2.0, 48.0, 1.0, TpsConfig.DEFAULT_DUAL_SPECTATE_MAX_Y_DIFFERENCE, value -> dualSpectateMaxYDifference = (float) value, value -> formatDecimal(value, 0)); y += ROW_HEIGHT;
                    labels.add(new DrawLabel(dualSpectateOverheadEnabled ? "Frames nearby active players from above." : "Frames both players from a smooth side-on fight camera.", leftX + 8, y + 4, 0xAAAAAA));
                    y += 22;
                }
            }
            y += 10;
        }

        if (selectedTab != Tab.SEARCH && !normalizeSearch(settingsSearch).isEmpty() && y == startY) {
            labels.add(new DrawLabel("No settings matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }

        return y;
    }

    private int buildSearchSettings(int y) {
        int startY = y;
        y = buildTotemSettings(y);
        y = buildPvpSettings(y);
        y = buildStatsDashboard(y);
        y = buildToolsSettings(y);
        y = buildMiscSettings(y);
        y = buildKeybindSettings(y);

        if (!normalizeSearch(settingsSearch).isEmpty() && y == startY) {
            labels.add(new DrawLabel("No modules matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }
        return y;
    }

    private int buildStatsDashboard(int y) {
        int controlWidth = leftWidth - RESET_WIDTH - 6;
        int startY = y;
        if (shouldShowFeature("tools.armor_hud", "Armor HUD", "armor", "durability", "dura", "warning", "low armor")) {
            y = addFeatureSection(y, "tools.armor_hud", "Armor HUD");
            if (!isFeatureCollapsed("tools.armor_hud")) {
                addToggle(leftX, y, controlWidth, "Show Armor Durability HUD", armorHudEnabled, TpsConfig.DEFAULT_ARMOR_HUD_ENABLED, () -> armorHudEnabled, value -> armorHudEnabled = value, true); y += ROW_HEIGHT;
                addToggle(leftX, y, controlWidth, "Warn On Low Armor", armorDurabilityWarningEnabled, TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_ENABLED, () -> armorDurabilityWarningEnabled, value -> armorDurabilityWarningEnabled = value, true); y += ROW_HEIGHT;
                if (armorHudEnabled || armorDurabilityWarningEnabled) {
                    armorDurabilityWarningPercent = TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
                    if (armorHudEnabled) {
                        addToggle(leftX, y, controlWidth, "Auto HUD Position", armorHudAutoPosition, TpsConfig.DEFAULT_ARMOR_HUD_AUTO_POSITION, () -> armorHudAutoPosition, value -> armorHudAutoPosition = value, true); y += ROW_HEIGHT;
                        if (!armorHudAutoPosition) {
                            addWideButton(leftX, y, controlWidth, "Edit HUD Position", b -> this.client.setScreen(new ArmorHudLayoutScreen(this, armorHudX, armorHudY, armorHudVertical, armorHudSpacing, armorHudDurabilityMode, armorHudHotbarBorder))); y += ROW_HEIGHT;
                            addWideButton(leftX, y, controlWidth, "Orientation: " + (armorHudVertical ? "Vertical" : "Horizontal"), b -> {
                                armorHudVertical = !armorHudVertical;
                                changed();
                                clearAndInit();
                            }); y += ROW_HEIGHT;
                        }
                        addToggle(leftX, y, controlWidth, "Hotbar Border", armorHudHotbarBorder, TpsConfig.DEFAULT_ARMOR_HUD_HOTBAR_BORDER, () -> armorHudHotbarBorder, value -> armorHudHotbarBorder = value, true); y += ROW_HEIGHT;
                        addWideButton(leftX, y, controlWidth, "Durability: " + armorHudDurabilityModeLabel(armorHudDurabilityMode), b -> {
                            armorHudDurabilityMode = nextArmorHudDurabilityMode(armorHudDurabilityMode);
                            changed();
                            clearAndInit();
                        }); y += ROW_HEIGHT;
                        if (!armorHudAutoPosition) {
                            addIntSlider(leftX, y, controlWidth, "Piece Spacing", armorHudSpacing, 20, 64, 1, TpsConfig.DEFAULT_ARMOR_HUD_SPACING, value -> armorHudSpacing = value); y += ROW_HEIGHT;
                        }
                    }
                }
            }
            y += 10;
        }

        if (shouldShowFeature("tools.food_overlay", "Food Preview Overlay", "food", "hunger", "apple skin", "appleskin", "saturation")) {
            y = addFeatureSection(y, "tools.food_overlay", "Food Preview Overlay");
            if (!isFeatureCollapsed("tools.food_overlay")) {
                addToggle(leftX, y, controlWidth, "Show Food Preview Overlay", foodOverlayEnabled, TpsConfig.DEFAULT_FOOD_OVERLAY_ENABLED, () -> foodOverlayEnabled, value -> foodOverlayEnabled = value, true); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("tools.partial_status_icons", "Precise Status Icons", "partial hearts", "decimal health", "health", "hunger")) {
            y = addFeatureSection(y, "tools.partial_status_icons", "Precise Status Icons");
            if (!isFeatureCollapsed("tools.partial_status_icons")) {
                addToggle(leftX, y, controlWidth, "Show Precise Partial Hearts", partialStatusIconsEnabled, TpsConfig.DEFAULT_PARTIAL_STATUS_ICONS_ENABLED, () -> partialStatusIconsEnabled, value -> partialStatusIconsEnabled = value, true); y += ROW_HEIGHT;
            }
            y += 10;
        }
        if (shouldShowFeature("pvp.win_percent", "Win Percent Nametags", "win", "percent", "odds")) {
            y = addFeatureSection(y, "pvp.win_percent", "Win Percent Nametags");
            if (!isFeatureCollapsed("pvp.win_percent")) {
                addToggle(leftX, y, controlWidth, "Show Win Percent On Nametags", winOddsEnabled, true, () -> winOddsEnabled, value -> winOddsEnabled = value, true); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.totem_pop_counter", "Totem Pop Counter", "totem", "pops", "counter")) {
            y = addFeatureSection(y, "pvp.totem_pop_counter", "Totem Pop Counter");
            if (!isFeatureCollapsed("pvp.totem_pop_counter")) {
                addToggle(leftX, y, controlWidth, "Show Totem Pops On Nametags", totemPopNametagEnabled, TpsConfig.DEFAULT_TOTEM_POP_NAMETAG_ENABLED, () -> totemPopNametagEnabled, value -> totemPopNametagEnabled = value, true); y += ROW_HEIGHT;
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.ping_nametags", "Ping Nametags", "ping", "latency", "ms")) {
            y = addFeatureSection(y, "pvp.ping_nametags", "Ping Nametags");
            if (!isFeatureCollapsed("pvp.ping_nametags")) {
                addToggle(leftX, y, controlWidth, "Show Ping On Nametags", pingNametagEnabled, TpsConfig.DEFAULT_PING_NAMETAG_ENABLED, () -> pingNametagEnabled, value -> pingNametagEnabled = value, true); y += ROW_HEIGHT;
                labels.add(new DrawLabel("Displays each player's tab-list ping beside their nametag.", leftX + 8, y + 4, 0xAAAAAA));
                y += 22;
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.nametags", "Nametag Customization", "nametag", "layout", "move", "order")) {
            y = addFeatureSection(y, "pvp.nametags", "Nametag Customization");
            if (!isFeatureCollapsed("pvp.nametags")) {
                List<String> enabledItems = enabledNametagItems();
                if (enabledItems.isEmpty()) {
                    labels.add(new DrawLabel("Enable a nametag item to move it around.", leftX + 8, y + 4, 0xAAAAAA));
                    y += 22;
                } else {
                    for (String item : enabledItems) {
                        y = addNametagLayoutRow(y, item, enabledItems);
                    }
                    labels.add(new DrawLabel("Only enabled nametag items appear here.", leftX + 8, y + 4, 0xAAAAAA));
                    y += 22;
                }
            }
            y += 10;
        }

        if (shouldShowFeature("pvp.opponent_info", "Opponent Info", "mctiers", "pvptiers", "tiers", "rankings")) {
            y = addFeatureSection(y, "pvp.opponent_info", "Opponent Info");
            if (!isFeatureCollapsed("pvp.opponent_info")) {
                addToggle(leftX, y, controlWidth, "Send Opponent Tier Chat", opponentInfoEnabled, TpsConfig.DEFAULT_OPPONENT_INFO_ENABLED, () -> opponentInfoEnabled, value -> opponentInfoEnabled = value, false); y += ROW_HEIGHT;
                addToggle(leftX, y, controlWidth, "Show Opponent Stats On Nametags", opponentStatsNametagEnabled, TpsConfig.DEFAULT_OPPONENT_STATS_NAMETAG_ENABLED, () -> opponentStatsNametagEnabled, value -> opponentStatsNametagEnabled = value, true); y += ROW_HEIGHT;
                addWideButton(leftX, y, controlWidth, "Nametag Format: " + opponentStatsNametagFormatLabel(opponentStatsNametagFormat), b -> {
                    opponentStatsNametagFormat = nextOpponentStatsNametagFormat(opponentStatsNametagFormat);
                    changed();
                    clearAndInit();
                }); y += ROW_HEIGHT;
                labels.add(new DrawLabel("Chat posts duel tier info. Nametag shows the best cached tier/ranking tag.", leftX + 8, y + 4, 0xAAAAAA));
                y += 22;
            }
            y += 10;
        }
        if (selectedTab != Tab.SEARCH && !normalizeSearch(settingsSearch).isEmpty() && y == startY) {
            labels.add(new DrawLabel("No settings matched the search.", leftX + 8, y + 8, 0xCCCCCC));
            y += 34;
        }
        return y;
    }
    private void autofillDualSpectatePlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            status = Text.literal("Autofill failed: no world loaded").formatted(Formatting.RED);
            return;
        }

        String[] pair = DualSpectateCamera.findNearestPair(client);
        if (pair == null) {
            status = Text.literal("Autofill needs at least two nearby players").formatted(Formatting.RED);
            return;
        }

        dualSpectatePlayerOne = pair[0];
        dualSpectatePlayerTwo = pair[1];
        changed();
        status = Text.literal("Autofilled " + dualSpectatePlayerOne + " and " + dualSpectatePlayerTwo).formatted(Formatting.GREEN);
        clearAndInit();
    }

    private void refreshDualSpectateAutoFill() {
        if (!dualSpectateEnabled || !dualSpectateAutoFill) {
            return;
        }

        String[] pair = DualSpectateCamera.findNearestPair(MinecraftClient.getInstance());
        if (pair == null) {
            return;
        }

        if (pair[0].equals(dualSpectatePlayerOne) && pair[1].equals(dualSpectatePlayerTwo)) {
            applyToConfig();
            return;
        }

        dualSpectatePlayerOne = pair[0];
        dualSpectatePlayerTwo = pair[1];
        applyToConfig();
        status = Text.literal("Auto-filled " + dualSpectatePlayerOne + " and " + dualSpectatePlayerTwo).formatted(Formatting.AQUA);
        if (selectedTab == Tab.PVP) {
            clearAndInit();
        }
    }

    private int buildPlaceholderSettings(int y, String title, String message) {
        y = addSection(leftX, y, leftWidth, title + " Settings");
        labels.add(new DrawLabel(message, leftX + 8, y + 8, 0xCCCCCC));
        labels.add(new DrawLabel("This tab is already wired into the layout, so adding real settings later will not require rebuilding the whole screen.", leftX + 8, y + 22, 0xAAAAAA));
        return y + 58;
    }

    private boolean shouldShowFeature(String key, String title, String... terms) {
        if (selectedTab != Tab.SEARCH) {
            return true;
        }
        String query = normalizeSearch(settingsSearch);
        if (query.isEmpty()) return true;
        if (key != null && key.toLowerCase(Locale.ROOT).contains(query)) return true;
        if (title != null && title.toLowerCase(Locale.ROOT).contains(query)) return true;
        for (String term : terms) {
            if (term != null && term.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private int addFeatureSection(int y, String key, String title) {
        boolean collapsed = isFeatureCollapsed(key);
        if (isWidgetVisible(y)) {
            addDrawableChild(new SectionHeaderWidget(leftX, y, leftWidth, BUTTON_HEIGHT, title, collapsed, () -> {
                toggleFeatureSection(key);
                clearAndInit();
            }));
        } else {
            labels.add(new DrawLabel((collapsed ? "> " : "v ") + title, leftX + 8, y + 5, 0xFFFFFF));
        }
        return y + SECTION_HEIGHT;
    }

    private boolean isFeatureCollapsed(String key) {
        if (selectedTab == Tab.SEARCH) {
            return normalizeSearch(settingsSearch).isEmpty();
        }
        return collapsedSections.contains(key);
    }

    private void toggleFeatureSection(String key) {
        if (collapsedSections.contains(key)) {
            collapsedSections.remove(key);
        } else {
            collapsedSections.add(key);
        }
        applyToConfig();
    }

    private int addSection(int x, int y, int width, String title) {
        labels.add(new DrawLabel(title, x + 8, y + 5, TEXT_MAIN));
        labels.add(new DrawLabel("__line__", x, y + 18, ACCENT_SOFT, false, width));
        return y + SECTION_HEIGHT;
    }

    private void addWideButton(int x, int y, int width, String label, ButtonWidget.PressAction action) {
        if (!isWidgetVisible(y)) return;
        addDrawableChild(ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    private int addNametagLayoutRow(int y, String item, List<String> enabledItems) {
        if (!isWidgetVisible(y)) {
            return y + ROW_HEIGHT;
        }

        int gap = 6;
        int sideWidth = Math.min(120, Math.max(96, leftWidth / 5));
        int buttonWidth = 42;
        int labelWidth = Math.max(90, leftWidth - sideWidth - buttonWidth * 2 - gap * 3);
        int itemIndex = enabledItems.indexOf(item);
        boolean beforeName = nametagItemsBeforeName.contains(item);

        ButtonWidget sideButton = ButtonWidget.builder(Text.literal((itemIndex + 1) + ". " + nametagItemLabel(item) + ": " + (beforeName ? "Before" : "After")), b -> {
            toggleNametagItemSide(item);
            changed();
            clearAndInit();
        }).dimensions(leftX, y, labelWidth, BUTTON_HEIGHT).build();
        addDrawableChild(sideButton);

        ButtonWidget upButton = ButtonWidget.builder(Text.literal("Up"), b -> {
            moveNametagItem(item, -1);
            changed();
            clearAndInit();
        }).dimensions(leftX + labelWidth + gap, y, buttonWidth, BUTTON_HEIGHT).build();
        upButton.active = itemIndex > 0;
        addDrawableChild(upButton);

        ButtonWidget downButton = ButtonWidget.builder(Text.literal("Down"), b -> {
            moveNametagItem(item, 1);
            changed();
            clearAndInit();
        }).dimensions(leftX + labelWidth + gap + buttonWidth + gap, y, buttonWidth, BUTTON_HEIGHT).build();
        downButton.active = itemIndex >= 0 && itemIndex < enabledItems.size() - 1;
        addDrawableChild(downButton);

        addDrawableChild(ButtonWidget.builder(Text.literal(beforeName ? "Put After" : "Put Before"), b -> {
            toggleNametagItemSide(item);
            changed();
            clearAndInit();
        }).dimensions(leftX + labelWidth + gap + buttonWidth * 2 + gap * 2, y, sideWidth, BUTTON_HEIGHT).build());
        return y + ROW_HEIGHT;
    }

    private void addMacroListButtons(int x, int y, int width) {
        if (!isWidgetVisible(y)) return;
        int gap = 6;
        int buttonWidth = (width - gap) / 2;
        ButtonWidget addButton = ButtonWidget.builder(Text.literal("Add Macro"), b -> {
            if (macroMessages.size() < TpsConfig.MAX_MACRO_SLOTS) {
                macroMessages.add("");
                changed();
                clearAndInit();
            }
        }).dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build();
        addButton.active = macroMessages.size() < TpsConfig.MAX_MACRO_SLOTS;
        addDrawableChild(addButton);

        ButtonWidget removeButton = ButtonWidget.builder(Text.literal("Remove Last"), b -> {
            if (macroMessages.size() > TpsConfig.MIN_MACRO_SLOTS) {
                macroMessages.remove(macroMessages.size() - 1);
                changed();
                clearAndInit();
            }
        }).dimensions(x + buttonWidth + gap, y, width - buttonWidth - gap, BUTTON_HEIGHT).build();
        removeButton.active = macroMessages.size() > TpsConfig.MIN_MACRO_SLOTS;
        addDrawableChild(removeButton);
    }

    private void addKeybindButton(int x, int y, int width, String label, KeyBinding keyBinding) {
        if (!isWidgetVisible(y)) return;
        String value = listeningKeyBinding == keyBinding ? "Press a key..." : AtomicsClient.keyBindingName(keyBinding);
        ButtonWidget button = ButtonWidget.builder(rowText(label, value), b -> {
            if (keyBinding == null) {
                return;
            }
            listeningKeyBinding = keyBinding;
            listeningKeyLabel = label;
            status = Text.literal("Press a key for " + label + " (Delete unbinds)").formatted(Formatting.AQUA);
            clearAndInit();
        }).dimensions(x, y, width, BUTTON_HEIGHT).build();
        button.active = keyBinding != null;
        addDrawableChild(button);
    }

    private void setMacroMessage(int index, String value) {
        if (index < 0 || index >= macroMessages.size()) {
            return;
        }
        macroMessages.set(index, value == null ? "" : value);
    }

    private ButtonWidget addToggle(int x, int y, int controlWidth, String label, boolean initialValue, boolean defaultValue, BooleanSupplier getter, ToggleSetter setter, boolean rebuildOnChange) {
        if (!isWidgetVisible(y)) {
            setter.set(initialValue);
            return null;
        }
        ButtonWidget button = ButtonWidget.builder(rowText(label, initialValue ? "ON" : "OFF"), b -> {
            boolean newValue = !getter.getAsBoolean();
            setter.set(newValue);
            b.setMessage(rowText(label, newValue ? "ON" : "OFF"));
            changed();
            if (rebuildOnChange) clearAndInit();
        }).dimensions(x, y, controlWidth, BUTTON_HEIGHT).build();
        addDrawableChild(button);
        addDrawableChild(ButtonWidget.builder(Text.literal("↻"), b -> {
            setter.set(defaultValue);
            changed();
            if (rebuildOnChange) clearAndInit(); else button.setMessage(rowText(label, defaultValue ? "ON" : "OFF"));
        }).dimensions(x + controlWidth + 6, y, RESET_WIDTH, BUTTON_HEIGHT).build());
        setter.set(initialValue);
        return button;
    }

    private TextFieldWidget addTextField(int x, int y, int controlWidth, String label, String initialValue, String defaultValue, Consumer<String> setter) {
        labels.add(new DrawLabel(label, x + 4, y - 11, TEXT_MUTED));
        if (!isWidgetVisible(y)) return null;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, controlWidth, BUTTON_HEIGHT, Text.literal(label));
        field.setText(initialValue == null ? "" : initialValue);
        field.setPlaceholder(Text.literal(defaultValue).formatted(Formatting.DARK_GRAY));
        field.setChangedListener(value -> { setter.accept(value); changed(); });
        addDrawableChild(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("↻"), b -> { setter.accept(defaultValue); field.setText(defaultValue); changed(); }).dimensions(x + controlWidth + 6, y, RESET_WIDTH, BUTTON_HEIGHT).build());
        return field;
    }

    private int addDropdown(int x, int y, int controlWidth, String key, String label, List<? extends AutoVillagerTradeCatalog.Choice> options, String currentValue, String defaultValue, Consumer<String> setter) {
        if (options == null || options.isEmpty()) {
            return y;
        }

        AutoVillagerTradeCatalog.Choice selected = findChoice(options, currentValue);
        if (selected == null) {
            selected = options.get(0);
            setter.accept(selected.id());
        }

        boolean open = key.equals(openDropdownKey);
        if (isWidgetVisible(y)) {
            AutoVillagerTradeCatalog.Choice selectedForButton = selected;
            addDrawableChild(new DropdownButtonWidget(x, y, controlWidth, BUTTON_HEIGHT, label, selectedForButton.label(), open, () -> {
                openDropdownKey = open ? null : key;
                clearAndInit();
            }));

            String resetValue = choiceIdOrFirst(options, defaultValue);
            ButtonWidget reset = ButtonWidget.builder(Text.literal("↻"), b -> {
                setter.accept(resetValue);
                openDropdownKey = null;
                normalizeAutoVillagerTraderSelection();
                changed();
                clearAndInit();
            }).dimensions(x + controlWidth + 6, y, RESET_WIDTH, BUTTON_HEIGHT).build();
            reset.active = !selected.id().equals(resetValue);
            addDrawableChild(reset);
        }

        y += ROW_HEIGHT;
        if (open) {
            for (AutoVillagerTradeCatalog.Choice option : options) {
                int optionY = y;
                if (isWidgetVisible(optionY)) {
                    ButtonWidget button = ButtonWidget.builder(Text.literal(option.label()), b -> {
                        setter.accept(option.id());
                        openDropdownKey = null;
                        normalizeAutoVillagerTraderSelection();
                        changed();
                        clearAndInit();
                    }).dimensions(x + 8, optionY, controlWidth - 8, BUTTON_HEIGHT).build();
                    button.active = !option.id().equals(selectedForId(selected));
                    addDrawableChild(button);
                }
                y += ROW_HEIGHT;
            }
        }

        return y;
    }

    private static AutoVillagerTradeCatalog.Choice findChoice(List<? extends AutoVillagerTradeCatalog.Choice> options, String id) {
        if (id == null) {
            return null;
        }
        String normalizedId = id.trim();
        for (AutoVillagerTradeCatalog.Choice option : options) {
            if (option.id().equals(normalizedId)) {
                return option;
            }
        }
        return null;
    }

    private static String choiceIdOrFirst(List<? extends AutoVillagerTradeCatalog.Choice> options, String id) {
        AutoVillagerTradeCatalog.Choice choice = findChoice(options, id);
        return choice == null ? options.get(0).id() : choice.id();
    }

    private static String selectedForId(AutoVillagerTradeCatalog.Choice choice) {
        return choice == null ? "" : choice.id();
    }

    private void normalizeAutoVillagerTraderSelection() {
        autoVillagerTraderProfession = AutoVillagerTradeCatalog.normalizeProfessionId(autoVillagerTraderProfession);
        autoVillagerTraderTrade = AutoVillagerTradeCatalog.normalizeTradeId(autoVillagerTraderProfession, autoVillagerTraderTrade);
        if (autoVillagerTraderEnchantment == null) {
            autoVillagerTraderEnchantment = "";
        } else {
            autoVillagerTraderEnchantment = autoVillagerTraderEnchantment.trim();
            if (autoVillagerTraderEnchantment.length() > 64) {
                autoVillagerTraderEnchantment = autoVillagerTraderEnchantment.substring(0, 64);
            }
        }
    }

    private ValueSlider addIntSlider(int x, int y, int controlWidth, String label, int initialValue, int min, int max, int step, int defaultValue, IntSetter setter) {
        return addDoubleSlider(x, y, controlWidth, label, initialValue, min, max, step, defaultValue, value -> setter.set((int) Math.round(value)), value -> Integer.toString((int) Math.round(value)));
    }

    private ValueSlider addDoubleSlider(int x, int y, int controlWidth, String label, double initialValue, double min, double max, double step, double defaultValue, DoubleSetter setter, DoubleFunction<String> formatter) {
        if (!isWidgetVisible(y)) {
            setter.set(initialValue);
            return null;
        }
        ValueSlider slider = new ValueSlider(label, x, y, controlWidth, initialValue, min, max, step, setter::set, formatter);
        addDrawableChild(slider);
        addDrawableChild(ButtonWidget.builder(Text.literal("↻"), b -> slider.setActualValue(defaultValue)).dimensions(x + controlWidth + 6, y, RESET_WIDTH, BUTTON_HEIGHT).build());
        return slider;
    }

    private boolean isWidgetVisible(int y) {
        // Only add controls that are fully inside the scrollable settings area.
        // This prevents hidden rows from sitting underneath the footer buttons and
        // stealing clicks through the bottom bar.
        return y >= contentTop && y + BUTTON_HEIGHT <= contentBottom - 6;
    }

    private void loadStateFromConfig() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) {
            cfg = new TpsConfig();
            AtomicsClient.CONFIG = cfg;
        }
        cfg.normalize();
        enabled = cfg.enabled;
        particlesEnabled = cfg.particles.enabled;
        soundsEnabled = cfg.sounds.enabled;
        handScaleEnabled = cfg.item.handScaleEnabled;
        droppedScaleEnabled = cfg.item.droppedScaleEnabled;
        popScaleEnabled = cfg.popOverlay.scaleEnabled;
        retextureEnabled = cfg.retexture.enabled;
        totemOverlayEnabled = cfg.retexture.colorOverlayEnabled;
        totemOverlayHue = cfg.retexture.overlayHue;
        totemOverlayAlpha = cfg.retexture.overlayAlpha;
        autoPreview = false;
        replacementItemId = cfg.retexture.itemId;
        handScale = cfg.item.handScale;
        droppedScale = cfg.item.droppedScale;
        popScale = cfg.popOverlay.popScale;
        animationTicks = cfg.popOverlay.animationTicks;
        shieldDownEnabled = cfg.misc.shieldDownEnabled;
        shieldUpEnabled = cfg.misc.shieldUpEnabled;
        shieldWarningOverlayEnabled = cfg.misc.shieldWarningOverlayEnabled;
        fireOverlayEnabled = cfg.misc.fireOverlayEnabled;
        emptyBucketOverlayEnabled = cfg.misc.emptyBucketOverlayEnabled;
        shieldWarningOverlayR = cfg.misc.shieldWarningOverlayR;
        shieldWarningOverlayG = cfg.misc.shieldWarningOverlayG;
        shieldWarningOverlayB = cfg.misc.shieldWarningOverlayB;
        shieldWarningOverlayAlpha = cfg.misc.shieldWarningOverlayAlpha;
        emptyBucketOverlayR = cfg.misc.emptyBucketOverlayR;
        emptyBucketOverlayG = cfg.misc.emptyBucketOverlayG;
        emptyBucketOverlayB = cfg.misc.emptyBucketOverlayB;
        emptyBucketOverlayAlpha = cfg.misc.emptyBucketOverlayAlpha;
        shieldDownX = cfg.misc.shieldDownX;
        shieldDownY = cfg.misc.shieldDownY;
        shieldDownZ = cfg.misc.shieldDownZ;
        shieldDownRotX = cfg.misc.shieldDownRotX;
        shieldDownRotY = cfg.misc.shieldDownRotY;
        shieldDownRotZ = cfg.misc.shieldDownRotZ;
        shieldUpX = cfg.misc.shieldUpX;
        shieldUpY = cfg.misc.shieldUpY;
        shieldUpZ = cfg.misc.shieldUpZ;
        shieldUpRotX = cfg.misc.shieldUpRotX;
        shieldUpRotY = cfg.misc.shieldUpRotY;
        shieldUpRotZ = cfg.misc.shieldUpRotZ;
        fireOverlayHeight = cfg.misc.fireOverlayHeight;
        winOddsEnabled = cfg.pvp.winOddsEnabled;
        totemPopNametagEnabled = cfg.pvp.totemPopNametagEnabled;
        opponentStatsNametagEnabled = cfg.pvp.opponentStatsNametagEnabled;
        opponentStatsNametagFormat = TpsConfig.normalizeOpponentStatsNametagFormat(cfg.pvp.opponentStatsNametagFormat);
        pingNametagEnabled = cfg.pvp.pingNametagEnabled;
        nametagItemOrder.clear();
        nametagItemOrder.addAll(normalizeNametagItemOrder(cfg.pvp.nametagItemOrder));
        nametagItemsBeforeName.clear();
        nametagItemsBeforeName.addAll(normalizeNametagItemsBeforeName(cfg.pvp.nametagItemsBeforeName));
        dualSpectateEnabled = cfg.pvp.dualSpectateEnabled;
        dualSpectateAutoFill = cfg.pvp.dualSpectateAutoFill;
        dualSpectatePlayerOne = cfg.pvp.dualSpectatePlayerOne;
        dualSpectatePlayerTwo = cfg.pvp.dualSpectatePlayerTwo;
        dualSpectateForceThirdPerson = cfg.pvp.dualSpectateForceThirdPerson;
        dualSpectatePadding = cfg.pvp.dualSpectatePadding;
        dualSpectateMinDistance = cfg.pvp.dualSpectateMinDistance;
        dualSpectateMaxDistance = cfg.pvp.dualSpectateMaxDistance;
        dualSpectateOverheadEnabled = cfg.pvp.dualSpectateOverheadEnabled;
        dualSpectateOverheadGroupDistance = cfg.pvp.dualSpectateOverheadGroupDistance;
        dualSpectateMaxYDifference = cfg.pvp.dualSpectateMaxYDifference;
        friendFoeOverlayEnabled = cfg.pvp.friendFoeOverlayEnabled;
        friendFoeOverlayStyle = TpsConfig.normalizeFriendFoeStyle(cfg.pvp.friendFoeOverlayStyle);
        teamCountOverlayEnabled = cfg.pvp.teamCountOverlayEnabled;
        teamCountOverlayServerFilterEnabled = cfg.pvp.teamCountOverlayServerFilterEnabled;
        teamCountOverlayAllowedServers = cfg.pvp.teamCountOverlayAllowedServers;
        teamCountOverlayX = cfg.pvp.teamCountOverlayX;
        teamCountOverlayY = cfg.pvp.teamCountOverlayY;
        friendOverlayR = cfg.pvp.friendOverlayR;
        friendOverlayG = cfg.pvp.friendOverlayG;
        friendOverlayB = cfg.pvp.friendOverlayB;
        friendOverlayAlpha = cfg.pvp.friendOverlayAlpha;
        foeOverlayR = cfg.pvp.foeOverlayR;
        foeOverlayG = cfg.pvp.foeOverlayG;
        foeOverlayB = cfg.pvp.foeOverlayB;
        foeOverlayAlpha = cfg.pvp.foeOverlayAlpha;
        reachDisplayEnabled = cfg.combat.reachDisplayEnabled;
        opponentInfoEnabled = cfg.combat.opponentInfoEnabled;
        fullBrightEnabled = cfg.visual.fullBrightEnabled;
        armorHudEnabled = cfg.visual.armorHudEnabled;
        armorHudAutoPosition = cfg.visual.armorHudAutoPosition;
        armorDurabilityWarningEnabled = cfg.visual.armorDurabilityWarningEnabled;
        armorDurabilityWarningPercent = TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
        armorHudX = cfg.visual.armorHudX;
        armorHudY = cfg.visual.armorHudY;
        armorHudVertical = cfg.visual.armorHudVertical;
        armorHudSpacing = cfg.visual.armorHudSpacing;
        armorHudHotbarBorder = cfg.visual.armorHudHotbarBorder;
        armorHudDurabilityMode = cfg.visual.armorHudDurabilityMode;
        timeChangerEnabled = cfg.visual.timeChangerEnabled;
        timeOfDay = cfg.visual.timeOfDay;
        tntTimerEnabled = cfg.visual.tntTimerEnabled;
        tntTimerRange = cfg.visual.tntTimerRange;
        projectileTrailEnabled = cfg.visual.projectileTrailEnabled;
        foodOverlayEnabled = cfg.visual.foodOverlayEnabled;
        partialStatusIconsEnabled = cfg.visual.partialStatusIconsEnabled;
        TpsConfig.ParticleBurst trailBurst = firstProjectileTrailBurst(cfg);
        projectileTrailParticleId = trailBurst.particle;
        projectileTrailParticleCount = trailBurst.count;
        projectileTrailSpread = trailBurst.spreadX;
        projectileTrailSpeed = trailBurst.speed;
        streamerModeEnabled = cfg.visual.streamerModeEnabled;
        zoomEnabled = cfg.visual.zoomEnabled;
        zoomMultiplier = cfg.visual.zoomMultiplier;
        freelookEnabled = cfg.visual.freelookEnabled;
        freelookToggleMode = cfg.visual.freelookToggleMode;
        autoVillagerTraderEnabled = cfg.utility.autoVillagerTraderEnabled;
        autoVillagerTraderProfession = cfg.utility.autoVillagerTraderProfession;
        autoVillagerTraderTrade = cfg.utility.autoVillagerTraderTrade;
        autoVillagerTraderEnchantment = cfg.utility.autoVillagerTraderEnchantment;
        autoVillagerTraderCheckChests = cfg.utility.autoVillagerTraderCheckChests;
        autoVillagerTraderAutoCloseMerchant = cfg.utility.autoVillagerTraderAutoCloseMerchant;
        autoVillagerTraderRange = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
        villagerLevelerAutoDropItems = cfg.utility.villagerLevelerAutoDropItems;
        normalizeAutoVillagerTraderSelection();
        chatMacrosEnabled = cfg.macros.enabled;
        macroMessages.clear();
        for (String message : cfg.macros.messages) {
            macroMessages.add(message == null ? "" : message);
        }
        while (macroMessages.size() < TpsConfig.MIN_MACRO_SLOTS) {
            macroMessages.add("");
        }
        collapsedSections.clear();
        collapsedSections.addAll(cfg.ui.collapsedSections);
        stateLoaded = true;
    }

    void applyArmorHudLayout(int x, int y, boolean vertical, int spacing) {
        armorHudX = Math.max(-1, Math.min(10000, x));
        armorHudY = Math.max(-1, Math.min(10000, y));
        armorHudVertical = vertical;
        armorHudSpacing = Math.max(20, Math.min(64, spacing));
        changed();
    }

    void applyTeamCountOverlayLayout(int x, int y) {
        teamCountOverlayX = Math.max(-1, Math.min(10000, x));
        teamCountOverlayY = Math.max(-1, Math.min(10000, y));
        changed();
    }

    private static String nextArmorHudDurabilityMode(String mode) {
        mode = normalizeArmorHudDurabilityMode(mode);
        if (TpsConfig.ARMOR_HUD_DURABILITY_NUMBER.equals(mode)) {
            return TpsConfig.ARMOR_HUD_DURABILITY_PERCENT;
        }
        if (TpsConfig.ARMOR_HUD_DURABILITY_PERCENT.equals(mode)) {
            return TpsConfig.ARMOR_HUD_DURABILITY_BAR;
        }
        return TpsConfig.ARMOR_HUD_DURABILITY_NUMBER;
    }

    private static String armorHudDurabilityModeLabel(String mode) {
        mode = normalizeArmorHudDurabilityMode(mode);
        if (TpsConfig.ARMOR_HUD_DURABILITY_PERCENT.equals(mode)) return "Percent";
        if (TpsConfig.ARMOR_HUD_DURABILITY_BAR.equals(mode)) return "Bars";
        return "Numbers";
    }

    private static String normalizeArmorHudDurabilityMode(String mode) {
        if (TpsConfig.ARMOR_HUD_DURABILITY_PERCENT.equals(mode)
                || TpsConfig.ARMOR_HUD_DURABILITY_NUMBER.equals(mode)
                || TpsConfig.ARMOR_HUD_DURABILITY_BAR.equals(mode)) {
            return mode;
        }
        return TpsConfig.DEFAULT_ARMOR_HUD_DURABILITY_MODE;
    }

    private void changed() {
        if (initializing) return;

        // Push changes into the live config immediately so render mixins update
        // the inventory/hand/drop/overlay visuals as soon as a setting changes.
        // Saving still only controls whether those changes persist after restart.
        applyToConfig();

        status = Text.literal("Unsaved changes").formatted(Formatting.YELLOW);
        if (autoPreview) {
            previewCooldown = 8;
        }
    }

    private void save() {
        try {
            applyToConfig();
            AtomicsClient.CONFIG.save(FabricLoader.getInstance().getConfigDir().resolve("atomics_client.json"));
            status = Text.literal("Saved to config/atomics_client.json").formatted(Formatting.GREEN);
        } catch (Exception e) {
            status = Text.literal("Save failed: " + e.getMessage()).formatted(Formatting.RED);
        }
    }

    private void resetAll() {
        enabled = true;
        particlesEnabled = true;
        soundsEnabled = true;
        handScaleEnabled = false;
        droppedScaleEnabled = false;
        popScaleEnabled = false;
        retextureEnabled = false;
        totemOverlayEnabled = TpsConfig.DEFAULT_TOTEM_COLOR_OVERLAY_ENABLED;
        totemOverlayHue = TpsConfig.DEFAULT_TOTEM_OVERLAY_HUE;
        totemOverlayAlpha = TpsConfig.DEFAULT_TOTEM_OVERLAY_ALPHA;
        autoPreview = false;
        replacementItemId = TpsConfig.DEFAULT_RETEXTURE_ITEM_ID;
        handScale = TpsConfig.DEFAULT_HAND_SCALE;
        droppedScale = TpsConfig.DEFAULT_DROPPED_SCALE;
        popScale = TpsConfig.DEFAULT_POP_SCALE;
        animationTicks = TpsConfig.DEFAULT_POP_ANIMATION_TICKS;
        shieldDownEnabled = TpsConfig.DEFAULT_MISC_SHIELD_DOWN_ENABLED;
        shieldUpEnabled = TpsConfig.DEFAULT_MISC_SHIELD_UP_ENABLED;
        shieldWarningOverlayEnabled = TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_ENABLED;
        fireOverlayEnabled = TpsConfig.DEFAULT_MISC_FIRE_OVERLAY_ENABLED;
        emptyBucketOverlayEnabled = TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_ENABLED;
        shieldWarningOverlayR = TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_R;
        shieldWarningOverlayG = TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_G;
        shieldWarningOverlayB = TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_B;
        shieldWarningOverlayAlpha = TpsConfig.DEFAULT_SHIELD_WARNING_OVERLAY_ALPHA;
        emptyBucketOverlayR = TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_R;
        emptyBucketOverlayG = TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_G;
        emptyBucketOverlayB = TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_B;
        emptyBucketOverlayAlpha = TpsConfig.DEFAULT_EMPTY_BUCKET_OVERLAY_ALPHA;
        shieldDownX = TpsConfig.DEFAULT_SHIELD_DOWN_X;
        shieldDownY = TpsConfig.DEFAULT_SHIELD_DOWN_Y;
        shieldDownZ = TpsConfig.DEFAULT_SHIELD_DOWN_Z;
        shieldDownRotX = TpsConfig.DEFAULT_SHIELD_DOWN_ROT_X;
        shieldDownRotY = TpsConfig.DEFAULT_SHIELD_DOWN_ROT_Y;
        shieldDownRotZ = TpsConfig.DEFAULT_SHIELD_DOWN_ROT_Z;
        shieldUpX = TpsConfig.DEFAULT_SHIELD_UP_X;
        shieldUpY = TpsConfig.DEFAULT_SHIELD_UP_Y;
        shieldUpZ = TpsConfig.DEFAULT_SHIELD_UP_Z;
        shieldUpRotX = TpsConfig.DEFAULT_SHIELD_UP_ROT_X;
        shieldUpRotY = TpsConfig.DEFAULT_SHIELD_UP_ROT_Y;
        shieldUpRotZ = TpsConfig.DEFAULT_SHIELD_UP_ROT_Z;
        fireOverlayHeight = TpsConfig.DEFAULT_FIRE_OVERLAY_HEIGHT;
        winOddsEnabled = true;
        totemPopNametagEnabled = TpsConfig.DEFAULT_TOTEM_POP_NAMETAG_ENABLED;
        opponentStatsNametagEnabled = TpsConfig.DEFAULT_OPPONENT_STATS_NAMETAG_ENABLED;
        opponentStatsNametagFormat = TpsConfig.DEFAULT_OPPONENT_STATS_NAMETAG_FORMAT;
        pingNametagEnabled = TpsConfig.DEFAULT_PING_NAMETAG_ENABLED;
        nametagItemOrder.clear();
        nametagItemOrder.addAll(TpsConfig.defaultNametagItemOrder());
        nametagItemsBeforeName.clear();
        nametagItemsBeforeName.add(TpsConfig.NAMETAG_ITEM_OPPONENT_STATS);
        dualSpectateEnabled = false;
        dualSpectateAutoFill = false;
        dualSpectatePlayerOne = "";
        dualSpectatePlayerTwo = "";
        dualSpectateForceThirdPerson = true;
        dualSpectatePadding = 1.35f;
        dualSpectateMinDistance = 6.0f;
        dualSpectateMaxDistance = 80.0f;
        dualSpectateOverheadEnabled = TpsConfig.DEFAULT_DUAL_SPECTATE_OVERHEAD_ENABLED;
        dualSpectateOverheadGroupDistance = TpsConfig.DEFAULT_DUAL_SPECTATE_OVERHEAD_GROUP_DISTANCE;
        dualSpectateMaxYDifference = TpsConfig.DEFAULT_DUAL_SPECTATE_MAX_Y_DIFFERENCE;
        friendFoeOverlayEnabled = TpsConfig.DEFAULT_FRIEND_FOE_OVERLAY_ENABLED;
        friendFoeOverlayStyle = TpsConfig.DEFAULT_FRIEND_FOE_OVERLAY_STYLE;
        teamCountOverlayEnabled = TpsConfig.DEFAULT_TEAM_COUNT_OVERLAY_ENABLED;
        teamCountOverlayServerFilterEnabled = TpsConfig.DEFAULT_TEAM_COUNT_OVERLAY_SERVER_FILTER_ENABLED;
        teamCountOverlayAllowedServers = "";
        teamCountOverlayX = TpsConfig.DEFAULT_TEAM_COUNT_OVERLAY_X;
        teamCountOverlayY = TpsConfig.DEFAULT_TEAM_COUNT_OVERLAY_Y;
        friendOverlayR = TpsConfig.DEFAULT_FRIEND_OVERLAY_R;
        friendOverlayG = TpsConfig.DEFAULT_FRIEND_OVERLAY_G;
        friendOverlayB = TpsConfig.DEFAULT_FRIEND_OVERLAY_B;
        friendOverlayAlpha = TpsConfig.DEFAULT_FRIEND_OVERLAY_ALPHA;
        foeOverlayR = TpsConfig.DEFAULT_FOE_OVERLAY_R;
        foeOverlayG = TpsConfig.DEFAULT_FOE_OVERLAY_G;
        foeOverlayB = TpsConfig.DEFAULT_FOE_OVERLAY_B;
        foeOverlayAlpha = TpsConfig.DEFAULT_FOE_OVERLAY_ALPHA;
        reachDisplayEnabled = TpsConfig.DEFAULT_REACH_DISPLAY_ENABLED;
        opponentInfoEnabled = TpsConfig.DEFAULT_OPPONENT_INFO_ENABLED;
        fullBrightEnabled = TpsConfig.DEFAULT_FULL_BRIGHT_ENABLED;
        armorHudEnabled = TpsConfig.DEFAULT_ARMOR_HUD_ENABLED;
        armorHudAutoPosition = TpsConfig.DEFAULT_ARMOR_HUD_AUTO_POSITION;
        armorDurabilityWarningEnabled = TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_ENABLED;
        armorDurabilityWarningPercent = TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
        armorHudX = TpsConfig.DEFAULT_ARMOR_HUD_X;
        armorHudY = TpsConfig.DEFAULT_ARMOR_HUD_Y;
        armorHudVertical = TpsConfig.DEFAULT_ARMOR_HUD_VERTICAL;
        armorHudSpacing = TpsConfig.DEFAULT_ARMOR_HUD_SPACING;
        armorHudHotbarBorder = TpsConfig.DEFAULT_ARMOR_HUD_HOTBAR_BORDER;
        armorHudDurabilityMode = TpsConfig.DEFAULT_ARMOR_HUD_DURABILITY_MODE;
        timeChangerEnabled = TpsConfig.DEFAULT_TIME_CHANGER_ENABLED;
        timeOfDay = TpsConfig.DEFAULT_TIME_OF_DAY;
        tntTimerEnabled = TpsConfig.DEFAULT_TNT_TIMER_ENABLED;
        tntTimerRange = TpsConfig.DEFAULT_TNT_TIMER_RANGE;
        projectileTrailEnabled = TpsConfig.DEFAULT_PROJECTILE_TRAIL_ENABLED;
        foodOverlayEnabled = TpsConfig.DEFAULT_FOOD_OVERLAY_ENABLED;
        partialStatusIconsEnabled = TpsConfig.DEFAULT_PARTIAL_STATUS_ICONS_ENABLED;
        TpsConfig.ParticleBurst trailBurst = TpsConfig.defaultProjectileTrailParticle();
        projectileTrailParticleId = trailBurst.particle;
        projectileTrailParticleCount = trailBurst.count;
        projectileTrailSpread = trailBurst.spreadX;
        projectileTrailSpeed = trailBurst.speed;
        streamerModeEnabled = TpsConfig.DEFAULT_STREAMER_MODE_ENABLED;
        zoomEnabled = TpsConfig.DEFAULT_ZOOM_ENABLED;
        zoomMultiplier = TpsConfig.DEFAULT_ZOOM_MULTIPLIER;
        freelookEnabled = TpsConfig.DEFAULT_FREELOOK_ENABLED;
        freelookToggleMode = TpsConfig.DEFAULT_FREELOOK_TOGGLE_MODE;
        autoVillagerTraderEnabled = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_ENABLED;
        autoVillagerTraderProfession = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_PROFESSION;
        autoVillagerTraderTrade = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_TRADE;
        autoVillagerTraderEnchantment = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_ENCHANTMENT;
        autoVillagerTraderCheckChests = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_CHECK_CHESTS;
        autoVillagerTraderAutoCloseMerchant = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_AUTO_CLOSE_MERCHANT;
        autoVillagerTraderRange = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
        villagerLevelerAutoDropItems = TpsConfig.DEFAULT_VILLAGER_LEVELER_AUTO_DROP_ITEMS;
        openDropdownKey = null;
        chatMacrosEnabled = false;
        macroMessages.clear();
        for (int i = 0; i < TpsConfig.MIN_MACRO_SLOTS; i++) {
            macroMessages.add("");
        }
        collapsedSections.clear();
        if (AtomicsClient.CONFIG != null) {
            AtomicsClient.CONFIG.particles.disabledParticleIds = new ArrayList<>();
            AtomicsClient.CONFIG.particles.bursts = new ArrayList<>(List.of(TpsConfig.defaultParticleBurst()));
            AtomicsClient.CONFIG.visual.projectileTrailParticles = new ArrayList<>(List.of(TpsConfig.defaultProjectileTrailParticle()));
            AtomicsClient.CONFIG.sounds.sounds = new ArrayList<>(List.of(TpsConfig.defaultSoundPlay()));
            AtomicsClient.CONFIG.pvp.friendNames = new ArrayList<>();
            AtomicsClient.CONFIG.pvp.foeNames = new ArrayList<>();
            AtomicsClient.CONFIG.inventorySorter = new TpsConfig.InventorySorterSettings();
            AtomicsClient.CONFIG.utility.villagerLevelerTargetUuid = TpsConfig.DEFAULT_VILLAGER_LEVELER_TARGET_UUID;
            AtomicsClient.CONFIG.utility.villagerLevelerAutoDropItems = TpsConfig.DEFAULT_VILLAGER_LEVELER_AUTO_DROP_ITEMS;
        }
        changed();
        clearAndInit();
    }

    private void applyToConfig() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) {
            cfg = new TpsConfig();
            AtomicsClient.CONFIG = cfg;
        }
        cfg.normalize();
        cfg.enabled = enabled;
        cfg.particles.enabled = particlesEnabled;
        cfg.sounds.enabled = soundsEnabled;
        cfg.item.handScaleEnabled = handScaleEnabled;
        cfg.item.droppedScaleEnabled = droppedScaleEnabled;
        cfg.popOverlay.scaleEnabled = popScaleEnabled;
        cfg.retexture.enabled = retextureEnabled;
        cfg.item.handScale = handScale;
        cfg.item.droppedScale = droppedScale;
        cfg.popOverlay.popScale = popScale;
        cfg.popOverlay.animationTicks = animationTicks;
        cfg.retexture.itemId = replacementItemId == null || replacementItemId.trim().isEmpty() ? TpsConfig.DEFAULT_RETEXTURE_ITEM_ID : replacementItemId.trim();
        cfg.retexture.colorOverlayEnabled = totemOverlayEnabled;
        cfg.retexture.overlayHue = totemOverlayHue;
        cfg.retexture.overlayAlpha = totemOverlayAlpha;
        cfg.misc.shieldDownEnabled = shieldDownEnabled;
        cfg.misc.shieldUpEnabled = shieldUpEnabled;
        cfg.misc.shieldWarningOverlayEnabled = shieldWarningOverlayEnabled;
        cfg.misc.fireOverlayEnabled = fireOverlayEnabled;
        cfg.misc.emptyBucketOverlayEnabled = emptyBucketOverlayEnabled;
        cfg.misc.shieldWarningOverlayR = Math.max(0, Math.min(255, shieldWarningOverlayR));
        cfg.misc.shieldWarningOverlayG = Math.max(0, Math.min(255, shieldWarningOverlayG));
        cfg.misc.shieldWarningOverlayB = Math.max(0, Math.min(255, shieldWarningOverlayB));
        cfg.misc.shieldWarningOverlayAlpha = Math.max(0.0f, Math.min(1.0f, shieldWarningOverlayAlpha));
        cfg.misc.emptyBucketOverlayR = Math.max(0, Math.min(255, emptyBucketOverlayR));
        cfg.misc.emptyBucketOverlayG = Math.max(0, Math.min(255, emptyBucketOverlayG));
        cfg.misc.emptyBucketOverlayB = Math.max(0, Math.min(255, emptyBucketOverlayB));
        cfg.misc.emptyBucketOverlayAlpha = Math.max(0.0f, Math.min(1.0f, emptyBucketOverlayAlpha));
        cfg.misc.shieldDownX = shieldDownX;
        cfg.misc.shieldDownY = shieldDownY;
        cfg.misc.shieldDownZ = shieldDownZ;
        cfg.misc.shieldDownRotX = shieldDownRotX;
        cfg.misc.shieldDownRotY = shieldDownRotY;
        cfg.misc.shieldDownRotZ = shieldDownRotZ;
        cfg.misc.shieldUpX = shieldUpX;
        cfg.misc.shieldUpY = shieldUpY;
        cfg.misc.shieldUpZ = shieldUpZ;
        cfg.misc.shieldUpRotX = shieldUpRotX;
        cfg.misc.shieldUpRotY = shieldUpRotY;
        cfg.misc.shieldUpRotZ = shieldUpRotZ;
        cfg.misc.fireOverlayHeight = fireOverlayHeight;
        cfg.pvp.winOddsEnabled = winOddsEnabled;
        cfg.pvp.totemPopNametagEnabled = totemPopNametagEnabled;
        cfg.pvp.opponentStatsNametagEnabled = opponentStatsNametagEnabled;
        cfg.pvp.opponentStatsNametagFormat = TpsConfig.normalizeOpponentStatsNametagFormat(opponentStatsNametagFormat);
        cfg.pvp.pingNametagEnabled = pingNametagEnabled;
        cfg.pvp.nametagItemOrder = normalizeNametagItemOrder(nametagItemOrder);
        cfg.pvp.nametagItemsBeforeName = normalizeNametagItemsBeforeName(nametagItemsBeforeName);
        cfg.pvp.spoofedHealthMode = TpsConfig.PVP_HEALTH_MODE_PREFER_SRV;
        cfg.pvp.dualSpectateEnabled = dualSpectateEnabled;
        cfg.pvp.dualSpectateAutoFill = dualSpectateAutoFill;
        cfg.pvp.dualSpectatePlayerOne = dualSpectatePlayerOne == null ? "" : dualSpectatePlayerOne.trim();
        cfg.pvp.dualSpectatePlayerTwo = dualSpectatePlayerTwo == null ? "" : dualSpectatePlayerTwo.trim();
        cfg.pvp.dualSpectateForceThirdPerson = dualSpectateForceThirdPerson;
        cfg.pvp.dualSpectatePadding = Math.max(1.0f, Math.min(2.5f, dualSpectatePadding));
        cfg.pvp.dualSpectateMinDistance = Math.max(2.0f, Math.min(30.0f, dualSpectateMinDistance));
        cfg.pvp.dualSpectateMaxDistance = Math.max(10.0f, Math.min(160.0f, dualSpectateMaxDistance));
        cfg.pvp.dualSpectateOverheadEnabled = dualSpectateOverheadEnabled;
        cfg.pvp.dualSpectateOverheadGroupDistance = Math.max(4.0f, Math.min(80.0f, dualSpectateOverheadGroupDistance));
        cfg.pvp.dualSpectateMaxYDifference = Math.max(2.0f, Math.min(48.0f, dualSpectateMaxYDifference));
        cfg.pvp.friendFoeOverlayEnabled = friendFoeOverlayEnabled;
        cfg.pvp.friendFoeOverlayStyle = TpsConfig.normalizeFriendFoeStyle(friendFoeOverlayStyle);
        cfg.pvp.teamCountOverlayEnabled = teamCountOverlayEnabled;
        cfg.pvp.teamCountOverlayServerFilterEnabled = teamCountOverlayServerFilterEnabled;
        cfg.pvp.teamCountOverlayAllowedServers = teamCountOverlayAllowedServers == null ? "" : teamCountOverlayAllowedServers.trim();
        cfg.pvp.teamCountOverlayX = Math.max(-1, Math.min(10000, teamCountOverlayX));
        cfg.pvp.teamCountOverlayY = Math.max(-1, Math.min(10000, teamCountOverlayY));
        cfg.pvp.friendOverlayR = Math.max(0, Math.min(255, friendOverlayR));
        cfg.pvp.friendOverlayG = Math.max(0, Math.min(255, friendOverlayG));
        cfg.pvp.friendOverlayB = Math.max(0, Math.min(255, friendOverlayB));
        cfg.pvp.friendOverlayAlpha = Math.max(0.0f, Math.min(1.0f, friendOverlayAlpha));
        cfg.pvp.foeOverlayR = Math.max(0, Math.min(255, foeOverlayR));
        cfg.pvp.foeOverlayG = Math.max(0, Math.min(255, foeOverlayG));
        cfg.pvp.foeOverlayB = Math.max(0, Math.min(255, foeOverlayB));
        cfg.pvp.foeOverlayAlpha = Math.max(0.0f, Math.min(1.0f, foeOverlayAlpha));
        cfg.combat.reachDisplayEnabled = reachDisplayEnabled;
        cfg.combat.opponentInfoEnabled = opponentInfoEnabled;
        cfg.visual.fullBrightEnabled = fullBrightEnabled;
        cfg.visual.armorHudEnabled = armorHudEnabled;
        cfg.visual.armorHudAutoPosition = armorHudAutoPosition;
        cfg.visual.armorDurabilityWarningEnabled = armorDurabilityWarningEnabled;
        cfg.visual.armorDurabilityWarningPercent = TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
        cfg.visual.armorHudX = Math.max(-1, Math.min(10000, armorHudX));
        cfg.visual.armorHudY = Math.max(-1, Math.min(10000, armorHudY));
        cfg.visual.armorHudVertical = armorHudVertical;
        cfg.visual.armorHudSpacing = Math.max(20, Math.min(64, armorHudSpacing));
        cfg.visual.armorHudHotbarBorder = armorHudHotbarBorder;
        cfg.visual.armorHudDurabilityMode = normalizeArmorHudDurabilityMode(armorHudDurabilityMode);
        cfg.visual.timeChangerEnabled = timeChangerEnabled;
        cfg.visual.timeOfDay = Math.max(0, Math.min(24000, timeOfDay));
        cfg.visual.tntTimerEnabled = tntTimerEnabled;
        cfg.visual.tntTimerRange = Math.max(8, Math.min(128, tntTimerRange));
        cfg.visual.projectileTrailEnabled = projectileTrailEnabled;
        cfg.visual.foodOverlayEnabled = foodOverlayEnabled;
        cfg.visual.foodOverlayHue = TpsConfig.DEFAULT_FOOD_OVERLAY_HUE;
        cfg.visual.foodOverlayAlpha = TpsConfig.DEFAULT_FOOD_OVERLAY_ALPHA;
        cfg.visual.partialStatusIconsEnabled = partialStatusIconsEnabled;
        TpsConfig.ParticleBurst trailBurst = TpsConfig.defaultProjectileTrailParticle();
        trailBurst.particle = projectileTrailParticleId == null || projectileTrailParticleId.trim().isEmpty()
                ? TpsConfig.DEFAULT_PROJECTILE_TRAIL_PARTICLE_ID
                : projectileTrailParticleId.trim();
        trailBurst.count = Math.max(0, Math.min(20, projectileTrailParticleCount));
        trailBurst.spreadX = Math.max(0.0, Math.min(1.5, projectileTrailSpread));
        trailBurst.spreadY = trailBurst.spreadX;
        trailBurst.spreadZ = trailBurst.spreadX;
        trailBurst.speed = Math.max(0.0, Math.min(1.0, projectileTrailSpeed));
        cfg.visual.projectileTrailParticles = new ArrayList<>(List.of(trailBurst));
        cfg.visual.streamerModeEnabled = streamerModeEnabled;
        cfg.visual.zoomEnabled = zoomEnabled;
        cfg.visual.zoomMultiplier = Math.max(TpsConfig.MIN_ZOOM_MULTIPLIER, Math.min(TpsConfig.MAX_ZOOM_MULTIPLIER, zoomMultiplier));
        cfg.visual.freelookEnabled = freelookEnabled;
        cfg.visual.freelookToggleMode = freelookToggleMode;
        normalizeAutoVillagerTraderSelection();
        cfg.utility.autoVillagerTraderEnabled = autoVillagerTraderEnabled;
        cfg.utility.autoVillagerTraderProfession = autoVillagerTraderProfession == null ? "" : autoVillagerTraderProfession.trim();
        cfg.utility.autoVillagerTraderTrade = autoVillagerTraderTrade == null ? "" : autoVillagerTraderTrade.trim();
        cfg.utility.autoVillagerTraderEnchantment = autoVillagerTraderEnchantment == null ? "" : autoVillagerTraderEnchantment.trim();
        cfg.utility.autoVillagerTraderCheckChests = autoVillagerTraderCheckChests;
        cfg.utility.autoVillagerTraderAutoCloseMerchant = autoVillagerTraderAutoCloseMerchant;
        cfg.utility.autoVillagerTraderRange = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
        cfg.utility.villagerLevelerAutoDropItems = villagerLevelerAutoDropItems;
        cfg.macros.enabled = chatMacrosEnabled;
        int macroCount = Math.max(TpsConfig.MIN_MACRO_SLOTS, Math.min(TpsConfig.MAX_MACRO_SLOTS, macroMessages.size()));
        String[] normalizedMacros = new String[macroCount];
        for (int i = 0; i < normalizedMacros.length; i++) {
            normalizedMacros[i] = trimMacro(i < macroMessages.size() ? macroMessages.get(i) : "");
        }
        cfg.macros.messages = normalizedMacros;
        cfg.ui.collapsedSections = new ArrayList<>(collapsedSections);
    }

    private void previewNow() {
        applyToConfig();
        playPreview();

        MinecraftClient client = MinecraftClient.getInstance();
        int delayTicks = Math.max(1, animationTicks) + 10;
        long delayMs = delayTicks * 50L;
        client.setScreen(null);

        Thread reopenThread = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                return;
            }
            client.execute(() -> {
                if (client.currentScreen == null) {
                    client.setScreen(this);
                }
            });
        }, "AtomicsClient-PreviewReopen");
        reopenThread.setDaemon(true);
        reopenThread.start();
    }

    private void playPreview() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            status = Text.literal("Preview failed: no player loaded").formatted(Formatting.RED);
            return;
        }
        client.gameRenderer.showFloatingItem(Items.TOTEM_OF_UNDYING.getDefaultStack());
        TotemPopEffects.play(client.player);
        status = Text.literal("Preview played").formatted(Formatting.AQUA);
    }

    @Override
    public void tick() {
        super.tick();
        if (searchRefreshQueued) {
            searchRefreshQueued = false;
            clearAndInit();
            return;
        }
        if (settingsSearchFocused && settingsSearchField != null && getFocused() != settingsSearchField) {
            focusSettingsSearchField();
        }
        if (++dualSpectateAutoFillRefreshTicks >= 5) {
            dualSpectateAutoFillRefreshTicks = 0;
            refreshDualSpectateAutoFill();
        }
        if (previewCooldown > 0) {
            previewCooldown--;
            if (previewCooldown == 0) playPreview();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (settingsSearchField != null && settingsSearchField.isMouseOver(click.x(), click.y())) {
            settingsSearchFocused = true;
            focusSettingsSearchField();
            settingsSearchField.mouseClicked(click, doubleClick);
            return true;
        }
        settingsSearchFocused = false;
        if (settingsSearchField != null && getFocused() == settingsSearchField) {
            setFocused(null);
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (settingsSearchFocused && settingsSearchField != null && settingsSearchField.keyPressed(keyInput)) {
            return true;
        }
        if (listeningKeyBinding != null) {
            int key = keyInput.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                status = Text.literal("Keybind change cancelled").formatted(Formatting.YELLOW);
                listeningKeyBinding = null;
                listeningKeyLabel = null;
                clearAndInit();
                return true;
            }

            InputUtil.Key boundKey = key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE
                    ? InputUtil.UNKNOWN_KEY
                    : InputUtil.fromKeyCode(keyInput);
            AtomicsClient.setKeyBinding(listeningKeyBinding, boundKey);
            String label = listeningKeyLabel == null ? "Keybind" : listeningKeyLabel;
            status = Text.literal(label + " set to " + AtomicsClient.keyBindingName(listeningKeyBinding)).formatted(Formatting.GREEN);
            listeningKeyBinding = null;
            listeningKeyLabel = null;
            clearAndInit();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (settingsSearchFocused && settingsSearchField != null && settingsSearchField.charTyped(charInput)) {
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    protected void setInitialFocus() {
        if (settingsSearchFocused && settingsSearchField != null) {
            focusSettingsSearchField();
            return;
        }
        super.setInitialFocus();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= leftX && mouseX <= leftX + leftWidth && mouseY >= contentTop && mouseY <= contentBottom && maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 24)));
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AtomicsGuiStyle.drawBackground(context, this.width, this.height);
        AtomicsGuiStyle.drawBars(context, this.width, this.height, TOP_BAR_HEIGHT, FOOTER_HEIGHT);

        drawPanel(context, leftX - 8, contentTop - 8, leftWidth + 16, contentBottom - contentTop + 8);
        if (selectedTab.hasPreview) {
            drawPanel(context, previewX - 8, contentTop - 8, previewWidth + 16, contentBottom - contentTop + 8);
            context.fill(previewX - 9, contentTop - 8, previewX - 8, contentBottom, PANEL_BORDER);
        }

        AtomicsGuiStyle.drawTitle(context, this.textRenderer, Text.literal("Atomics Client"), this.width);
        AtomicsGuiStyle.drawSubtitle(context, this.textRenderer, Text.literal(selectedTab.label + " Settings"), this.width);

        super.render(context, mouseX, mouseY, delta);

        for (DrawLabel label : labels) {
            if (label.y < contentTop - 2 || label.y > contentBottom - 10) continue;
            if (label.lineWidth > 0) {
                context.fill(label.x, label.y, label.x + label.lineWidth, label.y + 1, label.color);
            } else if (label.centered) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label.text), label.x, label.y, textColor(label.color));
            } else {
                context.drawTextWithShadow(this.textRenderer, Text.literal(label.text), label.x, label.y, textColor(label.color));
            }
        }

        if (selectedTab.hasPreview) {
            renderPreviewPanel(context, mouseX, mouseY);
        }
        renderScrollBar(context);
        renderStatus(context);
    }

    private void drawPanel(DrawContext context, int x, int y, int width, int height) {
        AtomicsGuiStyle.drawPanel(context, x, y, width, height);
    }

    private void renderStatus(DrawContext context) {
        String text = status == null ? "" : status.getString();
        if (text.isBlank()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Changes are live. Save to keep them after restart."), OUTER_MARGIN, this.height - 13, TEXT_MUTED);
            return;
        }
        int width = this.textRenderer.getWidth(status) + 16;
        int x = OUTER_MARGIN;
        int y = this.height - 23;
        context.fill(x, y, x + width, y + 17, AtomicsGuiStyle.ROW);
        context.fill(x, y, x + 3, y + 18, ACCENT);
        context.drawTextWithShadow(this.textRenderer, status, x + 8, y + 5, TEXT_MAIN);
    }

    private void renderPreviewPanel(DrawContext context, int mouseX, int mouseY) {
        int x = previewX;
        int y = contentTop;
        int w = previewWidth;
        ItemStack previewStack = AtomicsClient.getPreviewTotemStack();
        context.drawTextWithShadow(this.textRenderer, Text.literal(selectedTab.label + " Preview"), x, y - 2, TEXT_MAIN);
        int boxTop = y + 22;
        int boxBottom = contentBottom - 12;
        context.fill(x, boxTop, x + w, boxBottom, AtomicsGuiStyle.ROW);
        context.drawStrokedRectangle(x, boxTop, w, boxBottom - boxTop, PANEL_BORDER);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(selectedTab.label + " Preview"), x + w / 2, boxTop + 14, TEXT_MAIN);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && selectedTab == Tab.TOTEM) {
            int entityLeft = x + 18;
            int entityTop = boxTop + 54;
            int entityRight = x + w / 2 + 18;
            int entityBottom = boxBottom - 28;
            int entitySize = Math.max(38, Math.min(52, (entityBottom - entityTop) / 5));
            InventoryScreen.drawEntity(context, entityLeft, entityTop, entityRight, entityBottom, entitySize, 0.06f, mouseX, mouseY, client.player);

            int itemX = x + w * 3 / 4;
            int startY = boxTop + 76;
            int gap = Math.max(58, (boxBottom - boxTop - 164) / 2);
            renderTotemSizePreview(context, previewStack, "Pop Overlay", itemX, startY, popScaleEnabled ? popScale : 1.0f);
            renderTotemSizePreview(context, previewStack, "Held Totem", itemX, startY + gap, handScaleEnabled ? handScale : 1.0f);
            renderTotemSizePreview(context, previewStack, "Dropped Totem", itemX, startY + gap * 2, droppedScaleEnabled ? droppedScale : 1.0f);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Preview will appear here"), x + w / 2, boxTop + (boxBottom - boxTop) / 2, TEXT_MUTED);
        }
    }
    private int renderTextPanel(DrawContext context, String title, String[] lines, int x, int y, int width) {
        int padding = 8;
        int lineHeight = 13;
        int height = padding * 2 + 14 + lines.length * lineHeight;
        int bottom = contentBottom - 10;
        if (y + height < contentTop || y > bottom) {
            return y + height;
        }
        int visibleTop = Math.max(y, contentTop);
        int visibleBottom = Math.min(y + height, bottom);
        context.fill(x, visibleTop, x + width, visibleBottom, AtomicsGuiStyle.ROW);
        drawClippedPanelBorder(context, x, y, width, height, visibleTop, visibleBottom, PANEL_BORDER);
        if (y + padding >= contentTop && y + padding <= bottom) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(title), x + padding, y + padding, TEXT_MAIN);
        }
        for (int i = 0; i < lines.length; i++) {
            int textY = y + padding + 16 + i * lineHeight;
            if (textY >= contentTop && textY <= bottom) {
                StyledLine line = parseStyledLine(lines[i]);
                context.drawTextWithShadow(this.textRenderer, Text.literal(line.text), x + padding, textY, textColor(line.color));
            }
        }
        return y + height;
    }

    private void drawClippedPanelBorder(DrawContext context, int x, int y, int width, int height, int visibleTop, int visibleBottom, int color) {
        if (y >= visibleTop && y < visibleBottom) {
            context.fill(x, y, x + width, y + 1, color);
        }
        int bottom = y + height - 1;
        if (bottom >= visibleTop && bottom < visibleBottom) {
            context.fill(x, bottom, x + width, bottom + 1, color);
        }
        context.fill(x, visibleTop, x + 1, visibleBottom, color);
        context.fill(x + width - 1, visibleTop, x + width, visibleBottom, color);
    }

    private void renderTotemSizePreview(DrawContext context, ItemStack stack, String label, int centerX, int centerY, float scale) {
        String display = label + "  " + formatDecimal(scale, 2) + "x";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(display), centerX, centerY - 22, TEXT_MAIN);
        renderScaledItem(context, stack, centerX, centerY, scale);
    }

    private void renderScrollBar(DrawContext context) {
        if (maxScroll <= 0) return;
        int barX = leftX + leftWidth + 4;
        int trackTop = contentTop;
        int trackBottom = contentBottom - 4;
        context.fill(barX, trackTop, barX + 4, trackBottom, 0x66000000);
        int trackH = trackBottom - trackTop;
        int thumbH = Math.max(24, trackH * trackH / (trackH + maxScroll));
        int thumbY = trackTop + (trackH - thumbH) * scrollOffset / maxScroll;
        context.fill(barX, thumbY, barX + 4, thumbY + thumbH, ACCENT);
    }

    private void renderScaledItem(DrawContext context, ItemStack stack, int centerX, int centerY, float scale) {
        float clampedScale = Math.max(0.01f, Math.min(3.0f, scale));
        float itemSize = 16.0f * clampedScale;
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX - itemSize / 2.0f, centerY - itemSize / 2.0f);
        matrices.scale(clampedScale, clampedScale);
        context.drawItem(stack, 0, 0);
        if (totemOverlayEnabled) {
            int[] rgb = hueAdjustmentToRgb(totemOverlayHue);
            int color = colorWithAlpha(rgb[0], rgb[1], rgb[2], totemOverlayAlpha);
            context.fill(0, 0, 16, 16, color);
        }
        matrices.popMatrix();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    private int getParticleCount() {
        return AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.particles == null || AtomicsClient.CONFIG.particles.bursts == null ? 0 : AtomicsClient.CONFIG.particles.bursts.size();
    }

    private TpsConfig getLiveConfig() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) {
            cfg = new TpsConfig();
            AtomicsClient.CONFIG = cfg;
        }
        return cfg.normalize();
    }

    private static TpsConfig.ParticleBurst firstProjectileTrailBurst(TpsConfig cfg) {
        if (cfg == null) {
            return TpsConfig.defaultProjectileTrailParticle();
        }
        cfg.normalize();
        if (cfg.visual.projectileTrailParticles == null || cfg.visual.projectileTrailParticles.isEmpty()) {
            return TpsConfig.defaultProjectileTrailParticle();
        }
        return cfg.visual.projectileTrailParticles.get(0);
    }

    private static String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimMacro(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > 256 ? trimmed.substring(0, 256) : trimmed;
    }

    private int getSoundCount() {
        return AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.sounds == null || AtomicsClient.CONFIG.sounds.sounds == null ? 0 : AtomicsClient.CONFIG.sounds.sounds.size();
    }

    private int getFriendFoeCount() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null || cfg.pvp == null) {
            return 0;
        }
        int friends = cfg.pvp.friendNames == null ? 0 : cfg.pvp.friendNames.size();
        int foes = cfg.pvp.foeNames == null ? 0 : cfg.pvp.foeNames.size();
        return friends + foes;
    }

    private int getInventorySortKitCount() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        return cfg == null || cfg.inventorySorter == null || cfg.inventorySorter.kits == null ? 0 : cfg.inventorySorter.kits.size();
    }

    private List<String> enabledNametagItems() {
        List<String> items = new ArrayList<>();
        for (String item : normalizeNametagItemOrder(nametagItemOrder)) {
            if (isNametagItemEnabled(item)) {
                items.add(item);
            }
        }
        return items;
    }

    private boolean isNametagItemEnabled(String item) {
        if (TpsConfig.NAMETAG_ITEM_WIN_ODDS.equals(item)) {
            return winOddsEnabled;
        }
        if (TpsConfig.NAMETAG_ITEM_TOTEM_POPS.equals(item)) {
            return totemPopNametagEnabled;
        }
        if (TpsConfig.NAMETAG_ITEM_OPPONENT_STATS.equals(item)) {
            return opponentStatsNametagEnabled;
        }
        if (TpsConfig.NAMETAG_ITEM_PING.equals(item)) {
            return pingNametagEnabled;
        }
        return false;
    }

    private void moveNametagItem(String item, int direction) {
        List<String> enabledItems = enabledNametagItems();
        int enabledIndex = enabledItems.indexOf(item);
        int targetEnabledIndex = enabledIndex + direction;
        if (enabledIndex < 0 || targetEnabledIndex < 0 || targetEnabledIndex >= enabledItems.size()) {
            return;
        }

        String targetItem = enabledItems.get(targetEnabledIndex);
        int from = nametagItemOrder.indexOf(item);
        int to = nametagItemOrder.indexOf(targetItem);
        if (from < 0 || to < 0) {
            return;
        }
        nametagItemOrder.remove(from);
        nametagItemOrder.add(to, item);
        List<String> normalizedOrder = normalizeNametagItemOrder(nametagItemOrder);
        nametagItemOrder.clear();
        nametagItemOrder.addAll(normalizedOrder);
    }

    private void toggleNametagItemSide(String item) {
        if (!TpsConfig.isKnownNametagItem(item)) {
            return;
        }
        if (nametagItemsBeforeName.contains(item)) {
            nametagItemsBeforeName.removeIf(item::equals);
        } else {
            nametagItemsBeforeName.add(item);
        }
        List<String> normalizedBeforeName = normalizeNametagItemsBeforeName(nametagItemsBeforeName);
        nametagItemsBeforeName.clear();
        nametagItemsBeforeName.addAll(normalizedBeforeName);
    }

    private static List<String> normalizeNametagItemOrder(List<String> items) {
        List<String> normalized = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                if (TpsConfig.isKnownNametagItem(item) && !normalized.contains(item)) {
                    normalized.add(item);
                }
            }
        }
        for (String item : TpsConfig.defaultNametagItemOrder()) {
            if (!normalized.contains(item)) {
                normalized.add(item);
            }
        }
        return normalized;
    }

    private static List<String> normalizeNametagItemsBeforeName(List<String> items) {
        List<String> normalized = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                if (TpsConfig.isKnownNametagItem(item) && !normalized.contains(item)) {
                    normalized.add(item);
                }
            }
        }
        return normalized;
    }
    private static String nametagItemLabel(String value) {
        return switch (value) {
            case TpsConfig.NAMETAG_ITEM_WIN_ODDS -> "Win Percent";
            case TpsConfig.NAMETAG_ITEM_TOTEM_POPS -> "Totem Pops";
            case TpsConfig.NAMETAG_ITEM_OPPONENT_STATS -> "Opponent Stats";
            case TpsConfig.NAMETAG_ITEM_PING -> "Ping";
            default -> "Unknown";
        };
    }

    private static String opponentStatsNametagFormatLabel(String value) {
        return switch (TpsConfig.normalizeOpponentStatsNametagFormat(value)) {
            case TpsConfig.OPPONENT_STATS_NAMETAG_TIER -> "Tier Only";
            case TpsConfig.OPPONENT_STATS_NAMETAG_MODE_TIER -> "Mode + Tier";
            default -> "Icon + Tier";
        };
    }

    private static String nextOpponentStatsNametagFormat(String value) {
        return switch (TpsConfig.normalizeOpponentStatsNametagFormat(value)) {
            case TpsConfig.OPPONENT_STATS_NAMETAG_ICON_TIER -> TpsConfig.OPPONENT_STATS_NAMETAG_TIER;
            case TpsConfig.OPPONENT_STATS_NAMETAG_TIER -> TpsConfig.OPPONENT_STATS_NAMETAG_MODE_TIER;
            default -> TpsConfig.OPPONENT_STATS_NAMETAG_ICON_TIER;
        };
    }

    private static String friendFoeStyleLabel(String value) {
        return switch (TpsConfig.normalizeFriendFoeStyle(value)) {
            case TpsConfig.FRIEND_FOE_STYLE_OUTLINE -> "Outline";
            case TpsConfig.FRIEND_FOE_STYLE_OUTLINE_FULL -> "Outline + Full";
            case TpsConfig.FRIEND_FOE_STYLE_PULSE -> "Pulse";
            default -> "Full Overlay";
        };
    }

    private static String nextFriendFoeStyle(String value) {
        return switch (TpsConfig.normalizeFriendFoeStyle(value)) {
            case TpsConfig.FRIEND_FOE_STYLE_FULL -> TpsConfig.FRIEND_FOE_STYLE_OUTLINE;
            case TpsConfig.FRIEND_FOE_STYLE_OUTLINE -> TpsConfig.FRIEND_FOE_STYLE_OUTLINE_FULL;
            case TpsConfig.FRIEND_FOE_STYLE_OUTLINE_FULL -> TpsConfig.FRIEND_FOE_STYLE_PULSE;
            default -> TpsConfig.FRIEND_FOE_STYLE_FULL;
        };
    }

    private static Text rowText(String label, String value) { return Text.literal(label + ": " + value); }
    private static Text graphToggleText(String label, boolean visible) { return Text.literal(label + " " + (visible ? "ON" : "OFF")); }
    private static int[] hueAdjustmentToRgb(float hueAdjustment) {
        float hue = ((hueAdjustment % 360.0f) + 360.0f) % 360.0f;
        java.awt.Color color = java.awt.Color.getHSBColor(hue / 360.0f, 1.0f, 1.0f);
        return new int[]{color.getRed(), color.getGreen(), color.getBlue()};
    }

    private static int colorWithAlpha(int r, int g, int b, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        int cr = Math.max(0, Math.min(255, r));
        int cg = Math.max(0, Math.min(255, g));
        int cb = Math.max(0, Math.min(255, b));
        return (a << 24) | (cr << 16) | (cg << 8) | cb;
    }
    private static int textColor(int color) { return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color; }
    private static String formatSigned(double value) { return String.format(Locale.US, "%+.3f", value); }
    private static String formatDegrees(double value) { return String.format(Locale.US, "%+.0f°", value); }
    private static String formatPercent(double value) { return String.format(Locale.US, "%.0f%%", value * 100.0); }
    private static String formatDecimal(double value, int decimals) { return String.format(Locale.US, "%." + decimals + "f", value); }
    private static StyledLine parseStyledLine(String value) {
        int separator = value.lastIndexOf('|');
        if (separator < 0 || separator == value.length() - 1) {
            return new StyledLine(value, 0xD8D8D8);
        }
        try {
            return new StyledLine(value.substring(0, separator), Integer.parseUnsignedInt(value.substring(separator + 1), 16));
        } catch (NumberFormatException e) {
            return new StyledLine(value, 0xD8D8D8);
        }
    }

    private enum Tab {
        TOTEM("Totem", true), PVP("Combat", false), STATS("HUD", false), TOOLS("View", false), MISC("Items", false), KEYBINDS("Controls", false), SEARCH("Find", false);
        private final String label;
        private final boolean hasPreview;
        Tab(String label, boolean hasPreview) { this.label = label; this.hasPreview = hasPreview; }
    }

    private class TabButtonWidget extends ClickableWidget {
        private final String label;
        private final boolean selected;
        private final Runnable action;

        private TabButtonWidget(int x, int y, int width, int height, String label, boolean selected, Runnable action) {
            super(x, y, width, height, Text.literal(label));
            this.label = label;
            this.selected = selected;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int bg = selected ? AtomicsGuiStyle.PANEL_SELECTED : isHovered() ? AtomicsGuiStyle.ROW_HOVER : AtomicsGuiStyle.ROW;
            int border = selected ? ACCENT : isHovered() ? AtomicsGuiStyle.PANEL_BORDER_HOVER : PANEL_BORDER;
            context.fill(x, y, x + width, y + height, bg);
            context.fill(x, y, x + width, y + 1, border);
            context.fill(x, y + height - 1, x + width, y + height, border);
            context.fill(x, y, x + 1, y + height, border);
            context.fill(x + width - 1, y, x + width, y + height, border);
            if (selected) {
                context.fill(x + 4, y + height - 3, x + width - 4, y + height - 1, ACCENT);
            }
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + width / 2, y + 8, selected ? TEXT_MAIN : TEXT_MUTED);
        }

        @Override
        public void onClick(net.minecraft.client.gui.Click click, boolean doubleClick) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    private class SectionHeaderWidget extends ClickableWidget {
        private final String title;
        private final boolean collapsed;
        private final Runnable action;

        private SectionHeaderWidget(int x, int y, int width, int height, String title, boolean collapsed, Runnable action) {
            super(x, y, width, height, Text.literal(title));
            this.title = title;
            this.collapsed = collapsed;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int bg = isHovered() ? AtomicsGuiStyle.ROW_HOVER : AtomicsGuiStyle.ROW;
            context.fill(x, y, x + width, y + height, bg);
            context.fill(x, y, x + width, y + 1, PANEL_BORDER);
            context.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
            context.fill(x, y, x + 3, y + height, collapsed ? AtomicsGuiStyle.TEXT_DIM : ACCENT);

            String marker = collapsed ? ">" : "v";
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(marker), x + 15, y + 7, collapsed ? TEXT_MUTED : TEXT_MAIN);
            context.drawTextWithShadow(textRenderer, Text.literal(title), x + 28, y + 7, TEXT_MAIN);
            String state = collapsed ? "show" : "hide";
            context.drawTextWithShadow(textRenderer, Text.literal(state), x + width - textRenderer.getWidth(state) - 10, y + 7, TEXT_MUTED);
        }

        @Override
        public void onClick(net.minecraft.client.gui.Click click, boolean doubleClick) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    private class DropdownButtonWidget extends ClickableWidget {
        private final String label;
        private final String value;
        private final boolean open;
        private final Runnable action;

        private DropdownButtonWidget(int x, int y, int width, int height, String label, String value, boolean open, Runnable action) {
            super(x, y, width, height, Text.literal(label));
            this.label = label;
            this.value = value;
            this.open = open;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int bg = isHovered() ? AtomicsGuiStyle.ROW_HOVER : AtomicsGuiStyle.ROW;
            context.fill(x, y, x + width, y + height, bg);
            context.fill(x, y, x + width, y + 1, open ? ACCENT : PANEL_BORDER);
            context.fill(x, y + height - 1, x + width, y + height, open ? ACCENT : PANEL_BORDER);
            context.fill(x, y, x + 1, y + height, PANEL_BORDER);
            context.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);

            String marker = open ? "^" : "v";
            String suffix = "  " + marker;
            int valueWidth = Math.max(24, width - textRenderer.getWidth(label) - textRenderer.getWidth(suffix) - 28);
            String trimmedValue = textRenderer.trimToWidth(value, valueWidth);
            String right = trimmedValue + suffix;
            context.drawTextWithShadow(textRenderer, Text.literal(label), x + 8, y + 7, TEXT_MAIN);
            context.drawTextWithShadow(textRenderer, Text.literal(right), x + width - textRenderer.getWidth(right) - 8, y + 7, open ? ACCENT : TEXT_MUTED);
        }

        @Override
        public void onClick(net.minecraft.client.gui.Click click, boolean doubleClick) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    private static class DrawLabel {
        final String text; final int x; final int y; final int color; final boolean centered; final int lineWidth;
        DrawLabel(String text, int x, int y, int color) { this(text, x, y, color, false, 0); }
        DrawLabel(String text, int x, int y, int color, boolean centered) { this(text, x, y, color, centered, 0); }
        DrawLabel(String text, int x, int y, int color, boolean centered, int lineWidth) { this.text = text; this.x = x; this.y = y; this.color = color; this.centered = centered; this.lineWidth = lineWidth; }
    }

    private record StyledLine(String text, int color) {
    }

    @FunctionalInterface private interface ToggleSetter { void set(boolean value); }
    @FunctionalInterface private interface IntSetter { void set(int value); }
    @FunctionalInterface private interface DoubleSetter { void set(double value); }

    private class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleConsumer setter;
        private final DoubleFunction<String> formatter;

        private ValueSlider(String label, int x, int y, int width, double initialValue, double min, double max, double step, DoubleConsumer setter, DoubleFunction<String> formatter) {
            super(x, y, width, BUTTON_HEIGHT, Text.empty(), 0.0);
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
            this.formatter = formatter;
            setActualValue(initialValue);
        }

        @Override protected void updateMessage() { setMessage(Text.literal(label + "        " + formatter.apply(getActualValue()))); }
        @Override protected void applyValue() { setter.accept(getActualValue()); if (!initializing) changed(); }
        private void setActualValue(double actualValue) { this.value = normalize(actualValue); applyValue(); updateMessage(); }
        private double getActualValue() { return snap(min + this.value * (max - min)); }
        private double normalize(double actualValue) { return max <= min ? 0.0 : (snap(actualValue) - min) / (max - min); }
        private double snap(double actualValue) {
            double clamped = Math.max(min, Math.min(max, actualValue));
            double snapped = Math.round((clamped - min) / step) * step + min;
            return Math.max(min, Math.min(max, snapped));
        }
    }
}
