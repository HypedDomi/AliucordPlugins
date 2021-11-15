package com.aliucord.plugins;

import android.content.Context;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.PluginManager;
import com.aliucord.api.CommandsAPI;
import com.aliucord.Utils;
import com.discord.api.commands.ApplicationCommandType;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class EditMessages extends Plugin {
    @Override
    public void start(Context context) {
        commands.registerCommand("editmessages", "Displays information about EditMessages",
        Utils.createCommandOption(
            ApplicationCommandType.BOOLEAN,
            "send", null, null,
            true, false
        ), ctx -> {
            var output = "";
            var plugin = PluginManager.plugins.get("EditMessages");
            if (plugin != null) {
                var version = plugin.getManifest().version;
                var author = plugin.getManifest().authors;
                var description = plugin.getManifest().description;
                output = "**EditMessages Info**\n> Version: " + version + "\n> Author: " + author + "\n> Description: " + description;
            } else {
                output = "**EditMessages Info**\n> Version: Unknown\n> Author: Unknown\n> Description: Unknown";
            }
            return new CommandsAPI.CommandResult(output, null, ctx.getRequiredBool("send"));
        });
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }
}
