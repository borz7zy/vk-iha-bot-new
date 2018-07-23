package com.fsoft.vktest.ViewsLayer.AccountsFragment;

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
import android.widget.ListView;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.ViewsLayer.MainActivity;

public class AccountsFragment extends Fragment {
    private String TAG = "AccountsFragment";
    private ApplicationManager applicationManager = null;
    private MainActivity activity = null;
    private View addAccountButton = null;
    private LinearLayout accountsList = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    public AccountsFragment() {
        applicationManager = BotService.applicationManager;
        activity = MainActivity.getInstance();
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
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Accounts Tab...");
        super.onAttach(context);
    }

    public void addAccount(){
        TgAccount tgAccount = new TgAccount(applicationManager, "tg"+System.currentTimeMillis());
        applicationManager.getCommunicator().addAccount(tgAccount);
        tgAccount.login();

    }

    private void refresh(){
        swipeRefreshLayout.setRefreshing(true);
        new AccountsListRefresher(activity, applicationManager, accountsList).fill();
        swipeRefreshLayout.setRefreshing(false);
    }
}
