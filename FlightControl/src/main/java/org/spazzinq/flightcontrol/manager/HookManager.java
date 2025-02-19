/*
 * This file is part of FlightControl, which is licensed under the MIT License.
 * Copyright (c) 2023 Spazzinq
 */

package org.spazzinq.flightcontrol.manager;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.spazzinq.flightcontrol.FlightControl;
import org.spazzinq.flightcontrol.multiversion.FactionsHookBase;
import org.spazzinq.flightcontrol.multiversion.WorldGuardHookBase;
import org.spazzinq.flightcontrol.multiversion.current.FactionsUUIDHook;
import org.spazzinq.flightcontrol.multiversion.current.FactionsXHook;
import org.spazzinq.flightcontrol.multiversion.current.MassiveFactionsHook;
import org.spazzinq.flightcontrol.multiversion.current.WorldGuardHook7;
import org.spazzinq.flightcontrol.multiversion.legacy.LegacyFactionsUUIDHook;
import org.spazzinq.flightcontrol.multiversion.legacy.WorldGuardHook6;
import org.spazzinq.flightcontrol.placeholder.ClipPlaceholder;
import org.spazzinq.flightcontrol.placeholder.MVdWPlaceholder;

import java.util.ArrayList;

public class HookManager {
    private final FlightControl pl;
    private final PluginManager pm;
    private final boolean is1_13;

    @Getter private String hookedMsg;
    private final ArrayList<String> hooked = new ArrayList<>();

    // Load early to prevent NPEs
    @Getter private WorldGuardHookBase worldGuardHook = new WorldGuardHookBase();
    @Getter private FactionsHookBase factionsHook = new FactionsHookBase();

    public HookManager(boolean is1_13) {
        pl = FlightControl.getInstance();
        this.is1_13 = is1_13;
        pm = pl.getServer().getPluginManager();
    }

    public void loadHooks() {
        loadFactionsHook();
        loadPlaceholderHooks();

        if (pluginLoading("WorldGuard")) {
            worldGuardHook = is1_13 ? new WorldGuardHook7() : new WorldGuardHook6();
        }

        printLoadedHooks();
    }

    private void loadFactionsHook() {
        if (pluginLoading("FactionsX")) {
            factionsHook = new FactionsXHook();
        } else if (pluginLoading("Factions")) {
            String website = pm.getPlugin("Factions").getDescription().getWebsite();

            if (website != null && website.equals("https://www.massivecraft.com/factions")) {
                factionsHook = new MassiveFactionsHook();
            } else if (pm.getPlugin("Factions").getDescription().getVersion().startsWith("1.6.9.5-U0.4")
                    || pm.getPlugin("Factions").getDescription().getAuthors().contains("ProSavage")) {
                factionsHook = new LegacyFactionsUUIDHook();
            } else {
                factionsHook = new FactionsUUIDHook();
            }
        }
    }

    private void loadPlaceholderHooks() {
        if (pluginLoading("PlaceholderAPI")) {
            new ClipPlaceholder(pl).register();
        }
        if (pluginLoading("MVdWPlaceholderAPI")) {
            new MVdWPlaceholder(pl);
        }
    }

    private void printLoadedHooks() {
        hookedMsg = hooked.isEmpty() ? "Hooked with no plugins." : "Loaded hooks: " + hooked;

        pl.getLogger().info(hookedMsg);
    }

    private boolean pluginLoading(String pluginName) {
        // Don't use .isPluginEnabled()--plugin might not yet be ready
        boolean enabled = pm.getPlugin(pluginName) != null;

        if (enabled) {
            hooked.add(pluginName);
        }

        return enabled;
    }
}
