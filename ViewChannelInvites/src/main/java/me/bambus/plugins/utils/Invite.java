package me.bambus.plugins.utils;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.Http;
import com.discord.api.channel.Channel;
import com.discord.models.user.CoreUser;

import java.util.ArrayList;
import org.json.JSONArray;

public class Invite {
    public static Logger logger = new Logger("ViewChannelInvites - Invite");

    public String code, maxUses, temporary, createdAt, uses, inviter;

    public Invite(String code, String maxUses, String temporary, String createdAt, String uses, String inviter) {
        this.code = code;
        this.maxUses = maxUses;
        this.temporary = temporary;
        this.createdAt = createdAt;
        this.uses = uses;
        this.inviter = inviter;
    }

    public static ArrayList<Invite> getInvites(Channel channel) {
        ArrayList<Invite> invites = new ArrayList<>();
        Utils.threadPool.execute(() -> {
            try {
                JSONArray response = new JSONArray(
                    Http.Request.newDiscordRequest("/channels/" + channel.h() + "/invites")
                        .execute()
                        .text()
                );

                for (int i = 0; i < response.length(); i++) {
                    String code = response.getJSONObject(i).getString("code");
                    String maxUses = response.getJSONObject(i).getString("max_uses");
                    String temporary = response.getJSONObject(i).getString("temporary");
                    String createdAt = response.getJSONObject(i).getString("created_at");
                    String uses = response.getJSONObject(i).getString("uses");
                    String inviter = response.getJSONObject(i).getJSONObject("inviter").getString("username");

                    invites.add(new Invite(code, maxUses, temporary, createdAt, uses, inviter));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        });
        int trys = 0;
        while (invites.size() == 0 && trys < 5) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e);
            }
            trys++;
        }
        return invites;
    }
}
