package com.fsoft.vktest.ViewsLayer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsoft.vktest.Communication.Wall;

/**
 *
 * Created by Dr. Failov on 24.03.2017.
 */
public class WallView extends View {
    Wall wall = null;
    Handler handler;
    TextView textViewName;
    TextView textViewActive;
    TextView textViewStatus;
    TextView textViewPostTotal;
    TextView textViewCommentTotal;
    TextView textViewRepliedTotal;
    Context context = null;
    AlertDialog alertDialog = null;

    WallView(Context context, Wall wall) {
        super(context);
        this.wall = wall;
        this.context = context;
        handler = new Handler();
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
        addView(getDelimiter(context));

        int color = active ? (initialized ? Color.GREEN : Color.RED) : Color.GRAY;

        {
            TextView textView = textViewName = new TextView(context);
            textView.setPadding(20, 0, 0, 0);
            textView.setTextColor(color);
            textView.setText(getWallName());
            textView.setTextSize(18);
            addView(textView);
        }
        {
            TextView textView = textViewActive = new TextView(context);
            textView.setTextColor(color);
            textView.setText("активен = " + isActive());
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = textViewStatus = new TextView(context);
            textView.setTextColor(color);
            textView.setText("Состояние = " + wallStatus);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = textViewPostTotal = new TextView(context);
            textView.setTextColor(color);
            textView.setText("новых записей = " + messagesDetected);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = textViewCommentTotal = new TextView(context);
            textView.setTextColor(color);
            textView.setText("новых комментариев = " + commentsDetected);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = textViewRepliedTotal = new TextView(context);
            textView.setTextColor(color);
            textView.setText("опубликовано ответов = " + messagesReplied);
            textView.setTextSize(10);
            addView(textView);
        }
        addView(getDelimiter(context));
    }
    private View getDelimiter(Context context){
        View delimiter = new View(context);
        delimiter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
        delimiter.setBackgroundColor(Color.DKGRAY);
        return delimiter;
    }
    public void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        builder.setView(scrollView);
        {
            TextView textView = new TextView(context);
            textView.setText("Стена " + getWallName());
            textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(20);
            textView.setTextColor(Color.WHITE);
            linearLayout.addView(textView);
            linearLayout.addView(getDelimiter(context));
        }

        linearLayout.addView(getOnOffRow("Активный ("+active+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                wallStatus = "Состояние изменено вручную.";
                active = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                wallStatus = "Состояние изменено вручную.";
                active = (false);
                closeDialog();
            }
        }));

        {
            Button button = new Button(context);
            button.setText("Удалить");
            button.setTextColor(Color.RED);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    messageBox(applicationManager.processCommands("wall rem " + id, applicationManager.getUserID()));
                    closeDialog();
                }
            });
            linearLayout.addView(button);
        }
        alertDialog = builder.show();
    }
    public void closeDialog(){
        if(alertDialog != null)
            alertDialog.dismiss();
    }
    private LinearLayout getOnOffRow(String text, OnClickListener onClickListener, OnClickListener offClickListener){
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setPadding(10, 0, 0, 0);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button buttonOn = new Button(context);
        buttonOn.setText("вкл");
        buttonOn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonOn.setTextColor(Color.GREEN);
        buttonOn.setOnClickListener(onClickListener);

        Button buttonOff = new Button(context);
        buttonOff.setText("выкл");
        buttonOff.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonOff.setTextColor(Color.YELLOW);
        buttonOff.setOnClickListener(offClickListener);

        LinearLayout horizontalLayout = new LinearLayout(context);
        horizontalLayout.setOrientation(HORIZONTAL);
        horizontalLayout.addView(textView);
        horizontalLayout.addView(buttonOn);
        horizontalLayout.addView(buttonOff);
        return horizontalLayout;
    }

    void messageBox(final String text){
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton("OK", null);
                builder.setMessage(text);
                builder.setTitle("Результат");
                builder.show();
            }
        });
    }
}
