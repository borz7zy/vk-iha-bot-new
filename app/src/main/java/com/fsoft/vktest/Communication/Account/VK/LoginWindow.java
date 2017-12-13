package com.fsoft.vktest.Communication.Account.VK;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.F;
import com.perm.kate.api.Auth;

/**
 * Это окно должно заниматься процедурой логина в аккаунт.
 * В его задачи входит:
 * - получить обьект аккаунта который нужно залогинить
 * - открыть окно браузера без выхода из приложения
 * - открыть вебстраницу логина и контролировать возможные выходы с неё
 * - обрабатывать переходы по страницам до того как пользователь залогинится
 * - как только токен получен, закрыться и сообщить об успешном логине
 * - самостоятельно задать токен в обьекте vkAccount и сделать Start Account
 *
 *
 * login: https://vk.com/dev/access_token
 * validation: https://vk.com/dev/need_validation
 * Created by Dr. Failov on 21.02.2017.
 */
public class LoginWindow extends CommandModule {
    private Handler handler = new Handler();
    private VkAccountCore vkAccount = null;
    private Dialog loginDialog = null;
    private String customUrl = null;

    public LoginWindow(ApplicationManager applicationManager, VkAccountCore vkAccount, String customUrl) {
        super(applicationManager);
        this.vkAccount = vkAccount;
        this.customUrl = customUrl;
        showLoginWindow();
    }
    public LoginWindow(ApplicationManager applicationManager, VkAccountCore vkAccount){
        this(applicationManager, vkAccount, null);
    }

    private void showLoginWindow(){
        if(loginDialog == null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Войди в аккаунт");
                        Context context = applicationManager.getContext();
                        final LoginView loginView = new LoginView(context);
                        loginView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                        LinearLayout linearLayout = new LinearLayout(context);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        linearLayout.addView(loginView);

                        TextView textView = new TextView(context);
                        textView.setText("Войди в аккаунт. Это окно само закроется, когда вход будет успешным.");
                        textView.setPadding(F.dp(10), F.dp(10), F.dp(10), F.dp(10));
                        linearLayout.addView(textView);

                        Button button = new Button(context);
                        button.setText("Назад");
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                loginView.goBack();
                            }
                        });
                        button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        linearLayout.addView(button);

                        loginDialog = new Dialog(context);
                        //loginDialog.setTitle("Войдите в аккаунт");
                        loginDialog.setContentView(linearLayout);
                        loginDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
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
    private void loginSuccess(String token, long id){
        closeLoginWindow();
        if(vkAccount != null) {
            vkAccount.setToken(token);
            vkAccount.setId(id);
            vkAccount.resetValidation_url();
            vkAccount.startAccount();
        }
        //// TODO: 10.04.2017 update instruction
        String help = "Поздравляю! Ты успешно вошёл в аккаунт. Теперь бот читает твою личку и будет отвечать там на сообщения с обращением \"Бот,\".\n" +
                "Можешь перейти на вкладку \"Стены\" и добавить свою стену, чтобы бот и там отвечал." +
                "- Напиши боту текст \"botcmd help\" от имени владельца, и он сообщит тебе все имеющиеся команды.\n" +
                "- Чтобы просто поговорить с ботом, каждое своё сообщение начинай с обращения \"Бот,\", например: \"Бот, как дела?\".\n" +
                "Дальше разбирайся сам:) Удачи!";
        log(help);
        messageBox(help);
    }

    class LoginView extends WebView {
        public LoginView(Context context) {
            super(context);
            setWebViewClient(new VkontakteWebViewClient());
            if(customUrl != null)
                loadUrl(customUrl);
            else
                loadUrl(Auth.getUrl(VkAccount.API_ID, Auth.getSettings()));
        }
        class VkontakteWebViewClient extends WebViewClient {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                parseUrl(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            private void parseUrl(String url) {
                //Tuto: https://vk.com/dev/implicit_flow_user
                //revoke=1	Параметр, указывающий, что необходимо не пропускать этап подтверждения прав, даже если пользователь уже авторизован.

                // URL: https://oauth.vk.com/authorize?client_id=4485671&revoke=1&response_type=token&display=mobile&redirect_uri=https://oauth.vk.com/blank.html&scope=notify,friends,photos,audio,video,docs,status,notes,pages,wall,groups,messages,offline,notifications

                // LOGIN URL: https://oauth.vk.com/oauth/authorize?client_id=4485671&redirect_uri=https%3A%2F%2Foauth.vk.com%2Fblank.html&response_type=token&scope=0&v=&state=&revoke=1&display=page&success=1
                // ERROR URL: https://oauth.vk.com/blank.html#error=access_denied&error_reason=user_denied&error_description=User denied your request
                // SUCCESS URL: https://oauth.vk.com/blank.html#access_token=4863e87054193cd0449124239a64324151d144a153666a597d9e8ad21f2fd0c8ed9349febe8afd894395a&expires_in=0&user_id=10299185
                //

                //
                try {
                    if(url==null)
                        return;
                    log(". URL=" + url);
                    if(url.startsWith(Auth.redirect_url) && url.contains("access_token=")) {
                        String[] auth=Auth.parseRedirectUrl(url);
                        String token = auth[0];
                        long id = Long.parseLong(auth[1]);
                        if(token != null && id != 0)
                            loginSuccess(token, id);
                    }
                } catch (Throwable e) {
                    log("! Ошибка разбора ссылки!\n" +
                            "Ссылка: " + url + "\n" +
                            "Ошибка: " + e.toString() + "\n");
                    e.printStackTrace();
                }
            }
        }
    }
}
