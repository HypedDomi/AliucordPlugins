package me.bambus.plugins;

import me.bambus.plugins.utils.Invite;

import android.view.View;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.discord.api.channel.Channel;

import java.util.ArrayList;

public class InvitePage extends SettingsPage {
    public Logger logger = new Logger("ViewChannelInvites - InvitePage");
    private ArrayList<Invite> invites;
    private Channel channel;
    public InvitePage(ArrayList<Invite> invites, Channel channel) {
        this.invites = invites;
        this.channel = channel;
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        setActionBarTitle("Invites for " + channel.m());
        setActionBarSubtitle(invites.size() + " invites");

        for (Invite invite : invites) {
            logger.debug(invite.code + " " + invite.maxUses + " " + invite.temporary + " " + invite.createdAt + " " + invite.uses + " " + invite.inviter);
        }
    }

}
