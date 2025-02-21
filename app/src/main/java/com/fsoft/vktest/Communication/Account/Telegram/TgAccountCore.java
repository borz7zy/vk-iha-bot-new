package com.fsoft.vktest.Communication.Account.Telegram;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.Account;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.NewViewsLayer.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Конкретно этот класс отвечает за зеркалирование функций, распределение нагрузки, обработку ошибок.
 *
 * ВСЕ МЕТОДЫ ЭТОГО КЛАССА КРОМЕ МЕТОДОВ CommandModule АСИНХРОННЫ!!!
 * ВЫЗЫВАТЬ ТОЛЬКО ИЗ ВТОРОГО\ТРЕТЬЕГО ПОТОКА!
 */

public class TgAccountCore extends Account {
    private String token = null;
    static public final int RETRIES = 3;
    private String userName = null;
    private String telegraphToken = "";
    private Runnable apiCounterChangedListener = null;
    private long apiCounter = 0;
    private Runnable apiErrorsChangedListener = null;
    private long errorCounter = 0;
    private RequestQueue queue = null;
    private Context context; // Add Context
    private long id = 0;

    public TgAccountCore(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
        this.context = applicationManager.getContext(); // Get the context
        userName = getFileStorage().getString("tguserName", userName);
        token = getFileStorage().getString("tgtoken", token);
        id = getFileStorage().getLong("tgid", id);
        telegraphToken = getFileStorage().getString("telegraphToken", telegraphToken);
        queue = Volley.newRequestQueue(context.getApplicationContext()); // Use the context
    }

