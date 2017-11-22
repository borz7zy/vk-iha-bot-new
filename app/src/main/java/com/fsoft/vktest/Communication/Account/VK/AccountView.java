package com.fsoft.vktest.Communication.Account.VK;

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

/**
 * ���� ������ ������ ��������� �� ���� ������� � ���������� ��� ���������
 * Created by Dr. Failov on 21.02.2017.
 */
public class AccountView extends LinearLayout {
    Handler handler;
    boolean lastState = false;
    Context context = null;
    AlertDialog alertDialog = null;

    AccountView(Context context) {
        super(context);
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

        int color = isActive() ? (isReady() ? Color.GREEN : Color.YELLOW) : Color.GRAY;

        {
            TextView textView = new TextView(context);
            textView.setPadding(20, 0, 0, 0);
            textView.setTextColor(color);
            textView.setText(userName);
            textView.setTextSize(18);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(isActive() ? color : Color.rgb(255, 100, 100));
            textView.setText("���������: " + (isActive() ? "�������" : "��������") + " (" + getState() + ")");
            textView.setTextSize(isActive()?10:13);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("�������������: " + (isReady()?"��������":"�����"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("��������� � API: " + getApiCounter() + " ��������");
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("��������� ���������: " + (messageProcessing ? "��������" : "���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("������ � �������: " + (processChats ? "��������" : "���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("���������� ��������� ����������� � �����: " + (replyMessagesInChats ? "��������" : "���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("�������� �����������: " + (replyAnyMessage?"��������":"���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("������� ���������: " + messageCounter);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("���������� ���������: " + messageSent);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("������� ������: " + acceptedRequests);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("��������� ��������: " + rejectedFolowers);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("������������� ����������� ������: " + blacklistedFolowers);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("���������� �������: " + (statusBroadcasting?"��������":"���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("�������� ���� ������: " + (acceptAnyRequest?"��������":"���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("���������� ��������: " + (rejectFollowers?"��������":"���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("�������� � �� ��� ��� ������ �� ������: " + (blacklistFollowers?(rejectFollowers?"��������":"��������� �������� ���������� ��������"):"���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("����� �� ����� ��� �� �������� � �����: " + (exitFromOfftopChats?"��������":"���������"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("����� �� ����� � ������: " + (exitFromBotsChats?"��������":"���������"));
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
            textView.setText("������� " + userName);
            textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(20);
            textView.setTextColor(Color.WHITE);
            linearLayout.addView(textView);
            linearLayout.addView(getDelimiter(context));
        }

        linearLayout.addView(getOnOffRow("�������� ("+isActive()+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setActive(true, "�������� �������");
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setActive(false, "��������� �������");
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("�������� ����������� ("+replyAnyMessage+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyAnyMessage = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyAnyMessage = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("��������� ��� ������ ("+acceptAnyRequest+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setAcceptAnyRequest(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setAcceptAnyRequest(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("���������� ������� ("+statusBroadcasting+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setStatusBroadcasting(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setStatusBroadcasting(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("������������ ��������� ("+messageProcessing+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setMessageProcessing(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setMessageProcessing(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("�������� �� ��������� ����� ("+exitFromOfftopChats+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromOfftopChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromOfftopChats = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("�������� �� ����� � ������ ("+exitFromBotsChats+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromBotsChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromBotsChats = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("�������� � ������� ("+processChats+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                processChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                processChats = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("������������ �� ��������� �� ������ ("+rejectFollowers+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRejectFollowers(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRejectFollowers(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("���������� � �� ��� ��� ������ �� ������ ("+(rejectFollowers && blacklistFollowers)+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRejectFollowers(true);
                blacklistFollowers = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                blacklistFollowers = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("���������� ��������� ����������� � ����� ("+(replyMessagesInChats)+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyMessagesInChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyMessagesInChats = (false);
                closeDialog();
            }
        }));


        {
            Button button = new Button(context);
            button.setText("��������� � �������");
            button.setTextColor(Color.YELLOW);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLoginWindow();
                    closeDialog();
                }
            });
            linearLayout.addView(button);
        }
        {
            Button button = new Button(context);
            button.setText("�������");
            button.setTextColor(Color.RED);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    applicationManager.vkAccounts.removeAccount(thisAccount);
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
        textView.setText(text.trim());
        textView.setPadding(0, 0, 0, 0);
        textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button buttonOn = new Button(context);
        buttonOn.setText("���");
        buttonOn.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonOn.setTextColor(Color.GREEN);
        buttonOn.setOnClickListener(onClickListener);

        Button buttonOff = new Button(context);
        buttonOff.setText("����");
        buttonOff.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonOff.setTextColor(Color.YELLOW);
        buttonOff.setOnClickListener(offClickListener);

        LinearLayout horizontalLayout = new LinearLayout(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        horizontalLayout.setPadding(10, 10, 10, 10);
        horizontalLayout.setOrientation(HORIZONTAL);
        horizontalLayout.setGravity(Gravity.CENTER);
        horizontalLayout.addView(textView);
        horizontalLayout.addView(buttonOn);
        horizontalLayout.addView(buttonOff);
        return horizontalLayout;
    }
}
