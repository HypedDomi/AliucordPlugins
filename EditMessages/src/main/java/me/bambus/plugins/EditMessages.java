package me.bambus.plugins;

import android.content.Context;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.EditText;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.graphics.Color;
import androidx.core.widget.NestedScrollView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.aliucord.Constants;
import android.text.SpannableStringBuilder;
import com.lytefast.flexinput.R;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.Logger;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.MDUtils;
import com.aliucord.PluginManager;
import com.aliucord.api.CommandsAPI;
import com.discord.api.commands.ApplicationCommandType;
import com.aliucord.patcher.Hook;
import com.aliucord.CollectionUtils;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.utilities.color.ColorCompat;
import com.discord.stores.StoreStream;
import com.discord.models.domain.ModelMessageDelete;
import com.discord.models.message.Message;
import com.discord.models.user.CoreUser;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class EditMessages extends Plugin {
    public Logger logger = new Logger("EditMessages");

    class EditPage extends SettingsPage {
        private Message message;
        public EditPage(Message message) {
            this.message = message;
        }
    
        @Override
        public void onViewBound(View view) {
            super.onViewBound(view);
            setActionBarTitle("EditMessage");
            setActionBarSubtitle(new CoreUser(message.getAuthor()).getUsername());
            
            var ctx = view.getContext();
            LinearLayout layout = getLinearLayout();
            LinearLayout original = new LinearLayout(ctx);
            LinearLayout buttons = new LinearLayout(ctx);
            LinearLayout newMSG_Header = new LinearLayout(ctx);
            LinearLayout newMSG = new LinearLayout(ctx);
            
            var content = message.getContent();

            if (!content.isEmpty()) {
                TextView originalHeader = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header);
                originalHeader.setText("Original message");
                originalHeader.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
                originalHeader.setPadding(0, DimenUtils.dpToPx(12), 0, 0);
                layout.addView(originalHeader);
                
                TextView originalMessage = new TextView(ctx);
                originalMessage.setText(MDUtils.renderCodeBlock(ctx, new SpannableStringBuilder(), null, content));
                originalMessage.setTextIsSelectable(true);
                layout.addView(originalMessage);
            }

            var copyIcon = ContextCompat.getDrawable(ctx, R.e.ic_copy_24dp).mutate();
            Button copyButton = new Button(ctx);
            LinearLayout.LayoutParams copyButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            copyButtonParams.rightMargin = DimenUtils.dpToPx(3);
            copyButtonParams.weight = 1;
            copyButton.setLayoutParams(copyButtonParams);
            copyButton.setText("Copy Content");
            copyButton.setCompoundDrawablesWithIntrinsicBounds(copyIcon, null, null, null);
            copyButton.setEnabled(!content.isEmpty());
            copyButton.setOnClickListener(v -> {
                Utils.setClipboard("Copy Content", content);
                Utils.showToast(ctx, "Copied content to clipboard");
                logger.info("Copied content to clipboard");
            });
            
            var deleteIcon = ContextCompat.getDrawable(ctx, R.e.ic_delete_24dp).mutate();
            DangerButton deleteButton = new DangerButton(ctx);
            LinearLayout.LayoutParams deleteButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            deleteButtonParams.leftMargin = DimenUtils.dpToPx(3);
            deleteButtonParams.weight = 1;
            deleteButton.setLayoutParams(deleteButtonParams);
            deleteButton.setText("Delete Message locally");
            deleteButton.setCompoundDrawablesWithIntrinsicBounds(deleteIcon, null, null, null);
            deleteButton.setOnClickListener(v -> {
                getActivity().onBackPressed();
                if (PluginManager.isPluginEnabled("MessageLogger")) {
                    logger.info("Due to how this plugin works, MessageLogger needs to be disabled");
                    PluginManager.disablePlugin("MessageLogger");
                    StoreStream.getMessages().handleMessageDelete(new ModelMessageDelete(message.getChannelId(), message.getId()));
                    PluginManager.enablePlugin("MessageLogger");
                } else {
                    StoreStream.getMessages().handleMessageDelete(new ModelMessageDelete(message.getChannelId(), message.getId()));
                }
                Utils.showToast(ctx, "Message deleted");
                logger.info("Message deleted");
            });

            buttons.setPadding(0, 0, 0, DimenUtils.dpToPx(12));
            buttons.addView(copyButton);
            buttons.addView(deleteButton);

            layout.addView(buttons);

            var newMessageHeader = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header);
            newMessageHeader.setText("Edit message");
            newMessageHeader.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
            newMessageHeader.setPadding(0, DimenUtils.dpToPx(12), 0, 0);
            newMSG_Header.addView(newMessageHeader);
            layout.addView(newMSG_Header);

            Button saveButton = new Button(ctx);
            EditText newMessage = new EditText(ctx);
            LinearLayout.LayoutParams newMessageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            newMessageParams.weight = 1;
            newMessage.setLayoutParams(newMessageParams);
            newMessage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            newMessage.setText(!content.isEmpty() ? content : "");
            newMessage.setTextColor(Color.WHITE);
            newMessage.addTextChangedListener(new TextWatcher(){
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    saveButton.setEnabled(!s.toString().isEmpty());
                }
            });
            newMSG.addView(newMessage);
            layout.addView(newMSG);

            LinearLayout.LayoutParams saveButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            saveButtonParams.setLayoutDirection(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            saveButton.setLayoutParams(saveButtonParams);
            saveButton.setText("Update");
            // var saveIcon = ContextCompat.getDrawable(ctx, R.e.icon_save).mutate();
            // saveButton.setCompoundDrawablesWithIntrinsicBounds(saveIcon, null, null, null);
            saveButton.setEnabled(!newMessage.getText().toString().isEmpty());
            saveButton.setOnClickListener(v -> {
                Utils.showToast(ctx, newMessage.getText().toString());
                logger.info("Updated Message");
            });
            layout.addView(saveButton);
        }
    }
    
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
        logger.info("Command registered");

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
                    Utils.openPageWithProxy(e.getContext(), new EditPage(msg));
                });

                for (int i = 0; i < layout.getChildCount(); i++) {
                    if (layout.getChildAt(i).getId() == Utils.getResId("dialog_chat_actions_delete", "id")) {
                        layout.addView(view, i+1);
                        break;
                    }
                }
            }));
        logger.info("MessageContextMenu patched");
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
        patcher.unpatchAll();
    }
}