package com.fsoft.vktest.ViewsLayer.MessagesFragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsoft.vktest.AnswerInfrastructure.BotBrain;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MessagesFragment extends Fragment {
    private String TAG = "MessagesFragment";
    private ApplicationManager applicationManager = null;
    private MessagesAdapter messagesAdapter = null;
    private ListView listView = null;
    private Handler handler = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private BotBrain.OnMessageStatusChangedListener messagesListener = null;

    public MessagesFragment() {
        applicationManager = ApplicationManager.getInstance();
        handler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_messages_list, container, false);
        listView = view.findViewById(R.id.activityMessagesListListView);
        swipeRefreshLayout = view.findViewById(R.id.activityMessagesListPullToRefresh);
        messagesAdapter = new MessagesAdapter(applicationManager);
        listView.setAdapter(messagesAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (messagesListener == null)
            messagesListener = new BotBrain.OnMessageStatusChangedListener() {
                @Override
                public void messageReceived(Message message) {
                    handler.post(() -> messagesAdapter.notifyDataSetChanged());
                }

                @Override
                public void messageAnswered(Message message) {
                    handler.post(() -> messagesAdapter.notifyDataSetChanged());
                }

                @Override
                public void messageError(Message message, Exception e) {
                    handler.post(() -> messagesAdapter.notifyDataSetChanged());
                }

                @Override
                public void messageIgnored(Message message) {
                    handler.post(() -> messagesAdapter.notifyDataSetChanged());
                }
            };
        applicationManager.getBrain().addMessageListener(messagesListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        applicationManager.getBrain().remMessageListener(messagesListener);
    }
}
