package me.bambus.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.databinding.WidgetChannelsListItemChannelVoiceBinding;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemVoiceChannel;
import com.discord.widgets.voice.settings.WidgetVoiceChannelSettings;
import com.discord.widgets.channels.list.items.ChannelListItemVoiceUser;
import com.discord.models.user.CoreUser;
import com.discord.models.user.User;
import com.lytefast.flexinput.R;

import java.util.HashMap;
import java.util.ArrayList;

@SuppressWarnings("unused")
@AliucordPlugin
public class VoiceUsersCounter extends Plugin {
    public Logger logger = new Logger("VoiceUsersCounter");

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        var itemClass = WidgetChannelsListAdapter.ItemChannelVoice.class.getDeclaredField("binding");
        itemClass.setAccessible(true);
        HashMap<Long, ArrayList<User>> channels = new HashMap<Long, ArrayList<User>>();

        patcher.patch(WidgetChannelsListAdapter.ItemVoiceUser.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class), new Hook(callFrame -> {
            var voiceUser = (ChannelListItemVoiceUser) callFrame.args[1];
            var channelId = voiceUser.component1().h();
            var users = channels.get(channelId);
            if (users == null) users = new ArrayList<User>();
            users.add(voiceUser.getUser());
            channels.put(voiceUser.component1().h(), users);
        }));

        patcher.patch(WidgetChannelsListAdapter.ItemChannelVoice.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class), new Hook(callFrame -> {
            var channel = (ChannelListItemVoiceChannel) callFrame.args[1];
            try {
                var binding = (WidgetChannelsListItemChannelVoiceBinding) itemClass.get(callFrame.thisObject);
                binding.a.setOnLongClickListener(view -> {
                    var channelId = channel.component1().h();
                    var users = channels.get(channelId);
                    var length = users == null ? 0 : users.size();
                    Toast.makeText(context, "Users in channel: " + length, Toast.LENGTH_SHORT).show();
                    return true;
                });
            } catch (Exception e) {
                logger.error(e);
            }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}