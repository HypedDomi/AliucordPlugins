package me.bambus.plugins;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.Divider;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;
import com.lytefast.flexinput.R;

import java.util.Arrays;

// Credits to Juby210
// https://github.com/Juby210/Aliucord-plugins/blob/d84f4baf04912e870e77c6a14ee04ddeed8537f8/PronounDB/src/main/java/io/github/juby210/acplugins/pronoundb/PluginSettings.java

@SuppressLint("SetTextI18n")
public final class PluginSettings extends SettingsPage {
    private static final String pluginName = "BetterReplies";
    private final SettingsAPI settings;
    public BetterReplies plugin;

    public PluginSettings(BetterReplies plugin) {
        this.plugin = plugin;
        this.settings = plugin.settings;
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        setActionBarTitle(pluginName);
        setPadding(0);

        var context = view.getContext();
        var layout = getLinearLayout();

        var appearance = new TextView(context, null, 0, R.i.UiKit_Settings_Item_Header);
        appearance.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        appearance.setText("Replies appearance");
        layout.addView(appearance);

        var replies = Arrays.asList(
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Default",
                        "Don't change the appearance of the Reply"),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Suppress Mentions",
                        "Suppress the reply mention"),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Force Mentions",
                        "Force the reply mention"));

        var radioManager = new RadioManager(replies);
        int format = settings.getInt("replySettings", 0);

        int radios = replies.size();
        for (int i = 0; i < radios; i++) {
            int index = i;
            var radio = replies.get(index);
            radio.e(e -> {
                settings.setInt("replySettings", index);
                radioManager.a(radio);
            });
            layout.addView(radio);
            if (i == format)
                radioManager.a(radio);
        }

        var replyMention = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Disable Mention",
                "Automatically disables the 'Mention' option when replying to someone else");
        replyMention.setChecked(settings.getBool("disableReplyMention", false));
        replyMention.setOnCheckedListener(value -> {
            settings.setBool("disableReplyMention", value);
            reloadPlugin();
        });
        layout.addView(new Divider(context));
        layout.addView(replyMention);
    }

    public void reloadPlugin() {
        PluginManager.stopPlugin(pluginName);
        PluginManager.startPlugin(pluginName);
    }
}