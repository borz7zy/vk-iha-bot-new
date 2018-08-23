package com.fsoft.vktest.ViewsLayer.AccountTgFragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.ViewsLayer.AccountsFragment.AccountsListRefresher;
import com.fsoft.vktest.ViewsLayer.MainActivity;

public class AccountTgFragment extends Fragment {
    private String TAG = "AccountTgFragment";
    private ApplicationManager applicationManager = null;
    private TgAccount tgAccount = null;
    private MainActivity activity = null;
    private Handler handler = null;


    private TextView messagesReceivedLabel = null;
    private TextView messagesSentLabel = null;
    private TextView apiCounterLabel = null;
    private TextView apiErrorsLabel = null;
    private View enabledButton = null;
    private TextView enabledLabel = null;
    private View messagesEnabledButton = null;
    private TextView messagesEnabledLabel = null;
    private View chatEnabledButton = null;
    private TextView chatEnabledLabel = null;
    private View statusBroadcastingButton = null;
    private TextView statusBroadcastingLabel = null;


    public AccountTgFragment(TgAccount tgAccount) {
        applicationManager = BotService.applicationManager;
        activity = MainActivity.getInstance();
        handler = new Handler();
        this.tgAccount = tgAccount;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account_tg_settings, container, false);

        messagesSentLabel = view.findViewById(R.id.activityAccountSettingsTextViewSentMessages);
        apiCounterLabel = view.findViewById(R.id.activityAccountSettingsTextViewSentRequests);
        messagesReceivedLabel = view.findViewById(R.id.activityAccountSettingsTextViewReceivedMessages);
        apiErrorsLabel = view.findViewById(R.id.activityAccountSettingsTextViewApiErrors);
        enabledLabel = view.findViewById(R.id.tg_account_enabled_label);
        enabledButton = view.findViewById(R.id.tg_account_enabled_button);
        messagesEnabledLabel = view.findViewById(R.id.tg_account_messagesenabled_label);
        messagesEnabledButton = view.findViewById(R.id.tg_account_messagesenabled_button);
        chatEnabledLabel = view.findViewById(R.id.tg_account_chatenabled_label);
        chatEnabledButton = view.findViewById(R.id.tg_account_chatenabled_button);
        statusBroadcastingLabel = view.findViewById(R.id.tg_account_broadcaststatus_label);
        statusBroadcastingButton = view.findViewById(R.id.tg_account_broadcaststatus_button);



        enabledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            }
        });

        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        if(tgAccount == null)
            return;
        //Настройка чтобы работало обновление полей с текстом в реальном времени
        tgAccount.setApiCounterChangedListener(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(tgAccount == null)
                            return;
                        if(apiCounterLabel == null)
                            return;
                        long counter = tgAccount.getApiCounter();
                        apiCounterLabel.setText(String.valueOf(counter));
                    }
                });
            }
        });
        tgAccount.setApiErrorsChangedListener(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(tgAccount == null)
                            return;
                        if(apiErrorsLabel == null)
                            return;
                        long counter = tgAccount.getErrorCounter();
                        apiErrorsLabel.setText(String.valueOf(counter));
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
                        if(tgAccount == null)
                            return;
                        if(messagesReceivedLabel == null)
                            return;
                        long counter = tgAccount.getMessageProcessor().getMessagesReceivedCounter();
                        messagesReceivedLabel.setText(String.valueOf(counter));
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
                        if(tgAccount == null)
                            return;
                        if(messagesSentLabel == null)
                            return;
                        long counter = tgAccount.getMessageProcessor().getMessagesSentCounter();
                        messagesSentLabel.setText(String.valueOf(counter));
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        tgAccount.setApiErrorsChangedListener(null);
        tgAccount.setApiCounterChangedListener(null);
        tgAccount.getMessageProcessor().setOnMessagesSentCounterChangedListener(null);
        tgAccount.getMessageProcessor().setOnMessagesReceivedCounterChangedListener(null);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Accounts Tab...");
        super.onAttach(context);
    }

    private void refresh(){

    }
}
