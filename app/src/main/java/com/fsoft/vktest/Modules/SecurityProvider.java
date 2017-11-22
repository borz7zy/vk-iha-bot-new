package com.fsoft.vktest.Modules;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.ViewsLayer.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для слежения за порядком в программе. Защита от модицикации программы, защита от взлома
 * Created by Dr. Failov on 24.03.2015.
 */

public class SecurityProvider implements Command {
    public interface OnDonationStateChangedListener {
        void donationStateChanged(Boolean donated);
    }

    private ApplicationManager applicationManager = null;
    private Boolean donationInstalled = null;
    private Boolean donationValidated = null;
    private Boolean donationPurchased = null;
    public Boolean forcedDonation = null;
    private DonationValidator donationValidator = null;
    private InterventionChecker interventionChecker = null;
    private DonationPurchaseChecker donationPurchaseChecker = null;
    private ArrayList<OnDonationStateChangedListener> donationStateChangedListeners = new ArrayList<>();
    private ArrayList<Command> commands = new ArrayList<>();

    public SecurityProvider(ApplicationManager applicationManager){
        this.applicationManager = applicationManager;
        donationValidator = new DonationValidator(applicationManager);
        donationPurchaseChecker = new DonationPurchaseChecker(applicationManager);
        interventionChecker = new InterventionChecker(applicationManager);

        commands.add(new SecretRoom());
    }
    public void addDonationStateChangedListener(OnDonationStateChangedListener listener){
        donationStateChangedListeners.add(listener);
    }
    public boolean deleteDonationStateChangedListener(OnDonationStateChangedListener listener){
        return donationStateChangedListeners.remove(listener);
    }
    public boolean getDonationInstalled(){
        if(donationInstalled == null)
            return false;
        return donationInstalled;
    }
    public boolean getDonationValidated(){
        if(donationValidated == null)
            return true;
        return donationValidated;
    }
    public boolean getDonationPurchased(){
        if(donationPurchased == null)
            return false;
        return donationPurchased;
    }
    public void setDonationInstalled(Boolean donationInstalled) {
        log("setDonationInstalled = " + donationInstalled);
        this.donationInstalled = donationInstalled;
        donationStateChanged();
        if(donationPurchased == null && donationInstalled != null && donationInstalled)
            donationPurchaseChecker.load();
    }
    public void setDonationValidated(Boolean donationValidated) {
        log("setDonationValidated = " + donationValidated);
        this.donationValidated = donationValidated;
        donationStateChanged();
    }
    public void setDonationPurchased(Boolean donationPurchased) {
        log("setDonationPurchased = " + donationPurchased);
        this.donationPurchased = donationPurchased;
        donationStateChanged();
        if(donationValidated== null && donationPurchased != null && donationInstalled && donationPurchased)
            donationValidator.load();
    }
    public boolean isDonated(){
        if(forcedDonation != null)
            return forcedDonation;
        return getDonationInstalled() && getDonationPurchased() && getDonationValidated();
    }
    public boolean allowStart(){
        if(interventionChecker.checkIntervention()) {
            //applicationManager.activity.messageBoxPermanent(log("! Обнаружены модификации программы. Дальнейший запуск невозможен."));
            return false;
        }
        //log(". Модификаций не обнаружено.");
        return true;
    }
    public void load(){
        if(forcedDonation == null)
            startDonationInstalledChecking();
    }
    public void close(){
        donationValidator.close();
    }
    @Override public String process(String input, Long senderId) {
        String result = "";
        //todo: remember | isdonated
        if(input.equals("isdonated"))
            result += ("Доначен: " + isDonated());
        for (Command command:commands)
            result += command.processCommand(input, senderId);
        return result;
    }
    @Override public String getHelp() {
        String result = "";
        for (Command command:commands)
            result += command.getHelp();
        return result;
    }

