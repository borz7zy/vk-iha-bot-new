package com.fsoft.vktest.ViewsLayer.AccountsFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Dimension;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.ViewsLayer.MainActivity;

import java.util.ArrayList;

public class AccountsFragment extends Fragment {
    private String TAG = "AccountsFragment";
    private ApplicationManager applicationManager = null;
    private MainActivity activity = null;
    private View addAccountButton = null;
    private LinearLayout accountsList = null;
    private  LayoutInflater layoutInflater = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private Handler handler = new Handler();

    public AccountsFragment() {
        applicationManager = BotService.applicationManager;
        activity = MainActivity.getInstance();
        layoutInflater = LayoutInflater.from(activity);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_accounts_list, container, false);
        addAccountButton = view.findViewById(R.id.activityAccountListButtonAdd);
        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAccount();
            }
        });
        accountsList = view.findViewById(R.id.activityAccountListLinearLayout);
        swipeRefreshLayout = view.findViewById(R.id.activityAccountListPullToRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onPause() {
        //сбросить лисененов, чтобы программа не вылетела после смены экрана
        ArrayList<TgAccount> tgAccounts = applicationManager.getCommunicator().getTgAccounts();
        for(TgAccount tgAccount:tgAccounts){
            tgAccount.setApiCounterChangedListener(null);
            tgAccount.setApiErrorsChangedListener(null);
            tgAccount.getMessageProcessor().setOnMessagesSentCounterChangedListener(null);
            tgAccount.getMessageProcessor().setOnMessagesReceivedCounterChangedListener(null);
        }
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Accounts Tab...");
        super.onAttach(context);
    }

    public void addAccount(){
        TgAccount tgAccount = new TgAccount(applicationManager, "tg"+System.currentTimeMillis());
        applicationManager.getCommunicator().addAccount(tgAccount);
        tgAccount.login(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }

    private void refresh(){
        swipeRefreshLayout.setRefreshing(true);
        fill();
        swipeRefreshLayout.setRefreshing(false);
    }



    private  void fill() {
        if(applicationManager == null || activity == null || accountsList == null)
            return;
        accountsList.removeAllViews();
        //получить список
        ArrayList<TgAccount> tgAccounts = applicationManager.getCommunicator().getTgAccounts();

        if(tgAccounts.isEmpty()){
            accountsList.addView(getEmptyView());
            return;
        }
        //заполнить
        for(TgAccount tgAccount:tgAccounts){
            accountsList.addView(getTgAccount(tgAccount));
        }

        accountsList.addView(getTextView("Нажми на аккаунт, чтобы изменить его настройки"));
    }
    private View getTgAccount(final TgAccount tgAccount){
        View view = layoutInflater.inflate(R.layout.item_account_tg, null, false);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView nameLabel = view.findViewById(R.id.item_account_textView_name);
        TextView statusLabel = view.findViewById(R.id.item_account_textView_status);
        final TextView messagesReceivedLabel = view.findViewById(R.id.item_account_textView_messages_received);
        final TextView messagesSentLabel = view.findViewById(R.id.item_account_textView_messages_sent);
        final TextView apiLabel = view.findViewById(R.id.item_account_textView_api_counter);
        final TextView apiErrorsLabel = view.findViewById(R.id.item_account_textView_api_errors);
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
        tgAccount.setApiErrorsChangedListener(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        apiErrorsLabel.setText(String.valueOf(tgAccount.getErrorCounter()));
                    }
                });
            }
        });
        tgAccount.setApiCounterChangedListener(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        apiLabel.setText(String.valueOf(tgAccount.getApiCounter()));
                    }
                });
            }
        });
        tgAccount.getMessageProcessor().setOnMessagesReceivedCounterChangedListener(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        messagesReceivedLabel.setText(String.valueOf(tgAccount.getMessageProcessor().getMessagesReceivedCounter()));
                    }
                });
            }
        });
        tgAccount.getMessageProcessor().setOnMessagesSentCounterChangedListener(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        messagesSentLabel.setText(String.valueOf(tgAccount.getMessageProcessor().getMessagesSentCounter()));
                    }
                });
            }
        });
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountMenu(tgAccount, v);
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showAccountMenu(tgAccount, v);
                return false;
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.openAccountTab(tgAccount);
            }
        });

        return view;
    }
    private TextView getTextView(String text){
        TextView endText = new TextView(activity);
        endText.setText(text);
        endText.setTextColor(activity.getResources().getColor(R.color.hint_text_color, activity.getTheme()));
        endText.setTextSize(Dimension.SP, 12);
        endText.setGravity(Gravity.CENTER);
        endText.setPadding(F.dp(10), F.dp(10), F.dp(10), F.dp(10));
        endText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return endText;
    }
    private View getEmptyView(){
        return getTextView("Не добавлено ни одного аккаунта. Чтобы добавить аккаунт, нажми на \"+\" вверху.");
    }
    private void showAccountMenu(final TgAccount tgAccount, View v){
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
