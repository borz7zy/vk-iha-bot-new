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
            TextView apiLabel = view.findViewById(R.id.item_account_textView_api_counter);
            View menuButton = view.findViewById(R.id.item_account_button_menu);
            nameLabel.setText(tgAccount.toString());
            statusLabel.setText(tgAccount.getState());
            apiLabel.setText(String.valueOf(tgAccount.getApiCounter()));
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
