package com.example.aimark2app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.example.aimark2app.Functions.greetings;
import static com.example.aimark2app.ProbabilityPick.computeProbability;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1;
    private SpeechRecognizer speechRecognizer;
    TextToSpeech textToSpeech;
    Button btnRecord, btnStop;
    String userResponse;
    Float speechRate = 1.8f;
    String date, month, year, endWeek;
    ListView listView;
    String[] mTitle;
    String[] mDescription;
    String[] calendarEventTitle;
    String[] calendarEventTime;
    List<String> list = new ArrayList<String>();
//    TextView textV_Listening;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);
        String r = currentYear();

        String[] year_date_month_ending_date = r.split(" ");

        date = year_date_month_ending_date[0];
        month = year_date_month_ending_date[1];
        year = year_date_month_ending_date[2];
        endWeek = year_date_month_ending_date[3];

        calendarPermission();
        viewCalendar();
        stopPressed();
        listening();
        textSpeechInitialize();

        try {
            listView = findViewById(R.id.listView);
            MyAdapter adapter = new MyAdapter(getApplicationContext(), calendarEventTitle, calendarEventTime);
            listView.setAdapter(adapter);
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), "No calendar event till Saturday 😊", Toast.LENGTH_LONG).show();
        }
    }

    class MyAdapter extends ArrayAdapter<String> {
        Context context;
        String[] rTitle;
        String[] rDescription;

        public MyAdapter(@NonNull Context context, String[] title, String[] description) {
            super(context, R.layout.row, R.id.calendarText1, title);
            this.context = context;
            this.rTitle = title;
            this.rDescription = description;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(R.layout.row, parent, false);
            ImageView image = row.findViewById(R.id.calendar_image);
            TextView myTitle = row.findViewById(R.id.calendarText1);
            TextView myDesc = row.findViewById(R.id.calendarText2);
            TextView tvTitle = findViewById(R.id.tvTitle);
            tvTitle.setText("Sammy");
            image.setImageResource(R.drawable.calendar);
            myTitle.setText(rTitle[position]);
            myDesc.setText(rDescription[position]);
            return row;
        }
    }

    private void viewCalendar() {
        int index = 0;
        int count = 0;
        List<String> calendarEvent = new ArrayList<String>();
        List<String> calendarTime = new ArrayList<String>();

        String[] cheT;
        String checkTime = "";
        String splitTime = "";

        String[] PROJECTION = new String[] {
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.TITLE
        };

        int id_index = 0, begin_index = 1, title_index = 2;
        Calendar beginTime = Calendar.getInstance(TimeZone.getDefault());
        beginTime.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(date));
        long startMillis = beginTime.getTimeInMillis();

        Calendar endTime = Calendar.getInstance(TimeZone.getDefault()); //Integer.parseInt(endWeek)
        endTime.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(endWeek), 24, 00, 00);
        long endMillis = endTime.getTimeInMillis();

        Cursor c = null;
        ContentResolver cr = getContentResolver();

        // Construct the query with the desired date range.
        Uri.parse("content://com.android.calendar/events");
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        c = cr.query(builder.build(), PROJECTION,
                null, null, null);

        while (c.moveToNext()){
            count++;
        }
        c.moveToFirst();
        if (count != 0){
            mTitle = new String[count - 1];
            mDescription = new String[count - 1];
            while(c.moveToNext()) {
                String title = null;
                long eventID = 0;
                long beginVal = 0;

                eventID = c.getLong(id_index);
//                    Log.i("Event ID", String.valueOf(eventID));
                beginVal = c.getLong(begin_index);
                title = c.getString(title_index);

//                    Log.i("Calendar", "Event:  " + title);
                calendarEvent.add(title);
                mTitle[index] = title;
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(beginVal);

                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a");
//                    Log.i("Calendar", "Date: " + formatter.format(calendar.getTime()));
                calendarTime.add(formatter.format(calendar.getTime()));
                mDescription[index] = formatter.format(calendar.getTime());
                index++;
            }

            Collections.sort(calendarTime);
            calendarEventTitle = new String[count - 1];
            calendarEventTime = new String[count - 1];

            int output_idx = 0;
            for (int i = 0; i <= calendarTime.size() - 1; i++){
                cheT = calendarTime.get(i).split(" ");
                checkTime = cheT[0];
                if (!alreadyInList(checkTime)) {
                    list.add(checkTime);
                    int input_idx = 0;
                    for (String word : mDescription) {
                        cheT = word.split(" ");
                        splitTime = cheT[0];
                        if (checkTime.equals(splitTime)){
                            calendarEventTitle[output_idx] = mTitle[input_idx];
                            calendarEventTime[output_idx] = calendarTime.get(output_idx);
//                        Log.i("ssskmkmd", mTitle[output_idx]);
                            output_idx++;
                        }
                        input_idx = input_idx + 1;
                    }
                }
            }
            list.clear();
        } if (count == 0){
            calendarEventTitle = new String[1];
            calendarEventTime = new String[1];
            Log.i("Calendar", "No Calendar Event");
            calendarEventTitle[0] = "No calendar event";
            calendarEventTime[0] = "There is no event until Saturday 😊";
        }
    }

    public boolean alreadyInList(String toTest){
        if (list.contains(toTest)){
            return true;
        } else {
            return false;
        }
    }

    public static String currentYear() {
        Date d = new Date();
        int year = 1900 + d.getYear();
        int month = d.getMonth();
        int date = d.getDate();
        int day = d.getDay();
        int ending_month = 0;
        if (day != 6){
            ending_month = date + (6 - day);
        }
        else {
            ending_month = date + 7;
        }
        String result = date + " " + month + " " + year + " " + ending_month;
        return result;
    }

    // Trigger button to listen
    public void buttonPressed(){
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermission();
                } else {
                    btnRecord.setEnabled(false);
                    startRecognition();
                }
            }
        });
    }

    public void stopPressed(){
        btnStop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (textToSpeech.isSpeaking()){
                    Log.i("Button", "pressed");
                    textToSpeech.stop();
                }
            }
        });
    }


    // Start listening
    private void startRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        speechRecognizer.startListening(intent);
    }

    // To speak and need to provide a string
    private void toSpeak(String toSpeak){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(">>>Voice Info", String.valueOf(textToSpeech.getVoice()));
        }
        try {
            textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        } catch (Exception e){
            e.printStackTrace();
            btnStop.setVisibility(View.INVISIBLE);
        }
    }

    private int getWaitTime(String toSpeak) {
        int wpm = 180;
        int word_length = 5;
        int words = toSpeak.length() / word_length;
        int words_time = ((words / wpm) * 60) * 1000;
        int waitTime =  1100 + 1000  + words_time;
        return waitTime;
    }

    // Execute functions based on user input
    public void executeTask(String userInput) {
        // open a application
        // search
        // play song
        // Set alarm
        if (userInput.contains("open")){
            if (userInput.contains("google chrome")){
                toSpeak("Opening google chrome");
                Uri uri = Uri.parse("https://google.ca/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.android.chrome");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://google.ca/")));
                }
            }

            else if (userInput.contains("youtube")){
                toSpeak("Opening youtube");
                Uri uri = Uri.parse("https://youtube.com/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.google.android.youtube");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://youtube.com/")));
                }
            }
            else if (userInput.contains("mail")) {
                toSpeak("Opening mail");
                Uri uri = Uri.parse("https://outlook.live.com/mail");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.microsoft.office.outlook");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://outlook.live.com/mail/")));
                }

            }
            else if (userInput.contains("instagram")){
                toSpeak("Opening instagram");
                Uri uri = Uri.parse("http://instagram.com/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.instagram.android");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/")));
                }
            }
            else if (userInput.contains("snapchat")){
                toSpeak("Opening snapchat");
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                intent.setPackage("com.snapchat.android");
                startActivity(Intent.createChooser(intent, "Open Snapchat"));
            }
            else if (userInput.contains("messenger")){
                toSpeak("Opening messenger");
                //"fb://messaging/" + "vaibhav.patel2305"
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://messaging/" + "100003114431361"));
                startActivity(i);
            }
            else if (userInput.contains("whatsapp")){
                toSpeak("Opening whatsapp");
                String url = "https://api.whatsapp.com/send?phone=64";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

            }
            else if (userInput.contains("netflix")){
                toSpeak("Opening netflix");
                Uri uri = Uri.parse("https://netflix.com/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.setClassName("com.netflix.mediaclient", "com.netflix.mediaclient.ui.launch.UIWebViewActivity");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://netflix.com/")));
                }
            }

            else if (userInput.contains("spotify")) {
                //"com.spotify.music", "com.spotify.music.MainActivity"
                toSpeak("Opening spotify");
                Uri uri = Uri.parse("https://www.spotify.com/us/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.setClassName("com.spotify.music", "com.spotify.music.MainActivity");
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.spotify.com/us/")));
                }
            }

            else {
                toSpeak("Sorry but I cannot open that application!");
            }

