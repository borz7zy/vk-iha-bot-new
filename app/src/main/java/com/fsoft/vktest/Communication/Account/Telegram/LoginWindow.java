package com.fsoft.vktest.Communication.Account.Telegram;

import android.app.Dialog;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.R;
import com.fsoft.vktest.ViewsLayer.MainActivity;

/**
 * Это окно должно заниматься процедурой логина в аккаунт.
 * В его задачи входит:
 * - получить обьект аккаунта который нужно залогинить
 * - открыть окно логина и ждать пока пользователь залогинится
 * - проверить токен на валидность
 * - как только токен получен, закрыться и сообщить об успешном логине
 * - самостоятельно задать токен в обьекте vkAccount и сделать Start Account
 *
 *
 * Created by Dr. Failov on 22.07.2018.
 */
public class LoginWindow extends CommandModule {
    private Handler handler = new Handler();
    private TgAccountCore tgAccount = null;
    private Dialog loginDialog = null;
    private MainActivity context = null;

    private EditText tokenField;
    private TextView saveButton;
    private View closeButton;

    public LoginWindow(ApplicationManager applicationManager, TgAccountCore tgAccount) {
        super(applicationManager);
        context = MainActivity.getInstance();
        this.tgAccount = tgAccount;
        showLoginWindow();
    }

    private void showLoginWindow(){
        if(loginDialog == null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Войди в аккаунт");
                        loginDialog = new Dialog(context);
                        loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        loginDialog.setContentView(R.layout.dialog_add_telegram_account);
                        loginDialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                        tokenField = loginDialog.findViewById(R.id.dialog_add_telegram_account_field_token);
                        saveButton = loginDialog.findViewById(R.id.dialogAdd_telegram_accountButtonSave);
                        closeButton = loginDialog.findViewById(R.id.dialogadd_telegram_accountButtonCancel);
                        if(saveButton != null)
                            saveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    saveButton();
                                }
                            });
                        if(closeButton != null)
                            closeButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    closeLoginWindow();
                                    Toast.makeText(context, applicationManager.getCommunicator().remTgAccount(tgAccount), Toast.LENGTH_SHORT).show();
                                }
                            });
                        loginDialog.show();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        log("! Ошибка показа окна логина: " + e.toString());
                    }
                }
            });
        }
    }
    private void closeLoginWindow(){
        if(loginDialog != null) {
            loginDialog.dismiss();
            loginDialog = null;
        }
    }
    private void saveButton() {
        //проверить и если валидно сохранить
        if(tokenField == null)
            return;
        String tokenString = tokenField.getText().toString();
        String[] parts = tokenString.split(":");
        if(parts.length != 2) {
            Toast.makeText(context, "Токен введён неверно.", Toast.LENGTH_SHORT).show();
            return;
        }
        String idString = parts[0];
        String token = parts[1];
        long id = 0;
        try {
            id = Long.parseLong(idString);
        }
        catch (Exception e){
            Toast.makeText(context, "Токен введён неверно: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        tgAccount.setId(id);
        tgAccount.setToken(token);
        tgAccount.getMe(new TgAccountCore.GetMeListener() {
            @Override
            public void gotUser(User user) {
                Toast.makeText(context, "Вход выполнен!", Toast.LENGTH_SHORT).show();
                closeLoginWindow();
                tgAccount.startAccount();
            }

            @Override
            public void error(Throwable error) {
                saveButton.setEnabled(true);
                saveButton.setText("Сохранить");
                tgAccount.setId(0);
                tgAccount.setToken("");
                Toast.makeText(context, "Токен не сработал: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        saveButton.setEnabled(false);
        saveButton.setText("Проверка...");
    }
    private void loginSuccess(String token, long id){
        closeLoginWindow();
        if(tgAccount != null) {
            tgAccount.setToken(token);
            tgAccount.setId(id);
            tgAccount.startAccount();
        }
    }
}
