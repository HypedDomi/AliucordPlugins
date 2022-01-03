package me.bambus.plugins;

import me.bambus.plugins.utils.Invite;
import me.bambus.plugins.InvitePage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.content.ContextCompat;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.Constants;
import com.discord.widgets.channels.settings.WidgetTextChannelSettings;
import com.discord.databinding.WidgetTextChannelSettingsBinding;
import com.discord.utilities.color.ColorCompat;
import com.lytefast.flexinput.R;

import java.util.ArrayList;

@SuppressWarnings("unused")
@AliucordPlugin
public class ViewChannelInvites extends Plugin {
    public Logger logger = new Logger("ViewChannelInvites");

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        var channelInvitesId = View.generateViewId();
        var getBinding = WidgetTextChannelSettings.class.getDeclaredMethod("getBinding");
        getBinding.setAccessible(true);

        patcher.patch(WidgetTextChannelSettings.class.getDeclaredMethod("configureUI", WidgetTextChannelSettings.Model.class), new Hook(callFrame -> {
            try {
                var channel = ((WidgetTextChannelSettings.Model) callFrame.args[0]).getChannel();
                
                var binding = (WidgetTextChannelSettingsBinding) getBinding.invoke(callFrame.thisObject);
                binding.a.findViewById(channelInvitesId).setVisibility(View.VISIBLE);
                binding.a.findViewById(channelInvitesId).setOnClickListener(v -> {
                    Utils.showToast("Getting Invites for " + channel.m());
                    ArrayList<Invite> invites = Invite.getInvites(channel);
                    if (invites.size() == 0) {
                        Utils.showToast("No Invites found for " + channel.m());
                        return;
                    }
                    Utils.openPageWithProxy(v.getContext(), new InvitePage(invites, channel));
                });
                binding.a.findViewById(channelInvitesId).setClickable(true);
            } catch (Throwable e) {
                logger.error(e);
            }
        }));

        patcher.patch(WidgetTextChannelSettings.class.getDeclaredMethod("onViewBound", View.class), new Hook(callFrame -> {
            var coordinatorLayout = (CoordinatorLayout) callFrame.args[0];
            var nestedScrollView = (NestedScrollView) coordinatorLayout.getChildAt(1);
            var linearLayout = (LinearLayout) nestedScrollView.findViewById(Utils.getResId("channel_settings_section_privacy_safety", "id"));
            var ctx = linearLayout.getContext();

            var iconLeft = ContextCompat.getDrawable(context, R.e.ic_link_white_24dp);
            iconLeft.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));
            var iconRight = ContextCompat.getDrawable(context, R.e.icon_carrot);
            iconRight.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));

            var channelInvites = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon);
            channelInvites.setId(channelInvitesId);
            channelInvites.setText("Channel Invites");
            channelInvites.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, iconRight, null);
            channelInvites.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium));

            linearLayout.addView(channelInvites);
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}