package me.bambus.plugins;

import android.annotation.SuppressLint;
import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.ReflectUtils;
import com.discord.api.channel.Channel;
import com.discord.api.message.MessageReference;
import com.discord.api.user.User;
import com.discord.models.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.models.user.MeUser;
import com.discord.stores.StoreStream;
import com.discord.utilities.view.text.SimpleDraweeSpanTextView;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.util.List;

@SuppressWarnings("unused")
@AliucordPlugin
public class BetterReplies extends Plugin {
    public Logger logger = new Logger("BetterReplies");

    public BetterReplies() {
        settingsTab = new SettingsTab(PluginSettings.class).withArgs(settings);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {
        try {
            patcher.patch(WidgetChatListAdapterItemMessage.class, "processMessageText", new Class<?>[]{SimpleDraweeSpanTextView.class, MessageEntry.class}, new Hook(callFrame -> {
                int replySettings = settings.getInt("replySettings", 0);
                if (replySettings == 0) return;

                Message msg = ((MessageEntry) callFrame.args[1]).getMessage();
                MessageReference reference = msg.component22();
                if (reference == null) return;
                Message reply = StoreStream.getMessages().getMessage(reference.a(), reference.c());

                MeUser currentUser = StoreStream.getUsers().getMe();
                User me = new User(currentUser.getId(), currentUser.getUsername(), null, null, String.valueOf(currentUser.getDiscriminator()), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);
                CoreUser author = new CoreUser(reply.getAuthor());

                if (author.getId() != new CoreUser(me).getId()) return;

                // Credits to wingio
                // https://github.com/wingio/plugins/blob/0470dc18df012690431c2671cee3c845d0ef93e3/KeywordAlerts/src/main/java/xyz/wingio/plugins/KeywordAlerts.java#L120-L125

                try {
                    List<User> mentions = (List<User>) ReflectUtils.getField(msg, "mentions");
                    logger.debug("mentions: " + mentions);
                    if (replySettings == 1 && mentions.contains(me)) {
                        mentions.remove(me);
                    } else if (replySettings == 2 && !mentions.contains(me)) {
                        mentions.add(me);
                    }
                    ReflectUtils.setField(msg, "mentions", mentions);
                    StoreStream.getMessages().handleMessageUpdate(msg.synthesizeApiMessage());
                } catch (Exception e) {
                    logger.error(e);
                }
            }));
        } catch (Exception e) {
            logger.error(e);
        }

        if (!settings.getBool("disableReplyMention", true)) return;
        // Code from Juby210
        // https://github.com/Juby210/Aliucord-plugins/blob/d84f4baf04912e870e77c6a14ee04ddeed8537f8/NoAutoReplyMention/src/main/java/io/github/juby210/acplugins/NoAutoReplyMention.java
        patcher.patch(
                "com.discord.stores.StorePendingReplies", "onCreatePendingReply",
                new Class<?>[]{Channel.class, Message.class, boolean.class, boolean.class},
                new PreHook(param -> {
                    param.args[2] = false; // mention
                    param.args[3] = true;  // showMentionToggle
                })
        );
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}