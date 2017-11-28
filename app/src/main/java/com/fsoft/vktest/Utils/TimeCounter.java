package com.fsoft.vktest.Utils;

import java.util.*;

/**
 * класс для учета времени чтобы понять сколько событий sender произошло за последние ... секунд
 *
 * Добавляются события, а потом можно считать сколько каких-то событий произошло за прошлые ... секунд
 * Created by Dr. Failov on 15.09.2014.
 */
public class TimeCounter {
    final Object sync = new Object();
    HashMap<Long, ArrayList<Long>> db = new HashMap<>();
    ArrayList<SimpleEntry<Long, Long>> database = new ArrayList<>(); //id -  time
    long oldThreshold = 2*60*60*1000; //24 hour

    public TimeCounter() {
    }
    public TimeCounter(long oldThreshold) {
        this.oldThreshold = oldThreshold;
    }

    public void add(Long senderId){
        database.add(new SimpleEntry<>(senderId, System.currentTimeMillis()));
        clearOld();
    }
    public int countLastSec(Long senderId, long sec){
        int cnt = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < database.size(); i++) {
            SimpleEntry<Long, Long> entry = database.get(i);
            if(entry != null) {
                long userId = entry.getKey();
                if (userId == senderId) {
                    long time = entry.getValue();
                    long dif = now - time;
                    if (dif < sec * 1000L)
                        cnt++;
                }
            }
        }
        return cnt;
    }
    public int countTotalLastSec(long sec){
        /*Подсчёт общего количество событий за указаное время
        * */

        int cnt = 0;
        for (int i = 0; i < database.size(); i++) {
            SimpleEntry<Long, Long> entry = database.get(i);
            if(entry != null) {
                long now = System.currentTimeMillis();
                long time = entry.getValue();
                long dif = now - time;
                if (dif < sec * 1000L)
                    cnt++;
            }
        }
        return cnt;
    }

    private void clearOld(){
        for (int i = 0; i < database.size(); i++) {
            SimpleEntry<Long, Long> entry = database.get(i);
            if(entry != null) {
                long now = System.currentTimeMillis();
                long time = entry.getValue();
                long dif = now - time;
                if (dif > oldThreshold) {
                    database.remove(entry);
                    i--;
                }
            }
        }
//        synchronized (sync) {
//            Set<Map.Entry<Long, ArrayList<Long>>> entries = db.entrySet();
//            long now = System.currentTimeMillis();
//            long oldThreshold = this.oldThreshold; //24hours
//            Iterator<Map.Entry<Long, ArrayList<Long>>> iterator = entries.iterator();
//            while (iterator.hasNext()) {
//                try {
//                    Map.Entry<Long, ArrayList<Long>> entry = iterator.next();
//                    long senderId = entry.getKey();
//                    ArrayList<Long> values = entry.getValue();
//                    for (int i = 0; i < values.size(); i++) {
//                        long value = values.get(i);
//                        long old = now - value;
//                        if (old > oldThreshold) {
//                            values.remove(value);
//                        }
//                    }
//                    if (values.size() == 0)
//                        db.remove(senderId);
//                }
//                catch (Exception e){/*don't worry*/}
//            }
//        }
    }
}