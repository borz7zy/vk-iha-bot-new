package com.fsoft.vktest.Communication.Account.Telegram;


import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.Account;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private String telegraphToken = "";//бот использует телеграф для отправки лонгридов

    private long apiCounter = 0; //счётчик доступа к АПИ
    private long errorCounter = 0; //счётчик ошибок при доступе к АПИ
    private RequestQueue queue = null;

    public TgAccountCore(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
        userName = getFileStorage().getString("userName", userName);
        screenName = getFileStorage().getString("screenName", screenName);
        telegraphToken = getFileStorage().getString("telegraphToken", telegraphToken);
        queue = Volley.newRequestQueue(applicationManager.getContext().getApplicationContext());
        //Proxy proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("192.168.1.11", 8118));
    }

    public void login(Runnable howToRefresh) {
        super.login();
        new LoginWindow(applicationManager, this, howToRefresh);
    }

    @Override public void login() {
        super.login();
        new LoginWindow(applicationManager, this, null);
    }

    @Override
    protected void checkTokenValidity(final OnTokenValidityCheckedListener listener) {
        super.checkTokenValidity(listener);
        if(getId() == 0) {
            listener.onTokenFail();
            log("В аккаунте " + this + " " + state("некорректный ID"));
            return;
        }
        if(getToken() == null || getToken().isEmpty()) {
            listener.onTokenFail();
            log("В аккаунте " + this + " " + state("некорректный токен"));
            return;
        }
        getMe(new GetMeListener() {
            @Override
            public void gotUser(User user) {
                log("Аккаунт " + this + " " + state("прошёл проверку"));
                listener.onTokenPass();
            }

            @Override
            public void error(Throwable error) {
                log("Аккаунт " + this + " " + state("не прошёл проверку токена: " + error.getClass().getName() + " " + error.getMessage()));
                listener.onTokenFail();
            }
        });
    }

    @Override
    public void startAccount() {
        super.startAccount();
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
    }

    @Override
    public String toString() {
        return screenName + "("+userName+", id="+getId()+")";
    }

    public String getUserName() {
        return userName;
    }
    public String getScreenName() {
        return screenName;
    }
    public long getApiCounter() {
        return apiCounter;
    }
    public long getErrorCounter() {
        return errorCounter;
    }
    public void setUserName(String userName) {
        this.userName = userName;
        getFileStorage().put("userName", userName).commit();
    }
    public void setScreenName(String screenName) {
        this.screenName = screenName;
        getFileStorage().put("screenName", screenName).commit();
    }
    public void setTelegraphToken(String telegraphToken) {
        this.telegraphToken = telegraphToken;
        getFileStorage().put("telegraphToken", telegraphToken).commit();
    }
    public String getTelegraphToken(){
        return telegraphToken;
    }


    public void publishOnTelegraph(final CreateTelegraphPageListener listener, final String text){
        log(". Publishing text on Telegra.ph...");
        if(getTelegraphToken().isEmpty()){
            log(". Creating Telegra.ph account...");
            createTelegraphAccount(new CreateTelegraphAccountListener() {
                @Override
                public void accountCreated(String token) {
                    log(". Telegra.ph account created!");
                    setTelegraphToken(token);
                    publishOnTelegraph(listener, text);
                }

                @Override
                public void error(Throwable error) {
                    error.printStackTrace();
                    log(". Creating Telegra.ph account error: " + error.toString());
                }
            }, "iHA bot");
            return;
        }
        createTelegraphPage(listener, "iHA bot message", text);
    }

    public void getMe(final GetMeListener listener){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getMe";
        log(". Sending request: " + url);
        apiCounter ++;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                errorCounter ++;
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                errorCounter ++;
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            User user = new User(result);
                            setScreenName(user.getFirst_name() + " " + user.getLast_name());
                            setUserName(user.getUsername());
                            listener.gotUser(user);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            errorCounter ++;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                errorCounter ++;
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void getUpdates(final GetUpdatesListener listener, long offset, int timeout){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getUpdates?offset="+offset+"&timeout="+timeout;
        log(". Sending request: " + url);
        apiCounter ++;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                errorCounter ++;
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                errorCounter ++;
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONArray result = jsonObject.getJSONArray("result");
                            ArrayList<Update> updates = new ArrayList<>();
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject updateJson = result.getJSONObject(i);
                                Update update = new Update(updateJson);
                                updates.add(update);
                            }
                            listener.gotUpdates(updates);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            errorCounter ++;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                errorCounter ++;
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void sendMessage(final SendMessageListener listener, final long chat_id, String text){
        if(text.length() > 1000){
            publishOnTelegraph(new CreateTelegraphPageListener() {
                @Override
                public void pageCreated(String link) {
                    sendMessage(listener, chat_id, link);
                }

                @Override
                public void error(Throwable error) {

                }
            }, text);
            return;
        }

        JSONObject jsonObject = new JSONObject();
        try {
            //text = URLEncoder.encode(text, "UTF-8");
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("text", text);
        }
        catch (Exception e){
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
        }
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/sendMessage";//?chat_id="+chat_id+"&text="+text;
        log(". Sending request: " + url);
        apiCounter ++;
        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try{
                            log(". Got response: " + jsonObject.toString());
                            //JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                errorCounter ++;
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                errorCounter ++;
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            Message message = new Message(result);
                            listener.sentMessage(message);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            errorCounter ++;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                errorCounter ++;
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void createTelegraphAccount(final CreateTelegraphAccountListener listener, String name){
        try {
            name = URLEncoder.encode(name, "UTF-8");
        }
        catch (Exception e){
            log("! Unsopported encoding for UEREncoder");
            e.printStackTrace();
        }
        final String url =   "https://api.telegra.ph/createAccount?short_name="+name+"&author_name="+name;
        log(". Sending request: " + url);
        apiCounter ++;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                errorCounter ++;
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                errorCounter ++;
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            String token = result.getString("access_token");
                            listener.accountCreated(token);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            errorCounter ++;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                errorCounter ++;
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void createTelegraphPage(final CreateTelegraphPageListener listener, final String name, final String text){
        //Example: https://api.telegra.ph/createPage?access_token=430d287a4199d7e31a530bae39ecf1dd841a87317091370758a4aabf01f6&title=iHA%20bot%20test&content=[{%22tag%22:%22p%22,%22children%22:[%22LOLKA%22]}]
        //Example: https://api.telegra.ph/createPage?access_token=430d287a4199d7e31a530bae39ecf1dd841a87317091370758a4aabf01f6&title=iHA%20bot%20test&content=[%22wwewewewe%22]
        final String url = "https://api.telegra.ph/createPage";//?access_token="+telegraphToken;//+"&title="+name;//+"&content=[%22"+text+"%22]";
        final Map<String,String> params = new HashMap<String, String>();
        params.put("access_token", telegraphToken);
        params.put("title", name);
        if(text.length() > 31000)
            params.put("content", "[\""+text.substring(0, 31000).replace("\"", " ")+"\"]");
        else
            params.put("content", "[\""+text+"\"]");
        log(". Sending request: " + url);
        log(". # access_token=" + telegraphToken);
        log(". # title=" + name);
        log(". # content=text[" + text.length() + "]");
        log(text);
        apiCounter ++;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject jsonObject = new JSONObject(response);
                            log(". Got response: " + response);
                            if(!jsonObject.has("ok")) {
                                errorCounter ++;
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                errorCounter ++;
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            String url = result.getString("url");
                            listener.pageCreated(url);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            errorCounter ++;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                errorCounter ++;
            }
        })
        {
            @Override
            protected Map<String,String> getParams(){
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };


        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public interface SendMessageListener{
        void sentMessage(Message message);
        void error(Throwable error);
    }
    public interface GetMeListener{
        void gotUser(User user);
        void error(Throwable error);
    }
    public interface GetUpdatesListener{
        void gotUpdates(ArrayList<Update> updates);
        void error(Throwable error);
    }
    public interface CreateTelegraphAccountListener{
        void accountCreated(String token);
        void error(Throwable error);
    }
    public interface CreateTelegraphPageListener{
        void pageCreated(String link);
        void error(Throwable error);
    }
}
