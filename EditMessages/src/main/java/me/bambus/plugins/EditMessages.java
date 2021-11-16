package me.bambus.plugins;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.core.content.ContextCompat;
import com.lytefast.flexinput.R;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.PluginManager;
import com.aliucord.api.CommandsAPI;
import com.aliucord.Utils;
import com.discord.api.commands.ApplicationCommandType;

import com.aliucord.patcher.Hook;
import com.aliucord.CollectionUtils;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.utilities.color.ColorCompat;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class EditMessages extends Plugin {
    @Override
    public void start(Context context) {
        commands.registerCommand("editmessages", "Displays information about EditMessages",
        Utils.createCommandOption(
            ApplicationCommandType.BOOLEAN,
            "send", "Whether or not to send the message in chat (default false)"
        ), ctx -> {
            var output = "";
            var plugin = PluginManager.plugins.get("EditMessages");
            if (plugin != null) {
                var version = plugin.getManifest().version;
                var author = plugin.getManifest().authors[0];
                var description = plugin.getManifest().description;
                output = "**EditMessages Info**\n> Version: " + version + "\n> Author: " + author + "\n> Description: " + description;
            } else {
                output = "**EditMessages Info**\n> Version: Unknown\n> Author: Unknown\n> Description: Unknown";
            }
            return new CommandsAPI.CommandResult(output, null, ctx.getBoolOrDefault("send", false));
        });

        var id = View.generateViewId();
        patcher.patch(WidgetChatListActions.class, "configureUI", new Class<?>[]{ WidgetChatListActions.Model.class }, new Hook(
            callFrame -> {
                var _this = (WidgetChatListActions) callFrame.thisObject;
                var rootView = (NestedScrollView) _this.getView();
                if(rootView == null) return;
                var layout = (LinearLayout) rootView.getChildAt(0);
                if (layout == null || layout.findViewById(id) != null) return;
                var ctx = layout.getContext();
                var msg = ((WidgetChatListActions.Model) callFrame.args[0]).getMessage();
                var view = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon);
                view.setId(id);
                view.setText("Edit message locally");
                var editDrawable = ContextCompat.getDrawable(ctx, R.e.ic_edit_24dp).mutate();
                editDrawable.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(editDrawable, null, null, null);
                view.setOnClickListener(e -> {
                    Utils.showToast("Clicked Button", false);
                    _this.dismiss();
                });
                layout.addView(view, 9);
            }));
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
        patcher.unpatchAll();
    }
}
