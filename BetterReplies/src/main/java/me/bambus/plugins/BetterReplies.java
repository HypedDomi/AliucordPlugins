package me.bambus.plugins;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.NotificationsAPI;
import com.aliucord.entities.NotificationData;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.MDUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.ChannelWrapper;
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
import com.discord.widgets.notice.NoticePopup;

import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
@AliucordPlugin
public class BetterReplies extends Plugin {
    public Logger logger = new Logger("BetterReplies");

    public BetterReplies() {
        settingsTab = new SettingsTab(PluginSettings.class).withArgs(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {
        try {
            // Credits to wingio
            // https://github.com/wingio/plugins/blob/main/KeywordAlerts/src/main/java/xyz/wingio/plugins/KeywordAlerts.java
            RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
                int replySettings = settings.getInt("replySettings", 0);
                if (replySettings == 0) return;
                if (message == null) return;
                Message msg = new Message(message);
                MessageReference reference = msg.component22();
                if (reference == null) return;
                Message reply = StoreStream.getMessages().getMessage(reference.a(), reference.c());

                MeUser currentUser = StoreStream.getUsers().getMe();
                CoreUser author = new CoreUser(reply.getAuthor());
                if (msg.getChannelId() == StoreStream.getChannelsSelected().getId() || author.getId() != currentUser.getId())
                    return;
                showNotification(msg);
            }));

            patcher.patch(WidgetChatListAdapterItemMessage.class, "processMessageText", new Class<?>[]{SimpleDraweeSpanTextView.class, MessageEntry.class}, new PreHook(callFrame -> {
                int replySettings = settings.getInt("replySettings", 0);
                if (replySettings == 0) return;

                Message msg = ((MessageEntry) callFrame.args[1]).getMessage();
                MessageReference reference = msg.component22();
                if (reference == null) return;
                Message reply = StoreStream.getMessages().getMessage(reference.a(), reference.c());

                MeUser currentUser = StoreStream.getUsers().getMe();
                User me = new User(currentUser.getId(), currentUser.getUsername(), null, null, String.valueOf(currentUser.getDiscriminator()), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);
                CoreUser _me = new CoreUser(me);
                CoreUser author = new CoreUser(reply.getAuthor());
                if (author.getId() != _me.getId()) return;

                try {
                    List<User> mentions = (List<User>) ReflectUtils.getField(msg, "mentions");
                    if (replySettings == 1) {
                        for (User user : mentions) {
                            if (new CoreUser(user).getId() == _me.getId()) {
                                mentions.remove(user);
                                patcher.patch(NoticePopup.class.getDeclaredMethod("getAutoDismissAnimator", Integer.class, Function0.class), new PreHook(callFrame2 -> {
                                    ValueAnimator result = (ValueAnimator) callFrame2.getResult();
                                    result.cancel();
                                }));
                                break;
                            }
                        }
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

    // Code from wingio
    // https://github.com/wingio/plugins/blob/0470dc18df012690431c2671cee3c845d0ef93e3/KeywordAlerts/src/main/java/xyz/wingio/plugins/KeywordAlerts.java#L147-L179
    private void showNotification(Message message) {
        CoreUser author = new CoreUser(message.getAuthor());
        String location = "";
        String icon = "";
        boolean isDm = false;
        ChannelWrapper channel = new ChannelWrapper(StoreStream.getChannels().getChannel(message.getChannelId()));
        if (channel.raw() != null) {
            var guild = StoreStream.getGuilds().getGuild(channel.getGuildId());
            if (channel.isGuild() && guild != null) {
                location += guild.getName();
                icon = String.format("https://cdn.discordapp.com/icons/%s/%s.png", guild.getId(), guild.getIcon());
            } else {
                CoreUser recipient = channel.raw().w().get(0) == null ? author : new CoreUser(channel.raw().w().get(0));
                icon = String.format("https://cdn.discordapp.com/avatars/%s/%s.png", recipient.getId(), recipient.getAvatar());
            }
            location += channel.isDM() ? /* Recipient */ new CoreUser(channel.raw().w().get(0)).getUsername() : " #" + channel.getName();
            isDm = channel.isDM();
        }
        NotificationData notD = new NotificationData();
        notD.setTitle(location);
        notD.setBody(MDUtils.render(isDm ? message.getContent() : "**" + author.getUsername() + ": ** " + message.getContent()));
        notD.setAutoDismissPeriodSecs(5);
        notD.setIconUrl(icon);
        notD.setOnClick(v -> {
            StoreStream.Companion.getMessagesLoader().jumpToMessage(message.getChannelId(), message.getId());
            return Unit.a;
        });
        notD.setOnClickTopRightIcon(v -> {
            Utils.openPageWithProxy(v.getContext(), new PluginSettings(this));
            return Unit.a;
        });
        NotificationsAPI.display(notD);
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}