package me.bambus.plugins;

import android.annotation.SuppressLint;
import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PreHook;
import com.discord.widgets.chat.MessageContent;
import com.discord.widgets.chat.MessageManager;
import com.discord.widgets.chat.input.ChatInputViewModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.jvm.functions.Function1;

@SuppressWarnings("unused")
@AliucordPlugin
public class ReplaceTimestamps extends Plugin {
    public Logger logger = new Logger("ReplaceTimestamps");

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        try {
            patcher.patch(ChatInputViewModel.class.getDeclaredMethod("sendMessage", Context.class, MessageManager.class,
                    MessageContent.class, List.class, boolean.class, Function1.class), new PreHook(callFrame -> {
                MessageContent content = (MessageContent) callFrame.args[2];
                String text = content.getTextContent();

                final Pattern pattern = Pattern.compile("(?<!\\d)\\d{1,2}:\\d{2}(?!\\d)");
                final Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    String timestamp = matcher.group();
                    text = text.replace(timestamp, getUnixTimestamp(timestamp));
                }

                callFrame.args[2] = new MessageContent(text, content.component2());
            }));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private String getUnixTimestamp(String time) {
        String[] timeParts = time.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        if (hours > 23 || minutes > 59) {
            return time;
        }
        DateFormat date = new SimpleDateFormat("yyyy-MM-dd");
        String _time = date.format(new Date()) + " " + time;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date then = null;
        try {
            then = df.parse(_time);
        } catch (Exception e) {
            logger.error(e);
            return time;
        }
        int unixTime = Integer.parseInt(String.valueOf(then.getTime()).substring(0, String.valueOf(then.getTime()).length() - 3));
        if (Double.isNaN(unixTime))
            return time;
        return "<t:" + unixTime + ":t>";
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}