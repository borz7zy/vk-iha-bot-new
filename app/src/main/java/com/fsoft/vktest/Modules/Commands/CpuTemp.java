package com.fsoft.vktest.Modules.Commands;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.ResourceFileReader;

import java.io.File;
import java.util.ArrayList;

/**
 * Команда должна выводить информациб о темтературе из мимтемы
 * Created by Dr. Failov on 15.02.2017.
 */
public class CpuTemp extends CommandModule {
    public CpuTemp(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public String processCommand(Message message) {
        if(message.getText().equals("cputemp")) {
            try {
                File folder = new File("sys/devices/virtual/thermal");
                if(!folder.isDirectory())
                    return "Не вижу у тебя в системе термометров.";
                File[] thermals = folder.listFiles();
                String result = "Термометры в системе: \n";
                for (int i = 0; i < thermals.length; i++) {
                    File sensor = new File(thermals[i].getPath() + File.separator + "temp");
                    String name = thermals[i].getName();
                    if(!sensor.isFile())
                        result += "Термометр " + name + " не работает.\n";
                    try {
                        String data = F.readFromFile(sensor);
                        if(data.equals(""))
                            result += "Термометр " + name + " ничего нам не сообщил.\n";
                        else
                            result += "Термометр " + name + " сообщает температуру "+data.replace("\n", "")+"°С.\n";
                    }
                    catch (Exception e){
                        result += "Термометр " + name + " не разрешает прочитать температуру.\n";
                    }
                }
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                return "Не получилось прочитать термометров: " + e.toString() + ". Твой телефон не поддерживается.";
            }
        }
        return "";
    }

    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc(
                "Получить показатели термометров телефона",
                "С помощью этой команды ты сможешь удалённо контролировать " +
                        "не перегрелся ли твой телефон.\n" +
                        "Обычно термометры показывают температуру процессора и других важных микросхем.\n" +
                        "Обрати внимание: в системе не всегда есть термометры и они не всегда работают. " +
                        "Поэтому, эта команда на некоторых телефонах может показывать неправильную температуру.\n" +
                        "Кстати, иногда бывает что темтература отображается в 1000 раз больше. Тогда просто " +
                        "подели число на 1000.",
                "botcmd cputemp"));
        return result;
    }
}
