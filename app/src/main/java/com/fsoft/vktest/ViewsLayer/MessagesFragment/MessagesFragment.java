package com.fsoft.vktest.ViewsLayer.MessagesFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.R;

import java.util.ArrayList;

public class MessagesFragment extends Fragment {
    private String TAG = "MessagesFragment";
    private ApplicationManager applicationManager = null;


    public MessagesFragment() {
        applicationManager = ApplicationManager.getInstance();
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

    class MessagesAdapter extends BaseAdapter{
        private ArrayList<MessageHistory.MessageStatus> messageStatuses;
        private LayoutInflater layoutInflater = null;

        public MessagesAdapter() {
            messageStatuses = applicationManager.getMessageHistory().getMessages();
            layoutInflater = LayoutInflater.from(applicationManager.getContext());
        }

        @Override
        public int getCount() {
            if(messageStatuses == null)
                return 0;
            return messageStatuses.size();
        }

        @Override
        public Object getItem(int position) {
            if(messageStatuses == null)
                return null;
            return messageStatuses.get(position);
        }

        @Override
        public long getItemId(int position) {
            if(messageStatuses == null)
                return 0;
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(messageStatuses == null || layoutInflater == null)
                return null;
            View view = convertView;
            if(view == null)
                view = layoutInflater.inflate(R.layout.item_message_pair, null, false);
            MessageHistory.MessageStatus message = messageStatuses.get(position);
            view = fillView(view, message);
            return view;
        }

        private View fillView(View view, MessageHistory.MessageStatus messageStatus){
            TextView userNameLabel = view.findViewById(R.id.bubble_in_username);
            TextView receivedTextLabel = view.findViewById(R.id.bubble_in_text);
            ImageView userAvatarView = view.findViewById(R.id.bubble_in_avatar);
            TextView timeLabel = view.findViewById(R.id.bubble_in_time);
            ImageView checkView = view.findViewById(R.id.bubble_in_check);
            TextView botNameLabel = view.findViewById(R.id.bubble_in_bot_name);
            TextView botAnswerTextLabel = view.findViewById(R.id.bubble_in_bot_answer);
            ImageView botAvatarView = view.findViewById(R.id.bubble_in_bot_avatar);
            ProgressBar progressBarView = view.findViewById(R.id.bubble_in_bot_progressbar);

            userNameLabel.setText(messageStatus.getMessage().getAuthor().getName());
            receivedTextLabel.setText(messageStatus.getMessage().getText());
        }
    }
}