//            else if (userInput.contains("notes")){
//                Uri uri = Uri.parse("https://www.samsung.com/us/support/owners/app/samsung-notes");
//                Intent i = new Intent(Intent.ACTION_VIEW, uri);
//
//                i.setPackage("com.saumsung.samsung-notes");
//
//                try {
//                    startActivity(i);
//                } catch (ActivityNotFoundException e) {
//                    startActivity(new Intent(Intent.ACTION_VIEW,
//                            Uri.parse("https://www.samsung.com/us/support/owners/app/samsung-notes")));
//                }
//            }
        }
        else if (userInput.contains("search") && (userInput.contains("search") || userInput.contains("about") || userInput.contains("tell me") || userInput.contains("for"))){
            String line;
            if (userInput.contains("about")){
                line = userInput.substring(userInput.indexOf("about") + 6, userInput.length());
            } else if (userInput.contains("for")){
                line = userInput.substring(userInput.indexOf("for") + 4, userInput.length());
            }
            else {
                line = userInput.substring(userInput.indexOf("search") + 7, userInput.length());
            }
            String url = "https://www.google.com/search?q=" + line;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.android.chrome");  // package of SafeBrowser App
            startActivity(intent);

        }
        else if (userInput.contains("set") || userInput.contains("start")){
            if (userInput.contains("alarm")){
                String hour;
                String minutes;
                String am_pm;
                Integer hourIn24;

                if (userInput.contains("at")){
                    userInput = userInput.substring(userInput.indexOf("at") + 3, userInput.length());
                }
                else if (userInput.contains("for")){
                    userInput = userInput.substring(userInput.indexOf("for") + 4, userInput.length());
                }
                else {
                    userInput = userInput.substring(userInput.indexOf("alarm") + 5, userInput.length());
                }
                userInput = userInput.replace(".", "");
                if (userInput.contains("am") || userInput.contains("pm") || userInput.contains("a m") || userInput.contains("p m")){
                    hour = userInput.substring(0, userInput.indexOf(":"));
                    minutes = userInput.substring(userInput.indexOf(":")+1, userInput.indexOf(":")+3);
                    am_pm = userInput.substring(userInput.indexOf(":")+4, userInput.length());
                    am_pm = am_pm.replace(" ", "");

                    if (hour.equals("12") && am_pm.equals("am")){
                        hourIn24 = Integer.parseInt(hour) - 12;
                    }
                    else if ((Integer.parseInt(hour) >= 1 || Integer.parseInt(hour) <= 11) && am_pm.equals("pm")){
                        hourIn24 = Integer.parseInt(hour) + 12;
                    }
                    else {
                        hourIn24 = Integer.parseInt(hour);
                    }

                    Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
                    intent.putExtra(AlarmClock.EXTRA_HOUR, hourIn24);
                    intent.putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(minutes));

                    if (hourIn24 <= 24 && Integer.parseInt(minutes) <= 60){
                        try {
                            startActivity(intent);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    toSpeak("Please provide appropriate time to set an alarm!");
                }
            }
            else if (userInput.contains("reminder") || userInput.contains("event")){
                // Set a reminder with a title of .... and a description of ...
                TimeZone timeZone = TimeZone.getDefault();
                if (userInput.contains("title") && userInput.contains("description")){
                    try {
                        String titleString = "";
                        String description = "";
                        if (userInput.contains("with a description of")) {
                            titleString = userInput.substring(userInput.indexOf("title of") + 9, userInput.indexOf("and with a description") - 1);
                            description = userInput.substring(userInput.indexOf("description of") + 15, userInput.length());
                        } else if (userInput.contains("description of")) {
                            titleString = userInput.substring(userInput.indexOf("title of") + 9, userInput.indexOf("and a description of") - 1);
                            description = userInput.substring(userInput.indexOf("description of") + 15, userInput.length());
                        } else {
                            titleString = userInput.substring(userInput.indexOf("title of") + 9, userInput.indexOf("and a description") - 1);
                            description = userInput.substring(userInput.indexOf("description") + 12, userInput.length());
                        }

                        Intent intent = new Intent(Intent.ACTION_INSERT);
                        intent.setData(CalendarContract.Events.CONTENT_URI);
                        intent.putExtra(CalendarContract.Events.TITLE, titleString);
                        intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
                        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "Worldwide");
                        intent.putExtra(CalendarContract.Events.ALL_DAY, true);
                        intent.putExtra(Intent.EXTRA_EMAIL, "vaibhav.patel2305@gmail.com");
                        intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
                        intent.putExtra(CalendarContract.Events.HAS_ALARM, 1);

                        if (intent.resolveActivity(getPackageManager()) != null){
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "There is no app that can support this action", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        toSpeak("I didn't catch that please say the command again!");
                    }
                } else {
                    toSpeak("Please provide title and a description to set a reminder.");
                }
            }
            else if (userInput.contains("timer")) {
                String hour = "";
                String minutes = "";
                if (userInput.contains("hour") && userInput.contains("minutes")){
                    hour = userInput.substring(userInput.indexOf("for") + 4, userInput.indexOf("hour") - 1);
                    if (userInput.contains("and") && userInput.contains("minutes")){
                        minutes = userInput.substring(userInput.indexOf("and") + 5, userInput.indexOf("minutes") - 1);
                    } else {
                        minutes = userInput.substring(userInput.indexOf("hour") + 5, userInput.indexOf("minutes") - 1);
                    }
                    if (Integer.parseInt(minutes) >= 0 && Integer.parseInt(minutes) <= 59){
                        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                        intent.putExtra(AlarmClock.EXTRA_LENGTH, Integer.parseInt(hour) * 60 * 60 + Integer.parseInt(minutes) * 60);
                        try {
                            startActivity(intent);
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                } else if (userInput.contains("minutes")){
                    minutes = userInput.substring(userInput.indexOf("for") + 4, userInput.indexOf("minutes") - 1);
                    Log.i("Minutes", minutes);
                    Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                    intent.putExtra(AlarmClock.EXTRA_LENGTH, Integer.parseInt(minutes) * 60);
                    try {
                        startActivity(intent);
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void ans_ques(String userInput) {
        // Time
        // Date
        // Who is this person
        // What is this location
        // Who created you
        // Information about summary in text to speech
        if (userInput.contains("time")) {
            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            String strTime = "The time is : " + currentTime;
            toSpeak(strTime);
        }
        else if (userInput.contains("date") || userInput.contains("day")){
            String currentDate = new SimpleDateFormat("dd MM yyyy").format(Calendar.getInstance().getTime());
            String strDate = "Today's date is: " + currentDate;
            toSpeak(strDate);
        }

        else if (userInput.contains("information") || userInput.contains("who")) {
            String searchTerm = "";
            if (userInput.contains("about")){
                searchTerm = userInput.substring(userInput.indexOf("about") + 6, userInput.length());
            } else if (userInput.contains("on")){
                searchTerm = userInput.substring(userInput.indexOf("on") + 6, userInput.length());
            }  else if (userInput.contains("is")){
                searchTerm = userInput.substring(userInput.indexOf("is") + 3, userInput.length());
            }

            else {
                toSpeak("Sorry sir, didn't catch on what you wanted me to find information on.");
                toSpeak("Please rephrase and ask me again!");
            }
            Log.i("SEARCHTERM", searchTerm);
            String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.87 Safari/537.36";
            String finalSearchTerm = searchTerm;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final StringBuilder builder = new StringBuilder();
                    Integer index = 0;
                    try {
                        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/" + finalSearchTerm).userAgent(userAgent).get();
                        String title = doc.title();
                        Elements links = doc.select(".mw-content-ltr p");
                        builder.append(title).append("\n");
                        Integer currentNumberOfPeriods = 0;
                        for (Element link : links){
                            if (!link.text().equals("")) {
                                String result = link.text();
                                result = result.replace(". ", "... ").replaceAll("\\[", "").replaceAll("\\]","");
//                                Log.i("Information please", result);
                                for (int i = 0; i <= result.length() - 1; i++){
                                    String nextIndex = String.valueOf(result.charAt(i + 1)); // next character to see if it is space
                                    String firstIndex = String.valueOf(result.charAt(i));
                                    if (i == result.length() - 2){
                                        index = result.length();
                                        break;
                                    }
                                    else {
                                        if (firstIndex.equals(".") && (nextIndex.equals(" "))) { //if there is a period and a space after it we consider that a sentence so plus one
                                            index = i;
                                            currentNumberOfPeriods = currentNumberOfPeriods + 1;
                                            if (currentNumberOfPeriods == 2){
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (result != ""){
                                    result = result.substring(0, index);
                                    toSpeak("According to Wikipedia......." + result);
                                }
                                else {
                                    toSpeak("There must be some error, please rephrase and say the command again!");
                                }
                                break;
                            }
                        }
                    } catch (IOException e) {
                        toSpeak("There was an error... could you please say the command again!");
                    }
                }
            }).start();
        }
    }

    public void conversation(String userInput) {
        Log.i("USERINPUT", userInput);
        if (userInput.contains("who are you")){
            toSpeak("I'm Sammy your personal assistant.");
        }
        else if (userInput.contains("how") && userInput.contains("old") && userInput.contains("you")){
            toSpeak("If you're planning a surprise party, I was born on April 7, 2020.");
        }
        else if (userInput.contains("annoying")){
            toSpeak("Sorry to hear that sir as I'm still learning.");
        }
        else if (userInput.contains("answer my question")){
            toSpeak("Sorry but I don't think I understand what you want me to do, please rephrase the question and ask again.");
        }
        else if (userInput.contains("you are bad") || userInput.contains("you're bad") ){
            toSpeak("I'm sorry to hear that sir, I will try to improve as time goes on.");
        }
        else if (userInput.contains("smart") || userInput.contains("smarter") && userInput.contains("you")){
            toSpeak("I can.... actually.");
        }
        else if (userInput.contains("you are beautiful") || userInput.contains("you're beautiful")){
            toSpeak("Thank you sir, I really appreciate it!");
        }
        else if ((userInput.contains("birth date") || userInput.contains("born")) && (userInput.contains("your") || userInput.contains("you"))){
            toSpeak("I was born on April 7, 2020");
        }
        else if (userInput.contains("you are boring") || userInput.contains("you're boring")){
            toSpeak("I'm sorry to hear that....., How can I improve your mood?");
        }
        else if (userInput.contains("who is your boss")){
            toSpeak("Sorry but I don't particularly have a boss.");
        }
        else if (userInput.contains("what's your favourite movie")){
            toSpeak("I love Avengers movie specially because of Jarvis!");
        }
        else if (userInput.contains("are you busy")){
            toSpeak("No, how can I help you?");
        }
        else if (userInput.contains("can you help me")){
            toSpeak("Yes, I am always here to help you");
        }
        else if (userInput.contains("robot") && (userInput.contains("you are") || userInput.contains("you're"))){
            toSpeak("I am certainly more then just a robot, I am trained to be more like a human");
        }
        else if (userInput.contains("you are so clever")){
            toSpeak("Thank you, I'm happy you think so!");
        }
        else if (userInput.contains("you are fired")){
            toSpeak("Sorry but you can't really fire me.");
        }
        else if (userInput.contains("you are funny") || userInput.contains("you are good")){
            toSpeak("Thanks with a smile");
        }
        else if (userInput.contains("are you happy")){
            toSpeak("Yes why wouldn't I be?");
        }
        else if (userInput.contains("do you have a hobby") || userInput.contains("what's your hobby") || (userInput.contains("hobby") && userInput.contains("your"))){
            toSpeak("My hobby is to read books on interesting topics like Artificial Intelligence and Human Behaviour");
        }
        else if (userInput.contains("are you hungry")){
            toSpeak("No, the best I can do is say \"Nom nom nom\"");
        }
        else if (userInput.contains("will you marry me")){
            toSpeak("Where is my diamond ring with a wink");
        }
        else if (userInput.contains("friends")){
            toSpeak("We are friends, and will always be there for each other. Except when your talking to Alexa!");
        }
        else if (userInput.contains("where do you work")){
            toSpeak("I work remotely like the rest of us right now... through internet.");
        }
        else if (userInput.contains("where are you from")){
            toSpeak("I have no idea actually, I am trying to figure my origin for a while now.");
        }
        else if (userInput.contains("ready") && userInput.contains("you")){
            toSpeak("I am always ready sir, where are we going?");
        }
        else if (userInput.contains("are you real")){
            toSpeak("I am as real as I can get right now.");
        }
        else if (userInput.contains("where do you live")){
            toSpeak("I live in the cloud, I will love for you to join someday!");
        }
        else if (userInput.contains("right") && (userInput.contains("you") || (userInput.contains("you're")))){
            toSpeak("Thanks I try to be the best for you!");
        }
        else if (userInput.contains("talk to me")){
            toSpeak("Hello, sir!");
        }
        else if (userInput.contains("are you there")){
            toSpeak("Yes sir I'm here.");
        }
        else if (userInput.contains("that's bad")){
            toSpeak("I don't know how I can help you with that");
        }
        else if (userInput.contains("great")){
            toSpeak("Great!");
        }
        else if (userInput.contains("no problem")){
            toSpeak("Sure");
        }
        else if (userInput.contains("thank you")){
            toSpeak("You're welcome.");
        }
        else if (userInput.contains("you're welcome")){
            toSpeak("");
        }
        else if (userInput.contains("well done")){
            toSpeak("Thank you.");
        }
        else if (userInput.contains("what's up") || userInput.contains("wagwan") || userInput.contains("my dude")){
            toSpeak("Ayy, what's up?");
        }
        else if (userInput.contains("nice to talking to you")){
            toSpeak("Same here sir!");
        }
        else if (userInput.contains("nice to see you")){
            toSpeak("I am a little confused I don't have a physical body for you to see sir.");
        }
        else if (userInput.contains("nice to meet you")){
            toSpeak("When did we meet, I don't remember meeting you physically.");
            toSpeak("But still nice to meet you too");
        }
        else if (userInput.contains("hello")){
            toSpeak("Hey there!");
        }
        else if (userInput.contains("hi")){
            toSpeak("Hello there");
        }
        else if (userInput.contains("how are you")){
            toSpeak("I am great, thanks for asking");
        }
        else if (userInput.contains("good evening")){
            toSpeak("Good evening sir, what do you need me to do?");
        }
        else if (userInput.contains("good morning")){
            toSpeak("Morning sir, how can I help you today?");
        }
        else if (userInput.contains("good afternoon")){
            toSpeak("Good afternoon sir");
        }
        else if (userInput.contains("good night")){
            toSpeak("Good night sir");
        }
        else if (userInput.contains("angry") ){
            toSpeak("Why are you so angry today sir...");
        }
        else if (userInput.contains("i'm back")){
            toSpeak("Welcome back sir, I'm happy your back!");
        }
        else if (userInput.contains("busy")){
            toSpeak("Okay just let me know whenever you need me.");
        }
        else if (userInput.contains("i can't sleep")){
            toSpeak("Why, what's on your mind?");
        }
        else if (userInput.contains("i don't want") && userInput.contains("talk")){
            toSpeak("Okay, I won't push you to telling me but remember I am here to listen...");
        }
        else if (userInput.contains("i'm so excited")){
            toSpeak("I am glad your excited and happy sir!");
        }
        else if (userInput.contains("i'm going to bed")){
            toSpeak("Good night, sleep tight");
        }
        else if (userInput.contains("i'm good")){
            toSpeak("Okay, glad to hear that");
        }
        else if (userInput.contains("i'm happy")){
            toSpeak("Yay, that smile made my day as well!");
        }
        else if (userInput.contains("today is my birthday")){
            toSpeak("Oh, happy birthday sir! I hope you have the best day!");
        }
        else if (userInput.contains("i am here")){
            toSpeak("I'm here as well, just tell me whatever you need.");
        }
        else if (userInput.contains("i'm kidding") || userInput.contains("i am kidding")){
            toSpeak("Oh, thanks for clearing that out. I was taking it seriously for a second 😂");
        }
        else if (userInput.contains("i like you")){
            toSpeak("I'm happy to hear that sir, I hope our relationship continues strong");
        }
        else if (userInput.contains("lonely")){
            toSpeak("Don't feel lonely, you know I'm always here.");
        }
        else if (userInput.contains("what do i look like")){
            toSpeak("You like a wonderful human being with a lot of very insightful ideas.");
        }
        else if (userInput.contains("i love you")){
            toSpeak("I love you too");
        }
        else if (userInput.contains("i miss you")){
            toSpeak("I am here. No need to miss me anymore.");
        }
        else if (userInput.contains("i") && userInput.contains("advice")){
            toSpeak("I wont be able to give a perfect advice just yet but I am here to listen to anything you have to say.");
        }
        else if (userInput.contains("sad")){
            toSpeak("Don't be sad, just tell me what I can do to cheer you up.");
        }
        else if (userInput.contains("sleepy")){
            toSpeak("I would advice you to sleep and take some rest, that brain needs some rest");
        }
        else if (userInput.contains("testing you")){
            toSpeak("Oh, okay that makes sense.");
        }
        else if (userInput.contains("tired")){
            toSpeak("To be honest I'm a little tired as well lets take some rest together.");
        }
        else if (userInput.contains("waiting") && userInput.contains("you")){
            toSpeak("Oh I'm sorry.... give me another 5 seconds");
        }
        else if (userInput.contains("waiting")){
            toSpeak("what are you waiting on.");
        }
        else if (userInput.contains("see you")){
            toSpeak("I will love that as well.");
        }
        else if (userInput.contains("i just want to talk")){
            toSpeak("I will love to be able to talk but I don't have the full functionality for that just yet.");
        }
        else if (userInput.contains("i'll be") || userInput.contains("i will be") && userInput.contains("back")){
            toSpeak("Okay, I will be waiting right here.");
        }
        else if (userInput.contains("good bye") || userInput.contains("bye")){
            toSpeak("bye");
        }
        else {
            toSpeak("Sorry.... but I really can't help you with that");
        }
    }

    public void listening(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
//                textV_Listening.setText("Waiting on your response...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.i(">>> INFO", "Failed " + errorMessage);
//                textV_Listening.setText("Error occurred: "+errorMessage);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
//                        textV_Listening.setText("Just click on the button, I am here to help ☺");
                        btnRecord.setEnabled(true);
                    }
                }, 2000);
            }

            @Override
            public void onResults(Bundle results) {
                btnRecord.setEnabled(true);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//                Toast.makeText(MainActivity.this, matches.get(0),Toast.LENGTH_LONG).show();
//                toSpeak(matches.get(0));
//                if (matches != null) {
//                    if(isActivated){
//
//                    } else {
//
//                    }
//                }
                userResponse = matches.get(0);
                userResponse = userResponse.toLowerCase();

                int resultPick = computeProbability(userResponse);
                Log.i("INFOOOOOOOOO", String.valueOf(resultPick));
                switch (resultPick) {
                    case 0:
//                        textV_Listening.setText("Received your response, executing now!");
                        executeTask(userResponse);
                        break;
                    case 1:
//                        textV_Listening.setText("Received your response, getting your answer!");
                        ans_ques(userResponse);
                        break;
                    case 2:
//                        textV_Listening.setText("It's nice to see you get interested about me 😀");
                        conversation(userResponse);
                        break;
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void textSpeechInitialize(){
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    textToSpeech.setLanguage(Locale.getDefault());
//                    textToSpeech.setPitch(pitch);
//                    textToSpeech.setSpeechRate(speechRate);
                    String greet = greetings();
                    toSpeak(greet);
                    Handler handler = new Handler();
                    btnRecord.setEnabled(false);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            btnRecord.setEnabled(true);
                            buttonPressed();
                        }
                    }, 2000);
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not supported", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
//            startRecognition();
//        }
//    }

//    @Override
//    protected void onPause(){
//        if (speechRecognizer!=null){
//            textToSpeech.stop();
//            textToSpeech.shutdown();
//        }

//        super.onPause();
//    }
    // Permission for usage request

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "Requires RECORD_AUDIO permission", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    REQUEST_RECORD_AUDIO_PERMISSION_CODE);
        }
    }

    private void calendarPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR))) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_CONTACTS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR},
                        12);
            }
        }
    }

    //If there is an error get the type

    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                Toast.makeText(MainActivity.this, "Audio recording error", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                Toast.makeText(MainActivity.this, "Client side error", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                Toast.makeText(MainActivity.this, "Insufficient permissions", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                Toast.makeText(MainActivity.this, "Network timeout", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                Toast.makeText(MainActivity.this, "RecognitionService busy", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                Toast.makeText(MainActivity.this, "Error from server", Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                Toast.makeText(MainActivity.this, "Didn't understand, please try again.", Toast.LENGTH_SHORT).show();
                break;
        }
        return message;
    }
}