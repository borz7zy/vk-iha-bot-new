package com.fsoft.vktest.ViewsLayer;

import com.fsoft.vktest.Utils.UserList;

/**
 * список игнорируемых лалок)
 * Created by Dr. Failov on 01.01.2015.
 */
public class TeacherListFragment extends AllowListFragment {
    @Override
    String getFragmentName() {
        return "Учителя";
    }

    @Override
    UserList getUserList() {
        return applicationManager.brain.teachId;
    }

    @Override
    String getListDescription() {
        return "Эта категория пользователей имеет право написать боту \"Начать обучение\" и отвечая на накопившиеся неизвестные, пополнять базу.";
    }
}
