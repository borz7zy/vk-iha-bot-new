package com.fsoft.vktest.Communication.Account.Telegram;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.Account;

import org.json.JSONObject;

/**
 *
 * Конкретно этот класс отвечает за зеркалирование функций, распределение нагрузки, обработку ошибок.
 *
 *
 * ВСЕ МЕТОДЫ ЭТОГО КЛАССА КРОМЕ МЕТОДОВ CommandModule АСИНХРОННЫ!!!
 * ВЫЗЫВАТЬ ТОЛЬКО ИЗ ВТОРОГО\ТРЕТЬЕГО ПОТОКА!
 * Этот класс должен отвечать только за редирект методов, за распределение нагрузки и
 * решение возникающих проблем. Также за процедуру логина.
 * Он же в идеале должен при вызове ЕГО методов извне обрабатывать ошибки, может, управлять очередью.
 * Если возникают эксепшоны, хэндлить их здесь.
 * Ну, выводить там окно повторного логина, например.
 *
 * Статус аккаунта описывать так:
 * + Аккаунт включен
 * + Токен OK
 * + Аккаунт запущен
 *
 * Общая схема такая:
 * - получаем пустой аккаунт
 * - выводим диалог логина
 * - работаем с ним пока не получим токен
 * - получаем токен
 * - выполняем тестовый запрос (любой)
 * - если тестовый запрос проходит, то token_ok = true
 * - если enabled = true то начинаем startAccount()
 * ...работаем
 * - если возникает ошибка то: token_ok=false; stopAccount();
 * - если ошибка не критическая, то: через 5 минут делаем startAccount() & token_ok=true
 *
 * Created by Dr. Failov on 21.07.2018.
 */

public class TgAccountCore extends Account {
    static public final int RETRIES = 3;//количество повторных попыток в случае ошибки
    //это имя пользователя которому принадлежит этот аккаунт. Оно хранится здесь временно.
    // Когда оно нам нужно, обращаемся к геттеру. если нужно получить имя аккаунта, обращаемся к toString()
    private String userName = null;
    private String screenName = null;

    private long apiCounter = 0; //счётчик доступа к АПИ
    private long errorCounter = 0; //счётчик ошибок при доступе к АПИ
    private RequestQueue queue = null;

    public TgAccountCore(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
        queue = Volley.newRequestQueue(applicationManager.getContext());
    }

    @Override public void login() {
        super.login();
        new LoginWindow(applicationManager, this);
    }

    @Override
    protected void startAccount() {
        super.startAccount();
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
    }

    public void getMe(final GetMeListener listener){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getMe";
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            User user = new User(new JSONObject(response));
                        }
                        catch (Exception e){
                            listener.error(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public interface GetMeListener{
        void gotUser(User user);
        void error(Throwable error);
    }
}
