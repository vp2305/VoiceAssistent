package com.example.aimark2app;

import java.util.Arrays;
import java.util.List;

public class ProbabilityPick {
    public static int computeProbability(String userInput){
        // Returns 0 if task, 1 if question, 2 if conversation
        // go through the list of array each time add to the result and then % string length
        String temp = userInput;
        String[] words = temp.split(" ");

        List<String> taskList = Arrays.asList("play", "do", "can", "you", "search","set", "alarm", "search", "open", "reminder", "title", "title", "description", "description", "open", "mail", "gmail", "google", "what", "notes",
                "know", "song", "music", "about", "about", "album", "like", "where", "my", "timer", "for");
        List<String> questionList = Arrays.asList("give me", "may i", "what","is", "how", "who", "would", "doing", "information", "where", "created", "can", "i", "you", "invented", "what's",
                "how is", "weather", "today", "day", "up", "about", "current", "time", "date", "right now", "tell me", "how");
        List<String> conversationList = Arrays.asList("i'm", "i am", "are", "you", "what", "hey", "hi", "name", "real", "makes", "thank you", "sorry", "there", "excuse me", "do", "good evening", "good morning", "good night",
                "birthday", "birthday", "is", "think", "does", "that", "what's", "up", "tell", "me", "about", "yourself", "who", "created", "you",
                "never mind", "birthdate", "birth date", "birth date", "who is your", "who is your" , "i", "can't", "sleep");

        double probTask = 0;
        double probQues = 0;
        double probConvo = 0;

        for (String w : taskList){
            if (userInput.contains(w))
                probTask++;
        }

        for (String w : questionList){
            if (userInput.contains(w))
                probQues++;
        }

        for (String w : conversationList){
            if (userInput.contains(w))
                probConvo++;
        }

        probTask = probTask / words.length * 100;
        probQues = probQues / words.length * 100;
        probConvo = probConvo / words.length * 100;

        if (probTask > probQues && probTask > probConvo){
            return 0;
        } else if (probQues > probTask && probQues > probConvo) {
            return 1;
        } else if (probConvo > probTask && probConvo > probQues){
            return 2;
        } else {
            return 2;
        }
    }
}
