/*
 * This file is part of FlightControl, which is licensed under the MIT License.
 * Copyright (c) 2023 Spazzinq
 */

package org.spazzinq.flightcontrol.manager;

import com.google.common.io.Files;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spazzinq.flightcontrol.object.CommentConf;
import org.spazzinq.flightcontrol.object.StorageManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LangManager extends StorageManager {
    private final HashSet<String> languages = new HashSet<>(Arrays.asList("en", "fr", "zh"));
    private Locale locale;

    @Getter private CommentConf lang;
    private final File langFile;

    // Const
    public static final String HELP_HEADER = """
            \s
            &a&lFlightControl &f
            &aBy &fSpazzinq
            \s
            &a&lQUERY&a &7» &f...
            \s
            """;

    // Bool
    // TODO Fix caps
    @Setter private boolean useActionBar;
    private boolean stickyActionBar;

    // Player messages
    @Getter private String tempflyActionbar;
    @Getter private String disableFlight;
    @Getter private String enableFlight;
    @Getter private String canEnableFlight;
    @Getter private String cannotEnableFlight;
    @Getter private String heightDenied;
    @Getter private String personalTrailDisable;
    @Getter private String personalTrailEnable;
    @Getter private String permDenied;

    // Admin messages
    @Getter private String prefix;
    @Getter private String pluginReloaded;
    // Config editing messages
    @Getter private String globalFlightSpeedSet;
    @Getter private String globalFlightSpeedSame;
    // TODO Set usage messages
    @Getter private String globalFlightSpeedUsage = "";
    @Getter private String enemyRangeSet;
    @Getter private String enemyRangeSame;
    @Getter private String enemyRangeUsage = "";
    // Command messages
    @Getter private String flyCommandEnable;
    @Getter private String flyCommandDisable;
    @Getter private String flyCommandUsage = "";
    @Getter private String flySpeedSet;
    @Getter private String flySpeedSame;
    @Getter private String flySpeedUsage = "";
    // TODO Fix caps
    @Getter private String tempFlySet;
    @Getter private String tempFlyDisable;
    @Getter private String tempFlyDisabled;
    @Getter private String tempFlyCheck;
    @Getter private String tempFlyUsage = "";
    @Getter private String blockBreakDisable;


    public LangManager() {
        super("lang.yml");

        locale = Locale.getDefault();
        langFile = confFile;
    }

    @Override protected void initializeConf() {
        if (langFile.exists()) {
            YamlConfiguration tempLocaleConf = YamlConfiguration.loadConfiguration(langFile);

            if (tempLocaleConf.isString("locale")) {
                String preferredLocale = tempLocaleConf.getString("locale");

                if (languages.contains(preferredLocale)) {
                    locale = Locale.forLanguageTag(preferredLocale);

                    try {
                        //noinspection UnstableApiUsage
                        Files.move(langFile, new File(pl.getDataFolder(), "lang_old.yml"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    pl.getLogger().info("Generated a new lang.yml!");
                }
            }
        }

        InputStream langResource = pl.getResource("lang_" + locale.getLanguage() + ".yml");
        boolean langResourceExists = langResource != null;

        if (!langResourceExists) {
            pl.getLogger().warning("No custom lang file for " + locale.getDisplayLanguage() + " could be found! " +
                    "Defaulting to English...");
        }

        conf = lang = new CommentConf(langFile, langResourceExists ? langResource : pl.getResource("lang_en.yml"));
    }

    @Override protected void initializeValues() {
            // boolean
            useActionBar = lang.getBoolean("player.actionbar.enabled");
            stickyActionBar = lang.getBoolean("player.actionbar.sticky");

            // String
            /* Player */
            tempflyActionbar = lang.getString("player.flight.tempfly_actionbar");
            disableFlight = lang.getString("player.flight.disabled");
            enableFlight = lang.getString("player.flight.enabled");
            canEnableFlight = lang.getString("player.flight.can_enable");
            cannotEnableFlight = lang.getString("player.flight.cannot_enable");
            heightDenied = lang.getString("player.flight.height_denied");
            personalTrailDisable = lang.getString("player.trail.disabled");
            personalTrailEnable = lang.getString("player.trail.enabled");
            permDenied = lang.getString("player.permission_denied");
            /* Admin */
            prefix = lang.getString("admin.prefix");
            pluginReloaded = lang.getString("admin.reloaded");
            globalFlightSpeedSet = lang.getString("admin.global_flight_speed.set");
            globalFlightSpeedSame = lang.getString("admin.global_flight_speed.same");
            enemyRangeSet = lang.getString("admin.enemy_range.set");
            enemyRangeSame = lang.getString("admin.enemy_range.same");
            flyCommandEnable = lang.getString("admin.fly.enable");
            flyCommandDisable = lang.getString("admin.fly.disable");
            flySpeedSet = lang.getString("admin.flyspeed.set");
            flySpeedSame = lang.getString("admin.flyspeed.same");
            tempFlySet = lang.getString("admin.tempfly.set");
            tempFlyDisable = lang.getString("admin.tempfly.disable");
            tempFlyDisabled = lang.getString("admin.tempfly.disabled");
            tempFlyCheck = lang.getString("admin.tempfly.check");
            blockBreakDisable = lang.getString("player.flight.block_break_disable");
    }

    @Override protected void updateFormatting() {
        boolean modified = false;

        if (!lang.isString("admin.tempfly.check")) {
            // TODO Add depending on locale
            lang.addSubnodes(Collections.singleton("check: '&e&lFlightControl &7» &f%player%&e has &f%duration%&e of" +
                    " flight remaining.'"), "admin.tempfly.disabled");

            modified = true;
        }

        // 4.6.10
        if (lang.isString("admin.tempfly.add")) {
            lang.deleteNode("admin.tempfly.add");

            modified = true;
        }

        // 4.6.10
        if (lang.isString("admin.tempfly.enable")) {
            lang.addIndentedSubnodes(Collections.singleton("set: '" + lang.getString("admin.tempfly.enable") + "'"), "admin.tempfly");
            lang.deleteNode("admin.tempfly.enable");

            modified = true;
        }

        // 4.7.0
        if (!lang.isString("player.flight.tempfly_actionbar")) {
            // TODO Add depending on locale
            lang.addIndentedSubnodes(Collections.singleton("tempfly_actionbar: '&7You have &a%duration% &7of flight remaining.'"), "player.flight");

            modified = true;
        }

        // 4.7.5
        if (lang.isString("admin.global_flight_speed.usage")) {
            lang.deleteNode("admin.global_flight_speed.usage");
            lang.deleteNode("admin.enemy_range.usage");
            lang.deleteNode("admin.fly.usage");
            lang.deleteNode("admin.flyspeed.usage");
            lang.deleteNode("admin.tempfly.usage");

            modified = true;
        }

        // 4.10.5
        if (lang.isBoolean("player.actionbar")) {
            lang.deleteNode("player.actionbar");
            lang.addIndentedSubnodes(Collections.singleton("actionbar:"), "player");
            lang.addIndentedSubnodes(Set.of("enabled: false", "sticky: false"), "player.actionbar");

            modified = true;
        }

        if (modified) {
            lang.save();
        }
    }

    // Version 4
    @Override protected void migrateFromOldVersion() {
        if (pl.getConfManager().getConf().isConfigurationSection("messages")) {
            CommentConf conf = pl.getConfManager().getConf();
            ConfigurationSection msgs = conf.getConfigurationSection("messages");

            lang.set("player.actionbar", msgs.getBoolean("actionbar"));

            lang.set("player.flight.enabled", msgs.getString("flight.enable"));
            lang.set("player.flight.disabled", msgs.getString("flight.disable"));
            lang.set("player.flight.can_enable", msgs.getString("flight.can_enable"));
            lang.set("player.flight.cannot_enable", msgs.getString("flight.cannot_enable"));

            lang.set("player.trail.enabled", msgs.getString("trail.enable"));
            lang.set("player.trail.disabled", msgs.getString("trail.disable"));

            lang.set("player.permission_denied", msgs.getString("permission_denied"));

            lang.save();

            conf.deleteNode("messages");
            conf.save();

            pl.getLogger().info("Successfully migrated the messages from config.yml to lang.yml!");
        }
    }

    public boolean useActionBar() {
        return useActionBar;
    }
    public boolean isActionBarSticky() {
        return stickyActionBar;
    }
}
