package com.fsoft.vktest.ViewsLayer;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsoft.vktest.R;

public class AccountsFragment extends Fragment {
    private String TAG = "AccountsFragment";

    public AccountsFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_accounts_list, container, false);
    }


    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Accounts Tab...");
        super.onAttach(context);
    }
}
