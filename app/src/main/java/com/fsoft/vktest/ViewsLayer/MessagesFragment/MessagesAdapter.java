package com.fsoft.vktest.ViewsLayer.MessagesFragment;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

class MessagesAdapter extends BaseAdapter {
    private ApplicationManager applicationManager = null;
    private ArrayList<MessageStatus> messageStatuses;
    private LayoutInflater layoutInflater = null;
    private SimpleDateFormat sdf = null;

    public MessagesAdapter(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        // Если getMessages() возвращает null, используем пустой список
//        messageStatuses = applicationManager.getMessageHistory() != null ? applicationManager.getMessageHistory().getMessages() : new ArrayList<>();
        layoutInflater = LayoutInflater.from(applicationManager.getContext());
        sdf = new SimpleDateFormat("HH:mm");
    }

    @Override
    public int getCount() {
        return (messageStatuses != null) ? messageStatuses.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return (messageStatuses != null && position >= 0 && position < messageStatuses.size()) ? messageStatuses.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return (messageStatuses != null && position >= 0 && position < messageStatuses.size()) ? position : -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (messageStatuses == null || position < 0 || position >= messageStatuses.size()) {
            return null;
        }
        View view = convertView;
        if (view == null)
            view = layoutInflater.inflate(R.layout.item_message_pair, null, false);

        MessageStatus message = messageStatuses.get(position);
        return fillView(view, message);
    }

    private View fillView(View view, MessageStatus messageStatus) {
        if (messageStatus == null) return view; // Если messageStatus null, возвращаем view без изменений

        TextView userNameLabel = view.findViewById(R.id.bubble_in_username);
        TextView receivedTextLabel = view.findViewById(R.id.bubble_in_text);
        ImageView userAvatarView = view.findViewById(R.id.bubble_in_avatar);
        TextView timeLabel = view.findViewById(R.id.bubble_in_time);
        ImageView checkView = view.findViewById(R.id.bubble_in_check);
        TextView botNameLabel = view.findViewById(R.id.bubble_in_bot_name);
        TextView botAnswerTextLabel = view.findViewById(R.id.bubble_in_bot_answer);
        ImageView botAvatarView = view.findViewById(R.id.bubble_in_bot_avatar);
        ProgressBar progressBarView = view.findViewById(R.id.bubble_in_bot_progressbar);

        if (messageStatus.getMessage() != null) {
            userNameLabel.setText(messageStatus.getMessage().getAuthor().getName());
            receivedTextLabel.setText(messageStatus.getMessage().getText());
            timeLabel.setText(sdf.format(messageStatus.getMessage().getDate()));
            botNameLabel.setText(messageStatus.getMessage().getBotAccount().getScreenName());
//            messageStatus.getMessage().getBotAccount().fillAvatar(botAvatarView);
        }

        // Остальная логика для состояния
        if (messageStatus.isReceived()) {
            checkView.setImageResource(R.drawable.ic_clock_white);
            progressBarView.setVisibility(View.VISIBLE);
            botAnswerTextLabel.setText("Подготовка ответа...");
        } else if (messageStatus.isAnswered()) {
            checkView.setImageResource(R.drawable.ic_check);
            progressBarView.setVisibility(View.GONE);
            botAnswerTextLabel.setText(messageStatus.getMessage().getAnswer().toString());
        } else if (messageStatus.isIgnored()) {
            checkView.setImageResource(R.drawable.ic_minus);
            progressBarView.setVisibility(View.GONE);
            botAnswerTextLabel.setText("Пропуск");
        } else if (messageStatus.isError()) {
            checkView.setImageResource(R.drawable.ic_cross_n_v);
            progressBarView.setVisibility(View.GONE);
            botAnswerTextLabel.setText("Ошибка подбора ответа");
        }

        return view;
    }

}
