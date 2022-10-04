package com.example.easyteamupfrontend;

import android.content.Context;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserSession {
    public static String id;
    public static String age;
    public static String bio;
    public static String image;
    public static String BASE_URL = "http://54.193.68.101/";
    public static Event event;
    public static EditText address_edit;
    public static ArrayList<Event> joined_events = new ArrayList<Event>();
    public static List<Map<String, String>> determinedTimes;
    public static Context currentContext;
    public static Fragment currentFragment;

    public static boolean joined(Event event){
        for(Event e: joined_events){
            if(e.getName().equals(event.getName())){
                return true;
            }
        }
        return false;
    }

    public static String determinedTime(Event event){
        for(Map<String, String> m: determinedTimes){
            if(m.get("eventName").equals(event.getName())){
                return m.get("startTime");
            }
        }
        return event.getTimeSlots().toArray(new String[0])[0];
    }
}
