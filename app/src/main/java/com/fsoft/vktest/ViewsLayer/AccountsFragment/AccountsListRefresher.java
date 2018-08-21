package com.fsoft.vktest.ViewsLayer.AccountsFragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.ViewsLayer.MainActivity;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class AccountsListRefresher {
    private ApplicationManager applicationManager = null;
    private MainActivity activity = null;
    private LayoutInflater layoutInflater = null;
    private LinearLayout linearLayout = null;

    public AccountsListRefresher(MainActivity activity, ApplicationManager applicationManager, LinearLayout linearLayout) {
        this.applicationManager = applicationManager;
        this.activity = activity;
        this.linearLayout = linearLayout;
        if(applicationManager == null || activity == null || linearLayout == null)
            return;
        layoutInflater = LayoutInflater.from(activity);
    }

    public void fill() {
        if(applicationManager == null || activity == null || linearLayout == null)
            return;
        linearLayout.removeAllViews();
        //telegram
        ArrayList<TgAccount> tgAccounts = applicationManager.getCommunicator().getTgAccounts();
        for(final TgAccount tgAccount:tgAccounts){
            View view = layoutInflater.inflate(R.layout.item_account_tg, null, false);
            view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView nameLabel = view.findViewById(R.id.item_account_textView_name);
            TextView statusLabel = view.findViewById(R.id.item_account_textView_status);
            TextView messagesReceivedLabel = view.findViewById(R.id.item_account_textView_messages_received);
            TextView messagesSentLabel = view.findViewById(R.id.item_account_textView_messages_sent);
            TextView apiLabel = view.findViewById(R.id.item_account_textView_api_counter);
            TextView apiErrorsLabel = view.findViewById(R.id.item_account_textView_api_errors);
            TextView replyInstructionLabel = view.findViewById(R.id.item_account_textView_active_instruction);
            TextView chatsEnabledLabel = view.findViewById(R.id.item_account_textView_active_chats);
            TextView statusEnabledLabel = view.findViewById(R.id.item_account_textView_status);
            View menuButton = view.findViewById(R.id.item_account_button_menu);

            nameLabel.setText(tgAccount.toString());
            statusLabel.setText(tgAccount.getState());
            messagesReceivedLabel.setText(String.valueOf(tgAccount.getMessageProcessor().getMessagesReceivedCounter()));
            messagesSentLabel.setText(String.valueOf(tgAccount.getMessageProcessor().getMessagesSentCounter()));
            apiLabel.setText(String.valueOf(tgAccount.getApiCounter()));
            apiErrorsLabel.setText(String.valueOf(tgAccount.getErrorCounter()));
            replyInstructionLabel.setText("Выключено");
            chatsEnabledLabel.setText("Включено");
            statusEnabledLabel.setText("Выключено");
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAccountMenu(tgAccount, v);
                }
            });

            linearLayout.addView(view);
        }
    }
    void showAccountMenu(final TgAccount tgAccount, View v){
        PopupMenu popupMenu = new PopupMenu(activity, v);
        popupMenu.getMenu().add("Удалить аккаунт").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                applicationManager.getCommunicator().remTgAccount(tgAccount);
                fill();
                return false;
            }
        });
        popupMenu.show();
    }
}
