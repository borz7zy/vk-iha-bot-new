package com.fsoft.vktest.Modules;

import android.content.*;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Utils.F;

/**
 * проверка покупки донаткии в гугл плей
 * Created by Dr. Failov on 25.03.2015.
 */

//public class DonationPurchaseChecker implements Command {
//    private static final int ERROR_INVALID_PACKAGE_NAME = 1;
//    private static final int ERROR_NON_MATCHING_UID = 2;
//    private static final int ERROR_NOT_MARKET_MANAGED = 3;
//    private static final int ERROR_CHECK_IN_PROGRESS = 4;
//    private static final int ERROR_INVALID_PUBLIC_KEY = 5;
//    private static final int ERROR_MISSING_PERMISSION = 6;
//    private static final int ERROR_BROKEN_RESPONSE = 11;
//    private static final int ERROR_EXCEPTION_CHECKING = 12;
//    private static final int DONT_ALLOW_RETRY = 10; //нет маркета или не получилось подключиться к гапсам
//
//
//    private static final int LICENSED_OLD_KEY = 9;
//    private static final int NOT_LICENSED = 8;
//    private static final int LICENSED = 7;
//
//
//    private ApplicationManager applicationManager = null;
//    DonationBroadcastReceiver donationBroadcastReceiver = null;
//
//    public DonationPurchaseChecker(ApplicationManager applicationManager) {
//        this.applicationManager = applicationManager;
//    }
//    public void load(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                beginCheck();
//            }
//        }).start();
//    }
//    @Override public String process(String input, Long senderId) {
//        //todo: remember | get purchase
//        if(input.equals("get purchase"))
//            return applicationManager.securityProvider.getDonationPurchased()?"Донатка куплена.":"Донатка не куплена.";
//        return "";
//    }
//    @Override public String getHelp() {
//        return "";
//    }
//
//    private void beginCheck(){
//        try {
//            applicationManager.getContext().showWaitingDialog("Проверка лицензии...", "Инициализация...");
//            Context context = applicationManager.getContext();
//
//            //create listener
//            donationBroadcastReceiver = new DonationBroadcastReceiver();
//            IntentFilter intentFilter = new IntentFilter("IHA_BOT_DONATION_END_CHECK");
//            context.registerReceiver(donationBroadcastReceiver, intentFilter);
//
//            //run service
//            log(". running service...");
//            Intent i = new Intent();
//            i.setComponent(new ComponentName("com.fsoft.ihabotdonate", "com.fsoft.ihabotdonate.CheckDonationService"));
//            ComponentName componentName = context.startService(i);
//
//            applicationManager.getContext().setWaitingDialogMessage("Подключение...");
//            if(componentName == null){
//                F.sleep(500);
//                applicationManager.getContext().messageBox(log("Ошибка проверки подлинности. Ваша донатка устарела и не поддерживает новый алгоритм проверки. \nОбновите донатку и перезапустите приложение."));
//                applicationManager.getContext().hideWaitingDialog();
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            log("! error checking purchase: " + e.toString());
//            applicationManager.getContext().messageBox(log("Ошибка проверки лицензии. Попробуйте обновить модуль донатки до актуальной версии. "));
//            applicationManager.getContext().hideWaitingDialog();
//            pirated();
//        }
//    }
//    private void processResponse(int code){
//        {
//            applicationManager.getContext().unregisterReceiver(donationBroadcastReceiver);
//            log(". received response: " + code);
//            applicationManager.getContext().hideWaitingDialog();
//            if(code == LICENSED) {
//                licensed();
//                return;
//            }
//            if(code == NOT_LICENSED){
//                applicationManager.getContext().messageBox(log("Ваша донатка не прошла проверку на подлинность. Подлинной считается только та донатка, которая куплена в Google Play."));
//                pirated();
//                return;
//            }
//            String description = "Неизвестная ошибка.";
//            if (code == DONT_ALLOW_RETRY)
//                description = "Не удалось проверить подлинность донатки. Проверьте корректность работы на Вашем устройстве сервисов Google Play и работу Вашего Интернет-соединения.";
//            if (code == LICENSED_OLD_KEY)
//                description = "Проверка подлинности не пройдена. Приложение устарело или модифицировано.";
//            if (code == ERROR_NOT_MARKET_MANAGED)
//                description = "Приложение отсутствует в Play Market, поэтому его лицензия не может быть проверена.";
//            if (code == ERROR_CHECK_IN_PROGRESS)
//                description = "Некорректный CheckIn. Проверка не может проведена.";
//            if (code == ERROR_INVALID_PACKAGE_NAME)
//                description = "Ошибка - неправильно указано имя пакета.";
//            if (code == ERROR_INVALID_PUBLIC_KEY)
//                description = "Ошибка ключа шифрования.";
//            if (code == ERROR_MISSING_PERMISSION)
//                description = "Ошибка проверки подлинности: недостаточно полномочий.";
//            if (code == ERROR_NON_MATCHING_UID)
//                description = "Ошибка проверки подлинности: не совпадают UID";
//            if (code == ERROR_EXCEPTION_CHECKING)
//                description = "Во время отправки запроса на проверку подлинности донатки произошла ошибка.";
//            if (code == ERROR_BROKEN_RESPONSE)
//                description = "Ошибка: модуль проверки подлинности работает некорректно.";
//            applicationManager.getContext().messageBox(log(description + " ("+code+")"));
//        }
//    }
//    private void licensed(){
//        applicationManager.securityProvider.setDonationPurchased(true);
//    }
//    private void pirated(){
//        applicationManager.securityProvider.setDonationPurchased(false);
//    }
//    private String log(String text){
//        return ApplicationManager.log(text);
//    }
//
//    private class DonationBroadcastReceiver extends BroadcastReceiver{
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            try{
//                int code = intent.getIntExtra("code", ERROR_BROKEN_RESPONSE);
//                log(". Received licensing state: " + code);
//                processResponse(code);
//            }
//            catch (Exception e){
//                e.printStackTrace();
//                log("! Error processing broadcast: " + e.toString());
//                processResponse(ERROR_BROKEN_RESPONSE);
//            }
//        }
//    }
//}