    private void donationStateChanged(){
        boolean donated = isDonated();
        for(OnDonationStateChangedListener listener:donationStateChangedListeners)
            listener.donationStateChanged(donated);
    }
    private boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        boolean available = false;
        try {
            pm.getPackageInfo(packageName, 0);
            available = true;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return available;
    }
    private MainActivity getContext(){
        if(applicationManager != null)
            return applicationManager.getContext();
        return null;
    }
    private String log(String text){
        return ApplicationManager.log(text);
    }
    private void startDonationInstalledChecking(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //applicationManager.sleep(2000);
                if(getContext() == null) {
                    setDonationInstalled(false);
                    return;
                }
                String donatePackageName = "com.fsoft.ihabotdonate";
                String intentAction = "CHECK_VK_IHA_BOT_DONATION";
                String correctIntentName = "com.fsoft.ihabotdonate.Donation";
                if (isPackageInstalled(getContext(), donatePackageName)) {
                    List<ResolveInfo> pkgAppsList = getContext().getPackageManager().queryIntentActivities(new Intent(intentAction), 0);
                    for (ResolveInfo resolveInfo : pkgAppsList)
                        if (resolveInfo.activityInfo.name.equals(correctIntentName)) {
                            setDonationInstalled(true);
                            return;
                        }
                }
                setDonationInstalled(false);
            }
        }).start();
    }

    private class SecretRoom implements Command{
        ArrayList<Long> loggedIn = new ArrayList<>();
        ArrayList<Command> commands = new ArrayList<>();
        Login login = new Login();

        public SecretRoom() {
            commands.add(new ForcedDonation());
            commands.add(interventionChecker);
            commands.add(donationValidator);
            commands.add(donationPurchaseChecker);
        }
        @Override  public String getHelp() {
            String result = "";
            for (Command command:commands)
                result += command.getHelp();
            return result;
        }
        @Override public String process(String input, Long senderId) {
            String result = "";
            result += login.process(input, senderId);
            if(loggedIn.contains(senderId) || senderId.equals(10299185L))
                for (Command command:commands)
                    result += command.processCommand(input, senderId);
            return result;
        }

        private class Login implements Command{
            @Override
            public String process(String input, Long senderId) {
                CommandParser commandParser = new CommandParser(input);
                //todo: remember | login drfailov yayebu228
                //todo: remember | logout
                String word = commandParser.getWord();
                if(word.equals("login")){
                    String login = commandParser.getWord();
                    String loginMd5 = applicationManager.getMD5(login);
                    String correctMd5 = "f8cc64ad4657205c0a32f8bbb32a1a67"; //drfailov
//                    log("login = " + login);
//                    log("loginMd5 = " + loginMd5);
//                    log("correctMd5 = " + correctMd5);
                    if(loginMd5.equals(correctMd5)) {
                        String pass = commandParser.getWord();
                        String passMd5 = applicationManager.getMD5(pass);
                        String correctpassMd5 = "b48331bab99ce46ea7b082c0b266500c"; //yayebu228
//                        log("login = " + login);
//                        log("loginMd5 = " + loginMd5);
//                        log("correctMd5 = " + correctMd5);
                        if(passMd5.equals(correctpassMd5)) {//manuleee
                            loggedIn.add(senderId);
                            return "Добро пожаловать, %username%!";
                        }
                    }
                }
                else if(word.equals("logout")){
                    if(loggedIn.contains(senderId)){
                        loggedIn.remove(senderId);
                        return "Сеанс завершен.";
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "";
            }
        }
        private class ForcedDonation implements Command{

            public ForcedDonation() {
                forcedDonation = getForcedDonationState();
                donationStateChanged();
            }

            @Override
            public String process(String input, Long senderId) {
                CommandParser commandParser = new CommandParser(input);
                //todo: remember | force donate
                //todo: remember | force undonate
                //todo: remember | force off
                if(commandParser.getWord().equals("force")){
                    String word = commandParser.getWord();
                    if(applicationManager.getMD5(word).equals("6c62058ab695b3d3a521fc9e92c6ee01")) {//donate
                        forcedDonation = true;
                        donationStateChanged();
                        saveForcedDonationState(forcedDonation);
                        return "Принудительный донат активирован.";
                    }
                    else if(applicationManager.getMD5(word).equals("b3441ac1f1a921f79923d6de9fd4b5bf")) {//undonate
                        forcedDonation = false;
                        donationStateChanged();
                        saveForcedDonationState(forcedDonation);
                        return "Донат принудительно деактивирован.";
                    }
                    else if(applicationManager.getMD5(word).equals("3262d48df5d75e3452f0f16b313b7808")) {//off
                        forcedDonation = null;
                        donationStateChanged();
                        saveForcedDonationState(forcedDonation);
                        return "Принудительный режим отключен.";
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "";
            }

            private Boolean getForcedDonationState(){
                SharedPreferences sharedPreferences = applicationManager.activity.getPreferences(Context.MODE_PRIVATE);
                if(!sharedPreferences.contains("forceFD"))
                    return null;
                return sharedPreferences.getBoolean("forceFD", true);
            }
            private void saveForcedDonationState(Boolean newState){
                SharedPreferences sharedPreferences = applicationManager.activity.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if(newState == null)
                    editor.remove("forceFD");
                else
                    editor.putBoolean("forceFD", newState);
                editor.commit();
            }
        }
    }
}
