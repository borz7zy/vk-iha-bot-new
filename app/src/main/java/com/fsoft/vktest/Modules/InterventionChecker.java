package com.fsoft.vktest.Modules;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.ResourceFileReader;

/**
 * проверка кусков программы на предмет постороннего вмешатества
 *
 * !!! Выполнять перед компиляцией в продакшн
 *      log\("
 *    заменить на
 *      //log\("
 *      
 * 
 * !!! Выполнять для отдалки
 *      //log\("
 *    заменить на
 *      log\("
 * 
 * Created by Dr. Failov on 25.03.2015.
 */

//// TODO: 13.12.2017 переписать к хуям, а лучше засунуть в SecurityProvider нахуй вложенным классом
public class InterventionChecker extends CommandModule {
    public InterventionChecker(ApplicationManager applicationManager) {
        super(applicationManager);
    }
    //    private boolean DEBUG = false;
//
//    public InterventionChecker(ApplicationManager applicationManager) {
//        super(applicationManager);
//    }
//    public boolean checkIntervention(){
//        try {
//            //log(". Проверка вмешатества...");
//            //true - вмешивались
//            //false - не вмешивались
//            return checkActivityName()
//                    || checkDevelopersList()
//                    || checkShortName()
//                    || checkGroupLink()
//                    || checkDonationLink()
//                 //   || checkIcon()
//                    || checkVisibleName()
//                    || checkAllowed()
//                    || checkIsDonatedFunction();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            //log("! Ошибка проверки вмешатества: " + e.toString());
//            return true;
//        }
//    }
//    private boolean checkActivityName(){//FP iHA bot
//        String actual = applicationManager.activity.getResources().getString(R.string.app_name).toLowerCase();
//        String expected = f()+p()+ _space()+i()+h()+a()+ _space()+b()+o()+t();
//        if(DEBUG) {
//            log(". Activity name actual = "+actual);
//            log(". Activity name expected = "+expected);
//        }
//        return !actual.equals(expected);
//    }
//    private boolean checkDevelopersList(){//Dr. Failov - Главный разработчик. Сделал...
//        String data = ApplicationManager.getDevelopersList();
//        String actual = getMD5(data);
//        String expected = "f"+d()+"40709"+c()+"6a"+c()+"76"+c()+"4"+c()+""+c()+"2627314"+d()+"420"+c()+"f17";
//        if(DEBUG) {
//            log(". md5 developers actual = " + actual);
//            log(". md5 developers expected = " + expected);
//        }
//        return !actual.equals(expected);
//    }
//    private boolean checkVisibleName(){//Dr.Failov iHA bot™ v3.10
//        String actual = ApplicationManager.getVisibleName();
//        String expected = "D"+r()+".F"+a()+"il"+o()+"v iHA b"+o()+"t";
//        if(DEBUG) {
//            log(". md5 VisibleName actual = " + actual);
//            log(". md5 VisibleName expected = " + expected);
//        }
//        return !actual.contains(expected);
//    }
//    private boolean checkShortName(){//FP iHA bot
//        String data = ApplicationManager.getShortName();
//        String actual = getMD5(data);
//        String expected = "28"+f()+"8"+e()+"843902209368"+f()+"9"+f()+""+a()+"046"+f()+"7"+a()+""+e()+"422d";
//        if(DEBUG) {
//            log(". md5 ShortName actual = " + actual);
//            log(". md5 ShortName expected = " + expected);
//        }
//        return !actual.equals(expected);
//    }
//    private boolean checkGroupLink(){//http://vk.com/ihabotclub
//        String data = ApplicationManager.getGroupLink();
//        String actual = getMD5(data);
//        String expected = "625"+a()+"2"+f()+"50"+a()+"5d"+c()+"40"+c()+""+b()+"376d"+a()+"4"+e()+"d"+b()+""+a()+""+c()+""+b()+"2"+e()+"7"+b()+"";
//        if(DEBUG) {
//            log(". md5 GroupLink actual = " + actual);
//            log(". md5 GroupLink expected = " + expected);
//        }
//        return !actual.equals(expected);
//    }
//    private boolean checkDonationLink(){//market://details?id=com.fsoft.ihabotdonate
//        String data = ApplicationManager.getFullVersionLink();
//        String actual = getMD5(data);
//        String expected = "43806"+e()+"59075"+c()+"1"+a()+"1818"+f()+"583"+e()+""+e()+""+a()+"1958"+f()+"23";
//        if(DEBUG) {
//            log(". md5 DonationLink actual = " + actual);
//            log(". md5 DonationLink expected = " + expected);
//        }
//        return !actual.equals(expected);
//    }
//    private boolean checkIcon(){//icon
//        String data = ResourceFileReader.readFromResource(R.drawable.bot, applicationManager.activity.getResources());
//        String actual = getMD5(data);
//        String expected = "899043"+d()+"6"+b()+""+d()+""+a()+"28"+f()+"8"+a()+""+d()+""+d()+""+b()+"5316703200"+f()+"71";
//        if(DEBUG) {
//            log(". md5 icon actual = " + actual);
//            log(". md5 icon expected = " + expected);
//        }
//        return !actual.equals(expected);
//    }
//    private boolean checkAllowed(){//icon
//        long id = 10299185L;
//        boolean actual = applicationManager.brain.isAllowed(id);
//        boolean expected = true;
//        if(DEBUG) {
//            log(". allowed actual = " + actual);
//            log(". allowed expected = " + expected);
//        }
//        return actual != expected;
//    }
//    private boolean checkIsDonatedFunction(){
//        Boolean forceStateBefore = applicationManager.securityProvider.forcedDonation;
//        applicationManager.securityProvider.forcedDonation = false;
//        boolean actual = applicationManager.isDonated();
//        boolean expected = false;
//        applicationManager.securityProvider.forcedDonation = forceStateBefore;
//        if(DEBUG) {
//            log(". allowed actual = " + actual);
//            log(". allowed expected = " + expected);
//        }
//        return actual != expected;
//    }
//
//    private String q(){return "q";}
//    private String w(){return "w";}
//    private String e(){return "e";}
//    private String r(){return "r";}
//    private String t(){return "t";}
//    private String y(){return "y";}
//    private String u(){return "u";}
//    private String i(){return "i";}
//    private String o(){return "o";}
//    private String p(){return "p";}
//    private String a(){return "a";}
//    private String s(){return "s";}
//    private String d(){return "d";}
//    private String f(){return "f";}
//    private String g(){return "g";}
//    private String h(){return "h";}
//    private String j(){return "j";}
//    private String k(){return "k";}
//    private String l(){return "l";}
//    private String z(){return "z";}
//    private String x(){return "x";}
//    private String c(){return "c";}
//    private String v(){return "v";}
//    private String b(){return "b";}
//    private String n(){return "n";}
//    private String m(){return "m";}
//    private String _space(){return " ";}
//    private String log(String text){
//        return ApplicationManager.log(text);
//    }
//    private String getMD5(String data){
//        return applicationManager.getMD5(data);
//    }
//    @Override  public String process(String input, Long senderId) {
//        return "";
//    }
//    @Override  public String getHelp() {
//        return "";
//    }
}
