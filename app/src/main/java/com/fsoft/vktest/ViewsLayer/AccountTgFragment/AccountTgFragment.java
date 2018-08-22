package com.fsoft.vktest.ViewsLayer.AccountTgFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        this.tgAccount = tgAccount;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account_tg_settings, container, false);
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

            }
        });

        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Accounts Tab...");
        super.onAttach(context);
    }

    private void refresh(){

    }
}
