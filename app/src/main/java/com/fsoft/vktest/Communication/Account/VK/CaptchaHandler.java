package com.fsoft.vktest.Communication.Account.VK;

/**
 * Эта хуйня будет описывать тот факт, что для обработки
 * капчи нам нужны данные о капче и аккаунте.
 * Created by Dr. Failov on 23.02.2017.
 */
public interface CaptchaHandler {
    void handleCaptcha(VkAccountCore vkAccountCore, String sid, String img);
}
