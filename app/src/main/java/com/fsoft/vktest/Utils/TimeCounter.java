package com.fsoft.vktest.Utils;

import java.util.*;

/**
 * класс для учета времени чтобы понять сколько событий sender произошло за последние ... секунд
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
//        synchronized (sync) {
//            if (!db.containsKey(senderId)) {
//                db.put(senderId, new ArrayList<Long>());
//            }
//            long time = System.currentTimeMillis();
//            db.get(senderId).add(time);
//        }
        clearOld();
    }
    public int countLastSec(Long senderId, long sec){
        int cnt = 0;
        for (int i = 0; i < database.size(); i++) {
            SimpleEntry<Long, Long> entry = database.get(i);
            if(entry != null) {
                long userId = entry.getKey();
                if (userId == senderId) {
                    long now = System.currentTimeMillis();
                    long time = entry.getValue();
                    long dif = now - time;
                    if (dif < sec * 1000L)
                        cnt++;
                }
            }
        }
        return cnt;
//        synchronized (sync)
//        {
//            if (!db.containsKey(senderId)) {
//                return 0;
//            }
//            int result = 0;
//            long currentTime = System.currentTimeMillis();
//            ArrayList<Long> list = db.get(senderId);
//            for (int i = 0; i < list.size(); i++) {
//                long dif = currentTime - list.get(i);
//                if (dif < sec * 1000)
//                    result++;
//            }
//            return result;
//        }
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

//         int result = 0;
//        Long currentTime = System.currentTimeMillis();
//        synchronized (sync) {
//            Set<Map.Entry<Long, ArrayList<Long>>> entries = db.entrySet();
//            Iterator<Map.Entry<Long, ArrayList<Long>>> iterator = entries.iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<Long, ArrayList<Long>> entry = iterator.next();
//                ArrayList<Long> values = entry.getValue();
//                for (int i = 0; i < values.size(); i++) {
//                    long value = values.get(i);
//                    long dif = currentTime - value;
//                    if (dif < sec * 1000)
//                        result++;
//                }
//            }
//        }
//        return result;
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