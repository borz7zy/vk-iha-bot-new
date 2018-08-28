package com.fsoft.vktest.ViewsLayer.MessagesFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsoft.vktest.R;

public class MessagesFragment extends Fragment {
    private String TAG = "MessagesFragment";


    public MessagesFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_messages_list, container, false);
    }


    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Messages Tab...");
        super.onAttach(context);
    }
}