    @Override
    protected void checkTokenValidity(final OnTokenValidityCheckedListener listener) {
        super.checkTokenValidity(listener);
        if (getId() == 0) {
            listener.onTokenFail();
            log("В аккаунте " + this + " " + state("некорректный ID"));
            return;
        }
        if (getToken() == null || getToken().isEmpty()) {
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
        return getScreenName() + "(" + userName + ", id=" + getId() + ")";
    }

    public String getUserName() {
        return userName;
    }

    public long getApiCounter() {
        return apiCounter;
    }

    public void incrementApiCounter() {
        apiCounter++;
        if (apiCounterChangedListener != null)
            apiCounterChangedListener.run();
    }

    public void incrementErrorCounter() {
        errorCounter++;
        if (apiErrorsChangedListener != null)
            apiErrorsChangedListener.run();
    }

    public long getErrorCounter() {
        return errorCounter;
    }

    public void setUserName(String userName) {
        this.userName = userName;
        getFileStorage().put("tguserName", userName).commit();
    }

    public void setTelegraphToken(String telegraphToken) {
        this.telegraphToken = telegraphToken;
        getFileStorage().put("telegraphToken", telegraphToken).commit();
    }

    public String getTelegraphToken() {
        return telegraphToken;
    }

    public void setApiCounterChangedListener(Runnable apiCounterChangedListener) {
        this.apiCounterChangedListener = apiCounterChangedListener;
    }

    public void setApiErrorsChangedListener(Runnable apiErrorsChangedListener) {
        this.apiErrorsChangedListener = apiErrorsChangedListener;
    }

    public void publishOnTelegraph(final CreateTelegraphPageListener listener, final String text) {
        log(". Publishing text on Telegra.ph...");

        if (getTelegraphToken().isEmpty()) {
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

    public void getMe(final GetMeListener listener) {
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/getMe";
        log(". Sending request: " + url);
        incrementApiCounter();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            User user = new User(result);
                            setScreenName(user.getFirst_name() + " " + user.getLast_name());
                            setUserName(user.getUsername());
                            listener.gotUser(user);
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void getUpdates(final GetUpdatesListener listener, long offset, int timeout) {
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/getUpdates?offset=" + offset + "&timeout=" + timeout;
        log(". Sending request: " + url);
        incrementApiCounter();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
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
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void sendMessage(final SendMessageListener listener, final long chat_id, String text) {
        if (text.length() > 4000) {
            publishOnTelegraph(new CreateTelegraphPageListener() {
                @Override
                public void pageCreated(String link) {
                    sendMessage(listener, chat_id, link);
                }

                @Override
                public void error(Throwable error) {
                    listener.error(error); // Pass the error to the original listener
                }
            }, text);
            return;
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("text", text);
        } catch (Exception e) {
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
            listener.error(e); // Pass the error to the original listener
            return;
        }
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendMessage";
        log(". Sending request: " + url);
        incrementApiCounter();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            log(". Got response: " + jsonObject.toString());
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            Message message = new Message(result);
                            listener.sentMessage(message);
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }


    public void getUserPhoto(final GetUserPhotoListener listener, final long user_id) {
        log(". Получение фотографии пользователя " + user_id + "...");
        getUserProfilePhotos(new GetUserProfilePhotosListener() {
            @Override
            public void gotPhotos(UserProfilePhotos photos) {
                log(". Запрос на архив фотографий " + user_id + " выполнен.");
                if (photos.getArrayPhotos().isEmpty())
                    listener.error(new Exception(log("! У юзера нет фотографий!")));
                if (photos.getArrayPhotos().get(0).isEmpty())
                    listener.error(new Exception(log("! У фото нет размера!")));
                PhotoSize photoSize = photos.getArrayPhotos().get(0).get(1);
                final String file_id = photoSize.getFile_id();
                getFile(new GetFileListener() {
                    @Override
                    public void gotFile(File file) {
                        log(". Запрос на файл фотографии " + user_id + " выполнен.");
                        if (file.getFile_path().isEmpty())
                            listener.error(new Exception(log("! Почему-то не получен адрес файла")));
                        String url = file.getUrl(TgAccountCore.this);
                        log(". Ссылка на фотографию:   " + url);
                        listener.gotPhoto(url);
                    }

                    @Override
                    public void error(Throwable error) {
                        log("! Ошибка выполнения запроса на файл фотографи " + user_id + ": " + error.getMessage() + ".");
                        listener.error(error);
                    }
                }, file_id);
            }

            @Override
            public void error(Throwable error) {
                log("! Ошибка выполнения запроса на архив фотографий " + user_id + ": " + error.getMessage() + ".");
                listener.error(error);
            }
        }, user_id, 0, 1);
    }

    public void getUserProfilePhotos(final GetUserProfilePhotosListener listener, final long user_id, int offset, int limit) {
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/getUserProfilePhotos?user_id=" + user_id + "&offset=" + offset + "&limit=" + limit;
        log(". Sending request: " + url);
        incrementApiCounter();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            UserProfilePhotos userProfilePhotos = new UserProfilePhotos(result);
                            listener.gotPhotos(userProfilePhotos);
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void getFile(final GetFileListener listener, String file_id) {
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/getFile?file_id=" + file_id;
        log(". Sending request: " + url);
        incrementApiCounter();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            File file = new File(result);
                            listener.gotFile(file);
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void sendDocument(final SendMessageListener listener, final long chat_id, final String text, final java.io.File f) {
        try {
            final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendDocument";
            log(". Sending request: " + url);
            incrementApiCounter();
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("chat_id", String.valueOf(chat_id));
            params.put("caption", text);
            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    log(error.getClass().getName() + " while sending request: " + url);
                    listener.error(error);
                    error.printStackTrace();
                    incrementErrorCounter();
                }
            };
            Response.Listener<String> responseListener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        log(". Got response: " + response);
                        JSONObject jsonObject = new JSONObject(response);
                        if (!jsonObject.has("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception("No OK in response!"));
                            return;
                        }
                        if (!jsonObject.getBoolean("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception(jsonObject.optString("description", "No description")));
                            return;
                        }
                        JSONObject result = jsonObject.getJSONObject("result");
                        Message message = new Message(result);
                        listener.sentMessage(message);
                        state("Аккаунт работает");
                    } catch (Exception e) {
                        e.printStackTrace();
                        incrementErrorCounter();
                        listener.error(e);
                    }
                }
            };
            MultiPartReq mPR = new MultiPartReq(url, errorListener, responseListener, f, "document", params, headers);
            mPR.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mPR);
        } catch (Exception e) {
            if (listener != null)
                listener.error(e);
        }
    }

    public void sendDocument(final SendMessageListener listener, final long chat_id, String text, final String id) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("text", text);
            jsonObject.put("document", id);
        } catch (Exception e) {
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
        }
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendDocument";
        log(". Sending request: " + url);
        incrementApiCounter();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            log(". Got response: " + jsonObject.toString());
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            Message message = new Message(result);
                            listener.sentMessage(message);
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    public void sendAudio(final SendMessageListener listener, final long chat_id, final String text, final java.io.File f) {
        try {
            final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendAudio";
            log(". Sending request: " + url);
            incrementApiCounter();
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("chat_id", String.valueOf(chat_id));
            params.put("caption", text);
            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    log(error.getClass().getName() + " while sending request: " + url);
                    listener.error(error);
                    error.printStackTrace();
                    incrementErrorCounter();
                }
            };
            Response.Listener<String> responseListener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        log(". Got response: " + response);
                        JSONObject jsonObject = new JSONObject(response);
                        if (!jsonObject.has("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception("No OK in response!"));
                            return;
                        }
                        if (!jsonObject.getBoolean("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception(jsonObject.optString("description", "No description")));
                            return;
                        }
                        JSONObject result = jsonObject.getJSONObject("result");
                        Message message = new Message(result);
                        listener.sentMessage(message);
                        state("Аккаунт работает");
                    } catch (Exception e) {
                        e.printStackTrace();
                        incrementErrorCounter();
                        listener.error(e);
                    }
                }
            };
            MultiPartReq mPR = new MultiPartReq(url, errorListener, responseListener, f, "audio", params, headers);
            mPR.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mPR);
        } catch (Exception e) {
            if (listener != null)
                listener.error(e);
        }
    }

    public void sendAudio(final SendMessageListener listener, final long chat_id, String text, final String id) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("text", text);
            jsonObject.put("audio", id);
        } catch (Exception e) {
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
        }
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendAudio";
        log(". Sending request: " + url);
        incrementApiCounter();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            log(". Got response: " + jsonObject.toString());
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            Message message = new Message(result);
                            listener.sentMessage(message);
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    public void sendPhoto(final SendMessageListener listener, final long chat_id, final String text, final java.io.File f) {
        try {
            final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendPhoto";
            log(". Sending request: " + url);
            incrementApiCounter();
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("chat_id", String.valueOf(chat_id));
            params.put("caption", text);
            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    log(error.getClass().getName() + " while sending request: " + url);
                    listener.error(error);
                    error.printStackTrace();
                    incrementErrorCounter();
                }
            };
            Response.Listener<String> responseListener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        log(". Got response: " + response);
                        JSONObject jsonObject = new JSONObject(response);
                        if (!jsonObject.has("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception("No OK in response!"));
                            return;
                        }
                        if (!jsonObject.getBoolean("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception(jsonObject.optString("description", "No description")));
                            return;
                        }
                        JSONObject result = jsonObject.getJSONObject("result");
                        Message message = new Message(result);
                        listener.sentMessage(message);
                        state("Аккаунт работает");
                    } catch (Exception e) {
                        e.printStackTrace();
                        incrementErrorCounter();
                        listener.error(e);
                    }
                }
            };
            MultiPartReq mPR = new MultiPartReq(url, errorListener, responseListener, f, "photo", params, headers);
            mPR.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mPR);
        } catch (Exception e) {
            if (listener != null)
                listener.error(e);
        }
    }

    public void sendPhoto(final SendMessageListener listener, final long chat_id, String text, final String id) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("text", text);
            jsonObject.put("photo", id);
        } catch (Exception e) {
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
        }
        final String url = "https://api.telegram.org/bot" + getId() + ":" + getToken() + "/sendPhoto";
        log(". Sending request: " + url);
        incrementApiCounter();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            log(". Got response: " + jsonObject.toString());
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            Message message = new Message(result);
                            listener.sentMessage(message);
                            state("Аккаунт работает");
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }


    //TELEGRAPH
    public void createTelegraphAccount(final CreateTelegraphAccountListener listener, String name) {
        try {
            name = URLEncoder.encode(name, "UTF-8");
        } catch (Exception e) {
            log("! Unsopported encoding for UEREncoder");
            e.printStackTrace();
        }
        final String url = "https://api.telegra.ph/createAccount?short_name=" + name + "&author_name=" + name;
        log(". Sending request: " + url);
        incrementApiCounter();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            String token = result.getString("access_token");
                            listener.accountCreated(token);
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void createTelegraphPage(final CreateTelegraphPageListener listener, final String name, final String text) {
        final String url = "https://api.telegra.ph/createPage";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", telegraphToken);
        params.put("title", name);
        if (text.length() > 31000)
            params.put("content", "[\"" + text.substring(0, 31000).replace("\"", " ") + "\"]");
        else
            params.put("content", "[\"" + text + "\"]");
        log(". Sending request: " + url);
        log(". # access_token=" + telegraphToken);
        log(". # title=" + name);
        log(". # content=text[" + text.length() + "]");
        log(text);
        incrementApiCounter();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            log(". Got response: " + response);
                            if (!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if (!jsonObject.getBoolean("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            String url = result.getString("url");
                            listener.pageCreated(url);
                        } catch (Exception e) {
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    @Override
    public boolean remove() {
        return getFileStorage().delete();
    }

    public String log(String string) {
        return applicationManager.log(string);
    }

    public String state(String string) {
        Log.d("TgAccountCore", string);
        return string;
    }

    public String getScreenName() {
        return userName;
    }

    public void setScreenName(String screenName) {
        userName = screenName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        getStorage().put("tgtoken", token).commit();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        getStorage().put("tgid", id).commit();
    }

    public interface SendMessageListener {
        void sentMessage(Message message);

        void error(Throwable error);
    }

    public interface GetMeListener {
        void gotUser(User user);

        void error(Throwable error);
    }

    public interface GetUpdatesListener {
        void gotUpdates(ArrayList<Update> updates);

        void error(Throwable error);
    }

    public interface GetUserPhotoListener {
        void gotPhoto(String url);

        void error(Throwable error);
    }

    public interface GetUserProfilePhotosListener {
        void gotPhotos(UserProfilePhotos photos);

        void error(Throwable error);
    }

    public interface GetFileListener {
        void gotFile(File file);

        void error(Throwable error);
    }

    public interface CreateTelegraphAccountListener {
        void accountCreated(String token);

        void error(Throwable error);
    }

    public interface CreateTelegraphPageListener {
        void pageCreated(String link);

        void error(Throwable error);
    }
}