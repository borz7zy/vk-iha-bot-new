package com.fsoft.vktest.Communication.Account.VK;

/**
 * Это окно будет заниматься
 * - Загрузкой изображения капчи
 * - Отображением окна с вводом капчи
 * - заданием результата на аккаунте
 * - повторным запуском аккаунта
 * Created by Dr. Failov on 23.02.2017.
 */
public class CapthcaWindow implements CaptchaHandler{
    //не вижу особого смысла отображать два окна одновременно. Поэтому будет этот флаг и
    // блокировать показ новых окон пока не обработаем старые
    private static boolean windowShown = false;

    //todo маке captcha handler
    public CapthcaWindow() {
    }

    @Override
    public void handleCaptcha(VkAccountCore vkAccountCore, String sid, String img) {

    }
}
