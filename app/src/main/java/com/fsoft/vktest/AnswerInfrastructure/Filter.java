package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.HttpServer;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.ResourceFileReader;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.Parameters;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * ���� ����� ��������� ����� ����, ����� �� �� ������ ������ ������������.
 * Created by Dr. Failov on 30.03.2017.
 */
public class Filter implements Command{
    private HashMap<Long, Integer> warnings = new HashMap<>();
    private ArrayList<String> fuckingWords = null;
    private String allowedSymbols = null;
    private String[] allowedWords = {
            "vk.com",
            "com.fsoft",
            "perm"
    };
    private String securityReport = "";
    private FileStorage storage = new FileStorage("filter");
    ResourceFileReader blacklistResourceFileReader = new ResourceFileReader(applicationManager.activity.getResources(), R.raw.blacklist, name);

    public String processMessage(String in, long sender){
        if(in != null && isFilterOn() && !isAllowed(sender) && sender != HttpServer.USER_ID && !teachId.contains(sender)) { //���� �� �����������
            String out = in;
            out = passOnlyAllowedSymbols(out);
            out = out.replace(".", ". ");
            out = out.replace("&#", " ");
            out = out.replace(". . . ", "...");
            out = out.replace("vk. com", "vk.com");
            if(!isAllowedSymbol(out, true)){
                //log("! ������� ������: �������� ���������: " + out);
                applicationManager.messageBox("��������� ��� " + sender + " ("+applicationManager.vkCommunicator.getUserName(sender)+") ������.\n" +
                        "--------------\n" +
                        out + "\n" +
                        "--------------\n" +
                        securityReport);
                String warningMessage = Parameters.get("security_warning_message","\n������� ������: ���� ��������� �����������. \n" +
                        "���� ��� �� ���, �������� ����������� ������������.\n" +
                        "�� ��������� ��������������:", "����� ������� ������� ������������ ���� � ����� �� ��� ��������� ��� �������� ������� �����");
                if(warnings.containsKey(sender)){
                    int currentWarnings = warnings.get(sender);
                    currentWarnings ++;
                    warnings.put(sender, currentWarnings);
                    if(currentWarnings >= Parameters.get("security_warning_count", 3, "���������� �������������� � ������� ������� ���� �� ������� ��������������� ����.")){
                        String result = applicationManager.processCommands("ignor add " + sender + " �������������� ���������", applicationManager.getUserID());
                        out = Parameters.get("security_banned_message", "���� �������� �������������: RESULT",
                                "���������, ������� ������� ������������ ����� �� ����� ������������ �� ������� ������� ����.").replace("RESULT", result);
                    }
                    else {
                        out = warningMessage + currentWarnings + ".";
                    }
                }
                else {
                    warnings.put(sender, 1);
                    out = warningMessage + "1.";
                }
            }
            out = out.trim();
            return out;
        }
        return in;
    }
    public boolean isAllowedSymbol(String out, boolean deep){
        String tmp = prepareToFilter(out);
        loadWords();
        securityReport = "";
        boolean warning = false;
        for (int i = 0; i < fuckingWords.size(); i++) {
            if(tmp.contains(fuckingWords.get(i))) {
                securityReport += log("! ������� ������: ��������� �������������� ��������: " + fuckingWords.get(i)) + "\n";
                warning = true;
            }
        }
        if(!warning)
            securityReport = ". ����� �� ����������.";
        return !warning;
    }
    public @Override String process(String input, Long senderId) {
        CommandParser commandParser = new CommandParser(input);
        switch (commandParser.getWord()) {
            case "status":
                return "������ ������������� ���������� �������: " + isFilterOn() + "\n"+
                        "�������� ������� ������: "+(fuckingWords == null?"��� �� ���������":fuckingWords.size())+"\n"+
                        "����������� ��������: "+(allowedSymbols == null?"��� �� ���������":allowedSymbols.length())+"\n"+
                        "������������� �������� ��������������: "+ warnings.size() +"\n";
            case "warning":
                switch (commandParser.getWord()){
                    case "get":
                        String result = "������� ��������������:\n";
                        Iterator<Map.Entry<Long, Integer>> iterator = warnings.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Long, Integer> cur = iterator.next();
                            result += "- ������������ vk.com/id" + cur.getKey() + " ������� " + cur.getValue() + " ��������������.\n";
                        }
                        return result;
                    case "reset": {
                        Long id = applicationManager.getUserID(commandParser.getWord());
                        return "������� ������� ��� ������������ " + id + " : " + warnings.remove(id);
                    }
                    case "set": {
                        Long id = applicationManager.getUserID(commandParser.getWord());
                        int num = commandParser.getInt();
                        warnings.put(id, num);
                        return "������� ��� ������������ " + id + " : " + num;
                    }
                }
            case "enablefilter":
                boolean newState = commandParser.getBoolean();
                boolean oldState = isFilterOn();
                storage.put("enableFilter", newState);
                storage.commit();
                return "������ ������������� ����������.\n����: " + oldState + "\n�����: " + newState;
            case "addblacklistword":
                String word = commandParser.getText();
                word = (word).toLowerCase().replace("|", "");
                word = replaceTheSameSymbols(word);
                fuckingWords.add(word);
                return "��������� ����� � ������ ������ ����: "+word+" \n" + save();
        }
        return "";
    }
    public @Override String getHelp() {
        return "[ �������� �������� �������� �������������� ��� ������������ ]\n" +
                "---| botcmd warning reset <id ������������>\n\n"+
                "[ �������� �������� �������� �������������� ]\n" +
                "---| botcmd warning get\n\n"+
                "[ ������ �������� �������� �������������� ��� ������������ ]\n" +
                "---| botcmd warning set <id ������������> <����� �������� ��������>\n\n"+
                "[ �������� ��� ��������� ������ ������������� ���������� ]\n" +
                "[ ��� ��� ������, ������� ����� \"���� ��������� �����������.\" ]\n" +
                "[ ���� ������ ��������� �� �������������, �.�. ��� ���� ��� �� ������� ����� ������������� �� �������� �������������� ��������� ]\n" +
                "---| botcmd enablefilter <on/off>\n\n"+
                "[ �������� ����� � ������ ����������� ���� ]\n" +
                "---| botcmd addblacklistword <|����� ������� ��������|>\n\n";
    }

    private boolean isFilterOn(){
        return storage.getBoolean("enableFilter", true);
    }
    private String prepareToFilter(String in){
        String tmp = in.toLowerCase();
        for (int i = 0; i < allowedWords.length; i++)
            tmp = tmp.replace(allowedWords[i], "");
        tmp = replaceTheSameSymbols(tmp);
        //��������� ������ ����������� �������� � ��� ��������� ������� ���
        String allowed = "qwertyuiopasdfghjklzxcvbnm�����������������������������������1234567890";
        for (int i = 0; i < tmp.length(); i++) {
            char c = tmp.charAt(i);
            //��������� ���� �� ���� ������ � ������ �����������
            boolean isAllowed = false;
            for (int j = 0; j < allowed.length(); j++) {
                char ca = allowed.charAt(j);
                if(c == ca)
                    isAllowed = true;
            }
            if(!isAllowed)
                tmp = tmp.replace(c, ' ');
        }
        //�������� �������
        tmp = tmp.replace(" ", "");
        return tmp;
    }
    private String replaceTheSameSymbols(String in){
        String out = in;
        out = out.replace("�", "i");
        out = out.replace("�", "c");
        out = out.replace("�", "y");
        out = out.replace("�", "k");
        out = out.replace("�", "e");
        out = out.replace("�", "g");
        out = out.replace("�", "sh");
        out = out.replace("�", "sch");
        out = out.replace("�", "");
        out = out.replace("�", "f");
        out = out.replace("�", "y");
        out = out.replace("�", "v");
        out = out.replace("�", "a");
        out = out.replace("�", "p");
        out = out.replace("�", "l");
        out = out.replace("�", "d");
        out = out.replace("�", "z");
        out = out.replace("�", "e");
        out = out.replace("�", "ya");
        out = out.replace("�", "ch");
        out = out.replace("�", "t");
        out = out.replace("�", "i");
        out = out.replace("�", "y");

        out = out.replace("�", "y");
        out = out.replace("�", "k");
        out = out.replace("�", "e");
        out = out.replace("�", "h");
        out = out.replace("�", "3");
        out = out.replace("�", "x");
        out = out.replace("�", "v");
        out = out.replace("�", "b");
        out = out.replace("�", "a");
        out = out.replace("�", "r");
        out = out.replace("�", "o");
        out = out.replace("�", "c");
        out = out.replace("�", "m");
        out = out.replace("�", "n");
        out = out.replace("�", "t");
        out = out.replace("�", "i");
        //out = out.replace("�", "r");
        return out;
    }
    private void loadWords(){
        if(fuckingWords == null){
            String fileData = blacklistResourceFileReader.readFile();
            String[] words = fileData.split("\\\n");
            fuckingWords = new ArrayList<>();
            for (int i = 0; i < words.length; i++) {
                //words[i] = (words[i]).toLowerCase().replace("|", "");
                //words[i] = replaceTheSameSymbols(words[i]);
                fuckingWords.add(prepareToFilter(words[i]));
            }
            log(". ������ �������: ��������� " + fuckingWords.size() + " ��������.");
        }
    }
    private String save(){
        String result = "";
        if(fuckingWords.size() > 1){
            result += log(". ���������� ������ ������� � " + blacklistResourceFileReader.getFilePath() + "...\n");
            try {
                FileWriter fileWriter = new FileWriter(blacklistResourceFileReader.getFile());
                for (int i = 0; i < fuckingWords.size(); i++) {
                    fileWriter.append("|");
                    fileWriter.append(fuckingWords.get(i));
                    fileWriter.append("|");
                    if(i < fuckingWords.size() -1)
                        fileWriter.append("\n");
                }
                fileWriter.close();
                result += log(". ������ ������� ��������� " + fuckingWords.size() + " ����.\n");
            }
            catch (Exception e){
                result += log("! ��������� ���� ������ ������� � " + blacklistResourceFileReader.getFilePath() + " �� �������, � ��� ������: "+e.toString()+"\n");
            }
        }
        else
            result += log("! ���� ������ ������� �����. �� ���������.\n");
        return result;
    }
    private void loadSymbols(){
        if(allowedSymbols == null){
            ResourceFileReader resourceFileReader = new ResourceFileReader(applicationManager.activity.getResources(), R.raw.allowed_symbols, name);
            allowedSymbols = resourceFileReader.readFile();
            log(". ����������� �������: ��������� " + allowedSymbols.length() + " ��������.");
        }
    }
    private boolean isAllowedByServer(String text){
        if(text == null)
            return true;
        String encodedText = encodeURIcomponent(text).toUpperCase();
        if(encodedText.replace("%20", "").equals(""))
            return true;
        String address = "http://filyus.ru/verbal.hasBadLinks?q=" + encodedText;
        log("ADDRESS = " + address);
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpGet = new HttpGet(address);
            HttpResponse response = httpclient.execute(httpGet);
            String result = EntityUtils.toString(response.getEntity());
            log("RESULT = " + result);
            JSONObject jsonObject = new JSONObject(result);
            int bad = jsonObject.getInt("response");
            return !(bad == 1);
        }
        catch (Throwable e){
            e.printStackTrace();
            log("! Error while filtering: " + e.toString());
        }
        return true;
    }
    private String encodeURIcomponent(String s){
        /** Converts a string into something you can safely insert into a URL. */
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                if(ch == ' ') {
                    o.append('%');
                    o.append(toHex(ch / 16));
                    o.append(toHex(ch % 16));
                }
            }
            else o.append(ch);
        }
        return o.toString();
    }
    private char toHex(int ch) {
        return (char)(ch < 10 ? '0' + ch : 'A' + ch - 10);
    }
    private boolean isUnsafe(char ch) {
        return " qwertyuiopasdfghjklzxcvbnm1234567890QWERTYUIOPASDFGHJKLZXCVBNM".indexOf(ch) < 0;
    }
}
