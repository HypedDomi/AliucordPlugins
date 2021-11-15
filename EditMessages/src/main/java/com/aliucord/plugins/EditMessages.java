package com.aliucord.plugins;

import android.content.Context;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
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
            var info = "**EditMessages Info**\n> Version: 0.1";
            return new CommandsAPI.CommandResult(info, null, ctx.getRequiredBool("send"));
        });
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }
}
