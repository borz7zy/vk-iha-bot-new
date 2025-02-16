package com.fsoft.vktest.ViewsLayer.AccountTgFragment;

import android.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable; // Заменено на androidx.annotation.Nullable

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccountCore;
import com.fsoft.vktest.R;
import com.fsoft.vktest.ViewsLayer.MainActivity;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AccountTgFragment extends Fragment {
    private String TAG = "AccountTgFragment";
    private ApplicationManager applicationManager = null;
    private TgAccount tgAccount = null;
    private MainActivity activity = null;
    private Handler handler = null;

    private ImageView avatarView = null;
    private TextView nameLabel = null;
    private TextView statusLabel = null;
    private TextView messagesReceivedLabel = null;
    private TextView messagesSentLabel = null;
    private TextView apiCounterLabel = null;
    private TextView apiErrorsLabel = null;
    private View backButton = null;
    private View enabledButton = null;
    private TextView enabledLabel = null;
    private View chatEnabledButton = null;
    private TextView chatEnabledLabel = null;
    private View statusBroadcastingButton = null;
    private TextView statusBroadcastingLabel = null;

    public AccountTgFragment(TgAccount tgAccount, ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        activity = MainActivity.getInstance();
        handler = new Handler();
        this.tgAccount = tgAccount;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account_tg_settings, container, false);

        avatarView = view.findViewById(R.id.activityAccountSettingsImageViewAvatar);
        nameLabel = view.findViewById(R.id.activityAccountSettingsTextViewName);
        statusLabel = view.findViewById(R.id.activityAccountSettingsTextViewStatus);
        messagesSentLabel = view.findViewById(R.id.activityAccountSettingsTextViewSentMessages);
        apiCounterLabel = view.findViewById(R.id.activityAccountSettingsTextViewSentRequests);
        messagesReceivedLabel = view.findViewById(R.id.activityAccountSettingsTextViewReceivedMessages);
        apiErrorsLabel = view.findViewById(R.id.activityAccountSettingsTextViewApiErrors);
        enabledLabel = view.findViewById(R.id.tg_account_enabled_label);
        backButton = view.findViewById(R.id.tg_account_back_button);
        enabledButton = view.findViewById(R.id.tg_account_enabled_button);
        chatEnabledLabel = view.findViewById(R.id.tg_account_chatenabled_label);
        chatEnabledButton = view.findViewById(R.id.tg_account_chatenabled_button);
        statusBroadcastingLabel = view.findViewById(R.id.tg_account_broadcaststatus_label);
        statusBroadcastingButton = view.findViewById(R.id.tg_account_broadcaststatus_button);

        backButton.setOnClickListener(v -> activity.onBackPressed());
        enabledButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("Включить аккаунт");
            arrayList.add("Выключить аккаунт");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, arrayList);
            DialogInterface.OnClickListener listener = (dialog, which) -> {
                switch (which) {
                    case 0:
                        tgAccount.setEnabled(true);
                        refresh();
                        break;
                    case 1:
                        tgAccount.setEnabled(false);
                        refresh();
                        break;
                    default:
                        break;
                }
            };
            builder.setAdapter(adapter, listener);
            builder.show();
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

        tgAccount.setApiCounterChangedListener(() -> handler.post(() -> {
            if(tgAccount == null || apiCounterLabel == null) return;
            long counter = tgAccount.getApiCounter();
            apiCounterLabel.setText(String.valueOf(counter));
        }));

        tgAccount.setApiErrorsChangedListener(() -> handler.post(() -> {
            if(tgAccount == null || apiErrorsLabel == null) return;
            long counter = tgAccount.getErrorCounter();
            apiErrorsLabel.setText(String.valueOf(counter));
        }));

        tgAccount.getMessageProcessor().setOnMessagesReceivedCounterChangedListener(() -> handler.post(() -> {
            if(tgAccount == null || messagesReceivedLabel == null) return;
            long counter = tgAccount.getMessageProcessor().getMessagesReceivedCounter();
            messagesReceivedLabel.setText(String.valueOf(counter));
        }));

        tgAccount.getMessageProcessor().setOnMessagesSentCounterChangedListener(() -> handler.post(() -> {
            if(tgAccount == null || messagesSentLabel == null) return;
            long counter = tgAccount.getMessageProcessor().getMessagesSentCounter();
            messagesSentLabel.setText(String.valueOf(counter));
        }));

        tgAccount.setOnStateChangedListener(() -> handler.post(() -> {
            if(tgAccount == null || statusLabel == null) return;
            statusLabel.setText(tgAccount.getState());
        }));
    }

    @Override
    public void onPause() {
        tgAccount.setApiErrorsChangedListener(null);
        tgAccount.setApiCounterChangedListener(null);
        tgAccount.setOnStateChangedListener(null);
        tgAccount.getMessageProcessor().setOnMessagesSentCounterChangedListener(null);
        tgAccount.getMessageProcessor().setOnMessagesReceivedCounterChangedListener(null);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "Attached Accounts Tab...");
        super.onAttach(context);
    }

    private void refresh() {
        nameLabel.setText(tgAccount.getScreenName());
        statusLabel.setText(tgAccount.getState());
        tgAccount.getUserPhoto(new TgAccountCore.GetUserPhotoListener() {
            @Override
            public void gotPhoto(String url) {
                Picasso.get().load(url).into(avatarView);
            }

            @Override
            public void error(Throwable error) {
                Picasso.get().load(R.drawable.ic_ban).into(avatarView);
            }
        }, tgAccount.getId());

        messagesReceivedLabel.setText(String.valueOf(tgAccount.getMessageProcessor().getMessagesReceivedCounter()));
        messagesSentLabel.setText(String.valueOf(tgAccount.getMessageProcessor().getMessagesSentCounter()));
        apiCounterLabel.setText(String.valueOf(tgAccount.getApiCounter()));
        apiErrorsLabel.setText(String.valueOf(tgAccount.getErrorCounter()));

        Resources resources = getResources();
        int green = resources.getColor(R.color.green_enabled);
        int red = resources.getColor(R.color.red_disabled);

        if(enabledLabel != null) {
            boolean value = tgAccount.isEnabled();
            String text = value ? "Вкл" : "Выкл";
            int color = value ? green : red;
            enabledLabel.setTextColor(color);
            enabledLabel.setText(text);
        }

        if(chatEnabledLabel != null) {
            boolean value = tgAccount.getMessageProcessor().isChatsEnabled();
            String text = value ? "Вкл" : "Выкл";
            int color = value ? green : red;
            chatEnabledLabel.setTextColor(color);
            chatEnabledLabel.setText(text);
        }

        if(statusBroadcastingLabel != null) {
            boolean value = false;
            String text = value ? "Вкл" : "Выкл";
            int color = value ? green : red;
            statusBroadcastingLabel.setTextColor(color);
            statusBroadcastingLabel.setText(text);
        }
    }
}
