package com.atomics.client.compat;

import com.atomics.client.gui.AtomicsClientScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class AtomicsModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AtomicsClientScreen::new;
    }
}
