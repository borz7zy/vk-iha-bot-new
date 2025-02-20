package com.fsoft.vktest.Modules;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;

/*
 * Класс для слежения за порядком в программе. Защита от модицикации программы, защита от взлома
 * Created by Dr. Failov on 24.03.2015.
 */

public class SecurityProvider extends CommandModule{
    public SecurityProvider(ApplicationManager applicationManager) {
        super(applicationManager);
    }
}