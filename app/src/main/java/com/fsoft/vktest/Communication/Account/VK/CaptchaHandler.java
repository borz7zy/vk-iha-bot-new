package com.fsoft.vktest.Communication.Account.VK;

/**
 * ��� ����� ����� ��������� ��� ����, ��� ��� ���������
 * ����� ��� ����� ������ � ����� � ��������.
 * Created by Dr. Failov on 23.02.2017.
 */
public interface CaptchaHandler {
    void handleCaptcha(VkAccountCore vkAccountCore, String sid, String img);
}
