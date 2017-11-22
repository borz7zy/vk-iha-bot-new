package com.fsoft.vktest.ViewsLayer;

import com.fsoft.vktest.Utils.UserList;

/**
 * список игнорируемых лалок)
 * Created by Dr. Failov on 01.01.2015.
 */
public class IgnorListFragment extends AllowListFragment {
    @Override
    String getFragmentName() {
        return "Игнорируемые пользователи";
    }

    @Override
    UserList getUserList() {
        return applicationManager.brain.ignorId;
    }

    @Override
    String getListDescription() {
        return "На сообщения этой категории пользователей программа не будет отвечать никогда. Исключения составляют только владелец программы и доверенные.";
    }
}
