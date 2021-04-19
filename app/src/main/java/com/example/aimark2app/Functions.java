package com.example.aimark2app;

import java.util.Calendar;

public class Functions {

    public static String greetings(){
        String s = "";
        Calendar c = Calendar.getInstance();
        int time = c.get(Calendar.HOUR_OF_DAY);

        if (time >= 0 && time < 12){
            s = "Good Morning sir! how can I help you today?";
        } else if (time >= 12 && time < 16){
            s = "Good Afternoon sir";
        } else if (time >= 16 && time < 22){
            s = "Good Evening sir";
        }
        else if (time >= 22 && time < 24){
            s = "Hello sir, you need to take some rest... its getting late!";
        }
        return s;
    }








}
