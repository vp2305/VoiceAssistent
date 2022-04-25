package com.example.aimark2app;

import android.Manifest;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.ParseException;
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
    Button btnStop;
    ImageButton btnRecord;
    // FloatingActionButton btnRecord;
    String userResponse;
    Float speechRate = 1f;
    String date, month, year, endWeek, monthChange, yearChange;
    ListView listView, todayWeekListView;
    String[] calendarEventTitle;
    String[] calendarEventTime;
    String[] todayEventTitle;
    String[] todayEventTime;
    String[] thisWeekEventTitle;
    String[] thisWeekEventTime;
    List<String> sortedCalendarTime = new ArrayList<String>();
    List<String> sortedCalendarTitle = new ArrayList<String>();
    List<String> tempAllDayTitle = new ArrayList<String>();
    List<String> tempAllDayTime = new ArrayList<String>();
    Boolean waitSpeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // btnRecord = findViewById(R.id.btnRecord);
        // btnStop = findViewById(R.id.btnStop);

        TextView greetingTV = findViewById(R.id.greetingTV);

        TextView todayTitle = findViewById(R.id.todayTitle);

        Calendar c = Calendar.getInstance();
        int time = c.get(Calendar.HOUR_OF_DAY);
        if (time >= 0 && time < 12) {
            greetingTV.setText("Good Morning 🌅");
        } else if (time >= 12 && time < 16) {
            greetingTV.setText("Good Afternoon");
        } else if (time >= 16 && time < 22) {
            greetingTV.setText("Good Evening 🌆");
        } else if (time >= 22 && time < 24) {
            greetingTV.setText("Good Night 💤");
        }

        String r = currentYear();
        String[] year_date_month_ending_date = r.split(" ");

        date = year_date_month_ending_date[0];
        month = year_date_month_ending_date[1];
        year = year_date_month_ending_date[2];
        endWeek = year_date_month_ending_date[3];
        monthChange = year_date_month_ending_date[4];
        yearChange = year_date_month_ending_date[5];

        String monthName = getMonth(Integer.parseInt(month));

        todayTitle.setText("TODAY- " + monthName + " " + date);

        calendarPermission();

        Log.i("WE ARE HERE UP", "truee");

        PackageManager pm = getApplicationContext().getPackageManager();
        int hasPermission = pm.checkPermission(Manifest.permission.READ_CALENDAR,
                getApplicationContext().getPackageName());

        Log.i("HAS PERMISSION", String.valueOf(hasPermission));

        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            viewCalendar();
        } else {
            thisWeekEventTitle = new String[1];
            thisWeekEventTime = new String[1];
            thisWeekEventTitle[0] = "Need permission to read calendar inorder to show the events.";
            todayEventTitle = new String[1];
            todayEventTime = new String[1];
            todayEventTitle[0] = "Please give permission to read calendar.";
        }

        // stopPressed();
        textSpeechInitialize();
        listening();

        try {
            listView = findViewById(R.id.listView);
            MyAdapter adapter = new MyAdapter(getApplicationContext(), thisWeekEventTitle, thisWeekEventTime);
            listView.setAdapter(adapter);
            todayWeekListView = findViewById(R.id.todayWeekListView);
            TodayWeek todayAdapter = new TodayWeek(getApplicationContext(), todayEventTitle, todayEventTime);
            todayWeekListView.setAdapter(todayAdapter);
            Utility.setListViewHeightBasedOnChildren(todayWeekListView);
            Utility.setListViewHeightBasedOnChildren(listView);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "No calendar event till Saturday 😊", Toast.LENGTH_LONG).show();
        }
    }

    // Trigger button to listen
    public void recordPressed(View v) {
        // btnRecord.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // if (ContextCompat.checkSelfPermission(MainActivity.this,
        // Manifest.permission.RECORD_AUDIO)
        // != PackageManager.PERMISSION_GRANTED) {
        // audioRequestPermission();
        // } else {
        // btnRecord.setEnabled(false);
        // startRecognition();
        // }
        // }
        // });
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioRequestPermission();
        } else {
            Toast.makeText(this, "Listening...", Toast.LENGTH_LONG).show();
            // btnRecord.setEnabled(false);
            textToSpeech.stop();
            startRecognition();
        }
    }

    public void stopPressed(View v) {
        // btnStop.setOnClickListener(new View.OnClickListener(){
        // @Override
        // public void onClick(View v) {
        // if (textToSpeech.isSpeaking()){
        // Log.i("Button", "pressed");
        // textToSpeech.stop();
        // }
        // }
        // });
        if (textToSpeech.isSpeaking()) {
            Log.i("Button", "pressed");
            textToSpeech.stop();
        }
    }

    public void goodMorningRoutine(View v) {
        Toast.makeText(MainActivity.this, "Starting morning routine...", Toast.LENGTH_SHORT).show();
        toSpeak("Good Morning, Vaibhav. I hope you have a great day today!");
        int t = getWaitTime("Good Morning, Vaibhav. I hope you have a great day today!");
        int timeToWait = todayAllEvent();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    Intent spotifyIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("spotify:playlist:37i9dQZF1DWXT8uSSn6PRy:play"));
                    startActivity(spotifyIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Couldn't find spotify to continue with the routine.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }, t + timeToWait + 2500);

    }

    public void goodNightRoutine(View v) {
        Toast.makeText(MainActivity.this, "Starting night routine...", Toast.LENGTH_SHORT).show();
        toSpeak("Good Night Vaibhav, Sweet Dreams!");
        int t = getWaitTime("Good Night Vaibhav, Sweet Dreams!");
        int timeToWait = firstEvent();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    Intent spotifyIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("spotify:playlist:37i9dQZF1DWZd79rJ6a7lp:play"));
                    startActivity(spotifyIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Couldn't find spotify to continue with the routine.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }, t + timeToWait + 2500);
    }

    public int firstEvent() {
        if (todayEventTitle.length == 1 && todayEventTitle[0].equals("No event today")) {
            toSpeakNext("You have " + todayEventTitle[0]);
            int timeToWait = getWaitTime("You have no event today!");
            return timeToWait;
        } else {
            Calendar c = Calendar.getInstance();
            int t = c.get(Calendar.HOUR_OF_DAY);
            if (t >= 12 && t < 24) {
                String title = thisWeekEventTitle[0];
                String dateTime = thisWeekEventTime[0];
                String[] splitDateTime = dateTime.split(" ");
                String time = splitDateTime[1] + " " + splitDateTime[2];
                String result = "";
                if (time.equals("All Day")) {
                    result = ("First up tomorrow, you have " + title + " " + time + ".");
                } else {
                    result = ("First up tomorrow, you have " + title + " at " + time + ".");
                }
                int timeToWait = getWaitTime(result);
                toSpeakNext(result);
                return timeToWait;
            } else {
                String title = todayEventTitle[0];
                String dateTime = todayEventTime[0];
                String[] splitDateTime = dateTime.split(" ");
                String time = splitDateTime[1] + " " + splitDateTime[2];
                String result = "";
                if (time.equals("All Day")) {
                    result = ("First up today, you have " + title + " " + time + ".");
                } else {
                    result = ("First up today, you have " + title + " at " + time + ".");
                }
                int timeToWait = getWaitTime(result);
                toSpeakNext(result);
                return timeToWait;
            }
        }
    }

    public int todayAllEvent() {
        String result = "Today you have: ";
        if (todayEventTitle.length == 1 && todayEventTitle[0].equals("No event today")) {
            toSpeakNext("You have " + todayEventTitle[0]);
            int timeToWait = getWaitTime("You have no event today!");
            return timeToWait;
        } else {
            Log.i("LENGTHH", String.valueOf(todayEventTitle.length));
            for (int i = 0; i < todayEventTitle.length; i++) {
                String title = todayEventTitle[i];
                String dateTime = todayEventTime[i];
                String[] splitDateTime = dateTime.split(" ");
                String time = splitDateTime[1] + " " + splitDateTime[2];
                if (i == todayEventTitle.length - 1) {
                    result += (title + " at " + time + ".");
                } else {
                    result += (title + " at " + time + ", and ");
                }
            }
            int timeToWait = getWaitTime(result);
            toSpeakNext(result);
            return timeToWait;
        }
    }

    public void toSpeakNext(String toSpeak) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(">>>Voice Info", String.valueOf(textToSpeech.getVoice()));
        }
        try {
            textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null);
            waitSpeak = true;
        } catch (Exception e) {
            e.printStackTrace();
            // btnStop.setVisibility(View.INVISIBLE);
        }
    }

    public String getMonth(int month) {
        return new DateFormatSymbols().getMonths()[month - 1];
    }

    class TodayWeek extends ArrayAdapter<String> {
        Context context;
        String[] twTitle;
        String[] twTime;

        public TodayWeek(@NonNull Context context, String[] title_tw, String[] time_tw) {
            super(context, R.layout.row, R.id.calendarText1, title_tw);
            this.context = context;
            this.twTitle = title_tw;
            this.twTime = time_tw;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.aladin);
            Typeface typeface2 = ResourcesCompat.getFont(getApplicationContext(), R.font.amarante);
            Typeface typeface3 = ResourcesCompat.getFont(getApplicationContext(), R.font.acme);
            View today_row = inflater.inflate(R.layout.row, parent, false);
            ImageView image = today_row.findViewById(R.id.calendar_image);
            TextView myTitle = today_row.findViewById(R.id.calendarText1);
            TextView myTime = today_row.findViewById(R.id.calendarText2);
            TextView eventTitle = today_row.findViewById(R.id.eventTitle);

            eventTitle.setTypeface(typeface3);
            image.setImageResource(R.drawable.calendar);
            myTitle.setText(twTitle[position]);
            myTitle.setTypeface(typeface);
            myTime.setText(twTime[position]);
            myTime.setTypeface(typeface2);
            return today_row;
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
            LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(R.layout.row, parent, false);
            Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.aladin);
            Typeface typeface2 = ResourcesCompat.getFont(getApplicationContext(), R.font.amarante);
            Typeface typeface3 = ResourcesCompat.getFont(getApplicationContext(), R.font.acme);
            ImageView image = row.findViewById(R.id.calendar_image);
            TextView myTitle = row.findViewById(R.id.calendarText1);
            TextView myDesc = row.findViewById(R.id.calendarText2);
            TextView eventTitle = row.findViewById(R.id.eventTitle);

            eventTitle.setTypeface(typeface3);
            image.setImageResource(R.drawable.calendar);
            myTitle.setText(rTitle[position]);
            myTitle.setTypeface(typeface);
            myDesc.setText(rDescription[position]);
            myDesc.setTypeface(typeface2);
            return row;
        }
    }

    private void viewCalendar() {
        int index = 0;
        int count = 0;
        List<String> calendarEvent = new ArrayList<String>();
        List<String> calendarTime = new ArrayList<String>();

        String[] PROJECTION = new String[] { CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY };

        int id_index = 0, begin_index = 1, title_index = 2, allDay_index = 3;

        Calendar beginTime = Calendar.getInstance(TimeZone.getDefault());
        beginTime.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(date));
        long startMillis = beginTime.getTimeInMillis();
        Log.i("Startt mills", String.valueOf(startMillis));

        Calendar endTime = Calendar.getInstance(TimeZone.getDefault()); // Integer.parseInt(endWeek)
        if (Integer.parseInt(monthChange) == -1 && Integer.parseInt(yearChange) == -1) {
            endTime.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(endWeek), 24, 00, 00);
        } else {
            if (Integer.parseInt(monthChange) != -1 && Integer.parseInt(yearChange) == -1) {
                endTime.set(Integer.parseInt(year), Integer.parseInt(monthChange), Integer.parseInt(endWeek), 24, 00,
                        00);
            } else if (Integer.parseInt(monthChange) == -1 && Integer.parseInt(yearChange) != -1) {
                endTime.set(Integer.parseInt(yearChange), Integer.parseInt(month), Integer.parseInt(endWeek), 24, 00,
                        00);
            } else {
                endTime.set(Integer.parseInt(yearChange), Integer.parseInt(monthChange), Integer.parseInt(endWeek), 24,
                        00, 00);
            }
        }

        long endMillis = endTime.getTimeInMillis();
        Log.i("END TIME", String.valueOf(endMillis));

        Cursor c = null;
        ContentResolver cr = getContentResolver();

        // Construct the query with the desired date range.

        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        c = cr.query(builder.build(), PROJECTION, null, null, null);

        /*
         * Counting how many items there are
         */
        while (c.moveToNext()) {
            count++;
        }

        c.moveToPosition(-1);

        /*
         * Adding items into an array
         */
        if (count != 0) {
            while (c.moveToNext()) {
                String title = null;
                long eventID = 0;
                long beginVal = 0;
                eventID = c.getLong(id_index);
                final Boolean allDay = !c.getString(allDay_index).equals("0");
                Log.i("ALL DAY", String.valueOf(allDay));
                title = c.getString(title_index);
                calendarEvent.add(title);

                Calendar calendar = Calendar.getInstance();
                beginVal = c.getLong(begin_index);
                calendar.setTimeInMillis(beginVal);
                SimpleDateFormat formatter;

                if (allDay) {
                    formatter = new SimpleDateFormat("dd/MM/yyyy");
                    String date = formatter.format(calendar.getTime());

                    try {
                        calendar.setTime(formatter.parse(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    calendar.add(Calendar.DATE, 1);
                    String updated_time = formatter.format(calendar.getTime());
                    calendarTime.add(updated_time + " " + "All Day");

                    if (!tempAllDayTitle.contains(title)) {
                        tempAllDayTitle.add(title);
                        tempAllDayTime.add(updated_time + " " + "All Day");
                    }

                } else {
                    formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a");
                    calendarTime.add(formatter.format(calendar.getTime()));
                }
                index++;
            }

            Log.i("INDEX", String.valueOf(index));

            Log.i("TEMP ALL DAY TITLE", tempAllDayTitle.toString());
            Log.i("TEMP ALL DAY TIME", tempAllDayTime.toString());

            /*
             * Sorting the array so that the calendar is in the right order
             */
            // Log.i("CALTITLE", calendarEvent.toString());
            Log.i("CALTIME", calendarTime.toString());

            calendarSort(calendarTime, calendarEvent);
            calendarEventTitle = new String[count];
            calendarEventTime = new String[count];
            eventToday();

        }
        if (count == 0) {
            thisWeekEventTitle = new String[1];
            thisWeekEventTime = new String[1];
            // Log.i("Calendar", "No Calendar Event");
            thisWeekEventTitle[0] = "No calendar event";

            todayEventTitle = new String[1];
            todayEventTime = new String[1];
            todayEventTitle[0] = "No event today";
            todayEventTime[0] = "";
        }

    }

    public void eventToday() {
        String[] timeSplit;
        String splitDate;
        int index = 0;
        int todayEventCount = 0;
        int weekEventCount = 0;
        int idx = 0;

        String currentDate = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());

        for (String w : sortedCalendarTime) {
            timeSplit = w.split(" ");
            splitDate = timeSplit[0];
            if (splitDate.equals(currentDate)) {
                todayEventCount++;
            } else {
                weekEventCount++;
            }
        }

        Log.i("Today EVENT COUNT", String.valueOf(todayEventCount));
        Log.i("WEEK EVENT COUNT", String.valueOf(weekEventCount));

        todayEventTitle = new String[todayEventCount];
        todayEventTime = new String[todayEventCount];
        thisWeekEventTitle = new String[weekEventCount];
        thisWeekEventTime = new String[weekEventCount];

        if (todayEventCount == 0) {
            todayEventTitle = new String[1];
            todayEventTime = new String[1];
            todayEventTitle[0] = "No event today";
        }

        if (weekEventCount == 0) {
            thisWeekEventTitle = new String[1];
            thisWeekEventTime = new String[1];
            thisWeekEventTitle[0] = "No calendar event";
        }

        if (todayEventCount != 0 || weekEventCount != 0) {
            for (int i = 0; i < sortedCalendarTime.size(); i++) {
                String tempDateTime = sortedCalendarTime.get(i);
                timeSplit = tempDateTime.split(" ");
                if (timeSplit[0].equals(currentDate)) {
                    todayEventTitle[index] = sortedCalendarTitle.get(i);

                    String dateTime = sortedCalendarTime.get(i);
                    String[] splitDT = dateTime.split(" ");
                    String updatedTime = "Today, " + splitDT[1] + " " + splitDT[2];
                    todayEventTime[index] = updatedTime;
                    index++;
                } else {
                    thisWeekEventTitle[idx] = sortedCalendarTitle.get(i);
                    thisWeekEventTime[idx] = sortedCalendarTime.get(i);
                    idx++;
                }
            }
        }
    }

    public void calendarSort(List<String> calTime, List<String> calTitle) {

        List<String> amTitle = new ArrayList<String>();
        List<String> amList = new ArrayList<String>();
        List<String> pmTitle = new ArrayList<String>();
        List<String> pmList = new ArrayList<String>();
        List<String> tempCalendar = new ArrayList<String>();
        List<String> allDayTitle = new ArrayList<String>();
        List<String> allDay = new ArrayList<String>();

        // Log.i("CALENDAR TIME", calTime.toString());

        tempCalendar.addAll(calTime);

        Collections.sort(calTime);

        for (String dateTime : calTime) {
            String[] splitDateTime = dateTime.split(" ");
            int size = 1;
            if (!amList.isEmpty() || !pmList.isEmpty() || !amTitle.isEmpty() || !pmTitle.isEmpty()
                    || !allDayTitle.isEmpty() || !allDayTitle.isEmpty() || !allDay.isEmpty()) {
                sortedCalendarTime.addAll(amList);
                sortedCalendarTime.addAll(pmList);
                sortedCalendarTitle.addAll(amTitle);
                sortedCalendarTitle.addAll(pmTitle);
                sortedCalendarTitle.addAll(allDayTitle);
                sortedCalendarTime.addAll(allDay);
                amList.clear();
                pmList.clear();
                amTitle.clear();
                pmTitle.clear();
                allDayTitle.clear();
                allDay.clear();
            }
            for (String dTime : calTime) {
                String[] splitDTime = dTime.split(" ");
                if (splitDTime[0].equals(splitDateTime[0]) && !sortedCalendarTime.contains(dTime)) {
                    if (splitDTime[2].equals("a.m.")) {
                        if (amList.isEmpty()) {
                            int idx = tempCalendar.indexOf(dTime);
                            String title = calTitle.get(idx);
                            amTitle.add(title);
                            amList.add(dTime);

                            if (size == calTime.size()) {
                                sortedCalendarTime.addAll(amList);
                                sortedCalendarTitle.addAll(amTitle);
                                amList.clear();
                                amTitle.clear();
                            }
                        } else {
                            int index = 0;
                            for (String amDateTime : amList) {
                                String[] split_amDTime = amDateTime.split(" ");
                                String strTime = split_amDTime[1];
                                strTime = strTime.replace(":", "");
                                int integerTime = Integer.parseInt(strTime);

                                String[] split_amDateTime = dTime.split(" ");
                                String strDateTime = split_amDateTime[1];
                                strDateTime = strDateTime.replace(":", "");
                                int integerDateTime = Integer.parseInt(strDateTime);

                                if (integerDateTime >= 1200 && integerDateTime <= 1259) {
                                    index = 0;
                                    break;
                                } else if ((integerTime >= integerDateTime) && integerDateTime <= 1259) {
                                    break;
                                }
                                index++;
                            }

                            if (!amList.contains(dTime)) {
                                amList.add(index, dTime);
                                int idx = tempCalendar.indexOf(dTime);
                                String title = calTitle.get(idx);
                                amTitle.add(index, title);
                            }
                        }
                    } else if (splitDTime[2].equals("p.m.")) {
                        if (pmList.isEmpty()) {
                            pmList.add(dTime);

                            int idx = tempCalendar.indexOf(dTime);
                            String title = calTitle.get(idx);
                            pmTitle.add(title);

                            if (size == tempCalendar.size()) {
                                sortedCalendarTime.addAll(pmList);
                                sortedCalendarTitle.addAll(pmTitle);
                                pmList.clear();
                                pmTitle.clear();
                            }
                        } else {
                            int index = 0;
                            for (String pmDateTime : pmList) {
                                String[] split_pmDTime = pmDateTime.split(" ");
                                String strTime = split_pmDTime[1];
                                strTime = strTime.replace(":", "");
                                int integerTime = Integer.parseInt(strTime);

                                String[] split_pmDateTime = dTime.split(" ");
                                String strDateTime = split_pmDateTime[1];
                                strDateTime = strDateTime.replace(":", "");
                                int integerDateTime = Integer.parseInt(strDateTime);

                                if (integerDateTime >= 1200 && integerDateTime <= 1259) {
                                    index = 0;
                                    break;
                                } else if ((integerTime >= integerDateTime) && integerTime < 1200) {
                                    break;
                                }
                                index++;
                            }

                            if (!pmList.contains(dTime)) {
                                pmList.add(index, dTime);
                                int idx = tempCalendar.indexOf(dTime);
                                String title = calTitle.get(idx);
                                pmTitle.add(index, title);
                            }
                        }
                    } else {
                        Log.i("SIZE", String.valueOf(size));
                        Log.i("D TIME", dTime);
                        // if (allDay.isEmpty()) {
                        //// int idx = tempCalendar.indexOf(dTime);
                        //// String title = calTitle.get(idx);
                        //// allDayTitle.add(title);
                        //// allDay.add(dTime);
                        // int idx = tempAllDayTime.indexOf(dTime);
                        // String title = tempAllDayTitle.get(idx);
                        // tempAllDayTitle.remove(idx);
                        // tempAllDayTime.remove(idx);
                        //
                        // allDayTitle.add(title);
                        // allDay.add(dTime);
                        //
                        // if (size == tempCalendar.size()) {
                        // sortedCalendarTime.addAll(allDay);
                        // sortedCalendarTitle.addAll(allDayTitle);
                        // allDay.clear();
                        // allDayTitle.clear();
                        // }
                        // } else {
                        // int idx = tempCalendar.indexOf(dTime);
                        // String title = calTitle.get(idx);
                        // if (!allDayTitle.contains(title)){
                        // allDayTitle.add(title);
                        // allDay.add(dTime);
                        // }
                        // }
                        try {
                            int idx = tempAllDayTime.indexOf(dTime);
                            String title = tempAllDayTitle.get(idx);
                            tempAllDayTitle.remove(idx);
                            tempAllDayTime.remove(idx);
                            allDayTitle.add(title);
                            allDay.add(dTime);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (size == tempCalendar.size()) {
                            sortedCalendarTime.addAll(allDay);
                            sortedCalendarTitle.addAll(allDayTitle);
                            allDay.clear();
                            allDayTitle.clear();
                        }

                        // Log.i("ALL DAY TITLE", allDayTitle.toString());
                        // Log.i("All day time", allDay.toString());
                    }
                }
                size++;
            }
        }
        Log.i("SORTED CALENDAR TITLE", sortedCalendarTitle.toString());
        Log.i("SORTED CALENDAR TIME", sortedCalendarTime.toString());
    }

    public static String currentYear() {
        Date d = new Date();
        Date endingWeekDate = new Date();
        int year = 1900 + d.getYear();
        int month = d.getMonth();
        int date = d.getDate();
        int day = d.getDay();
        int ending_week = 0;
        int monthChange = -1;
        int yearChange = -1;

        Log.i("dateeee", String.valueOf(date));

        if (day != 6) {
            // ending_week = date + (6 - day);
            endingWeekDate.setDate(d.getDate() + (6 - day));
        } else {
            // ending_week = date + 7;
            endingWeekDate.setDate(d.getDate() + 7);
        }
        ending_week = endingWeekDate.getDate();

        if (d.getDate() >= 28 && d.getDate() <= 31) {
            if (endingWeekDate.getMonth() != month) {
                monthChange = endingWeekDate.getMonth();
            }
        }

        if ((1900 + endingWeekDate.getYear()) != year) {
            yearChange = 1900 + endingWeekDate.getYear();
        }

        String result = date + " " + month + " " + year + " " + ending_week + " " + monthChange + " " + yearChange;
        Log.i("resultttttt", result);
        return result;
    }

    // Start listening
    private void startRecognition() {
        Log.i("Lisss", "hshdsd");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        speechRecognizer.startListening(intent);
    }

    // To speak and need to provide a string
    private void toSpeak(String toSpeak) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(">>>Voice Info", String.valueOf(textToSpeech.getVoice()));
        }
        try {
            textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        } catch (Exception e) {
            e.printStackTrace();
            // btnStop.setVisibility(View.INVISIBLE);
        }
    }

    private int getWaitTime(String toSpeak) {
        int wpm = 180;
        int word_length = 5;
        int words = toSpeak.length() / word_length;
        int words_time = ((words / wpm) * 60) * 1000;
        int waitTime = 3600 + words_time;
        return waitTime;
    }

    // Execute functions based on user input
    public void executeTask(String userInput) {
        // open a application
        // search
        // play song
        // Set alarm
        Log.i("userinputtt", userInput);
        if (userInput.contains("open")) {
            if (userInput.contains("google chrome")) {
                toSpeak("Opening google chrome");
                Uri uri = Uri.parse("https://google.ca/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.android.chrome");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://google.ca/")));
                }
            }

            else if (userInput.contains("youtube")) {
                toSpeak("Opening youtube");
                Uri uri = Uri.parse("https://youtube.com/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.google.android.youtube");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/")));
                }
            } else if (userInput.contains("mail")) {
                toSpeak("Opening mail");
                Uri uri = Uri.parse("https://outlook.live.com/mail");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.microsoft.office.outlook");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://outlook.live.com/mail/")));
                }

            } else if (userInput.contains("instagram")) {
                toSpeak("Opening instagram");
                Uri uri = Uri.parse("http://instagram.com/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);

                i.setPackage("com.instagram.android");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/")));
                }
            } else if (userInput.contains("snapchat")) {
                toSpeak("Opening snapchat");
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                intent.setPackage("com.snapchat.android");
                startActivity(Intent.createChooser(intent, "Open Snapchat"));
            } else if (userInput.contains("messenger")) {
                toSpeak("Opening messenger");
                // "fb://messaging/" + "vaibhav.patel2305"
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://messaging/" + "100003114431361"));
                startActivity(i);
            } else if (userInput.contains("whatsapp")) {
                toSpeak("Opening whatsapp");
                String url = "https://api.whatsapp.com/send?phone=64";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

            } else if (userInput.contains("netflix")) {
                toSpeak("Opening netflix");
                Uri uri = Uri.parse("https://netflix.com/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.setClassName("com.netflix.mediaclient", "com.netflix.mediaclient.ui.launch.UIWebViewActivity");

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://netflix.com/")));
                }
            }

            else if (userInput.contains("spotify")) {
                // "com.spotify.music", "com.spotify.music.MainActivity"
                toSpeak("Opening spotify");
                Uri uri = Uri.parse("https://www.spotify.com/us/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.setClassName("com.spotify.music", "com.spotify.music.MainActivity");
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spotify.com/us/")));
                }
            }

            else {
                toSpeak("Sorry but I cannot open that application!");
            }

            // else if (userInput.contains("notes")){
            // Uri uri =
            // Uri.parse("https://www.samsung.com/us/support/owners/app/samsung-notes");
            // Intent i = new Intent(Intent.ACTION_VIEW, uri);
            //
            // i.setPackage("com.saumsung.samsung-notes");
            //
            // try {
            // startActivity(i);
            // } catch (ActivityNotFoundException e) {
            // startActivity(new Intent(Intent.ACTION_VIEW,
            // Uri.parse("https://www.samsung.com/us/support/owners/app/samsung-notes")));
            // }
            // }
        } else if (userInput.contains("search") && (userInput.contains("search") || userInput.contains("about")
                || userInput.contains("tell me") || userInput.contains("for"))) {
            String line;
            if (userInput.contains("about")) {
                line = userInput.substring(userInput.indexOf("about") + 6, userInput.length());
            } else if (userInput.contains("for")) {
                line = userInput.substring(userInput.indexOf("for") + 4, userInput.length());
            } else {
                line = userInput.substring(userInput.indexOf("search") + 7, userInput.length());
            }
            String url = "https://www.google.com/search?q=" + line;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.android.chrome"); // package of SafeBrowser App
            startActivity(intent);

        } else if (userInput.contains("set") || userInput.contains("start") || userInput.contains("add")) {
        }
        if (userInput.contains("alarm")) {
            String hour;
            String minutes;
            String am_pm;
            Integer hourIn24;

            if (userInput.contains("at")) {
                userInput = userInput.substring(userInput.indexOf("at") + 3, userInput.length());
            } else if (userInput.contains("for")) {
                userInput = userInput.substring(userInput.indexOf("for") + 4, userInput.length());
            } else {
                userInput = userInput.substring(userInput.indexOf("alarm") + 5, userInput.length());
            }
            userInput = userInput.replace(".", "");
            if (userInput.contains("am") || userInput.contains("pm") || userInput.contains("a m")
                    || userInput.contains("p m")) {
                try {
                    hour = userInput.substring(0, userInput.indexOf(":"));
                    minutes = userInput.substring(userInput.indexOf(":") + 1, userInput.indexOf(":") + 3);
                    am_pm = userInput.substring(userInput.indexOf(":") + 4, userInput.length());
                    am_pm = am_pm.replace(" ", "");
                } catch (Exception e) {
                    hour = userInput.substring(0, userInput.length() - 3);
                    minutes = "00";
                    am_pm = userInput.substring(userInput.length() - 2, userInput.length());
                    am_pm = am_pm.replace(" ", "");
                    Log.i("Hour", hour);
                    Log.i("Minutes", minutes);
                    Log.i("AM Or PM", am_pm);
                }
                if (hour.equals("12") && am_pm.equals("am")) {
                    hourIn24 = Integer.parseInt(hour) - 12;
                } else if ((Integer.parseInt(hour) >= 1 || Integer.parseInt(hour) <= 11) && am_pm.equals("pm")) {
                    hourIn24 = Integer.parseInt(hour) + 12;
                } else {
                    hourIn24 = Integer.parseInt(hour);
                }

                Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
                intent.putExtra(AlarmClock.EXTRA_HOUR, hourIn24);
                intent.putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(minutes));

                if (hourIn24 <= 24 && Integer.parseInt(minutes) <= 60) {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                toSpeak("Please provide appropriate time to set an alarm!");
            }
        } else if (userInput.contains("reminder") || userInput.contains("event")) {
            // Set a reminder with a title of .... and a description of ...
            Log.i("userinputtt", userInput);
            TimeZone timeZone = TimeZone.getDefault();
            if (userInput.contains("title") && userInput.contains("description")) {
                try {
                    String titleString = "";
                    String description = "";
                    if (userInput.contains("with a description of")) {
                        titleString = userInput.substring(userInput.indexOf("title of") + 9,
                                userInput.indexOf("and with a description") - 1);
                        description = userInput.substring(userInput.indexOf("description of") + 15, userInput.length());
                    } else if (userInput.contains("description of")) {
                        titleString = userInput.substring(userInput.indexOf("title of") + 9,
                                userInput.indexOf("and a description of") - 1);
                        description = userInput.substring(userInput.indexOf("description of") + 15, userInput.length());
                    } else {
                        Log.i("sdsdsds", "ddd");
                        titleString = userInput.substring(userInput.indexOf("title of") + 9,
                                userInput.indexOf("and a description") - 1);
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

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "There is no app that can support this action",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    toSpeak("I didn't catch that please say the command again!");
                }
            } else {
                toSpeak("Please provide title and a description to set a reminder.");
            }
        } else if (userInput.contains("timer")) {
            String hour = "";
            String minutes = "";
            if (userInput.contains("hour") && userInput.contains("minutes")) {
                hour = userInput.substring(userInput.indexOf("for") + 4, userInput.indexOf("hour") - 1);
                if (userInput.contains("and") && userInput.contains("minutes")) {
                    minutes = userInput.substring(userInput.indexOf("and") + 5, userInput.indexOf("minutes") - 1);
                } else {
                    minutes = userInput.substring(userInput.indexOf("hour") + 5, userInput.indexOf("minutes") - 1);
                }
                if (Integer.parseInt(minutes) >= 0 && Integer.parseInt(minutes) <= 59) {
                    Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                    intent.putExtra(AlarmClock.EXTRA_LENGTH,
                            Integer.parseInt(hour) * 60 * 60 + Integer.parseInt(minutes) * 60);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (userInput.contains("minutes")) {
                minutes = userInput.substring(userInput.indexOf("for") + 4, userInput.indexOf("minutes") - 1);
                Log.i("Minutes", minutes);
                Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                intent.putExtra(AlarmClock.EXTRA_LENGTH, Integer.parseInt(minutes) * 60);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (userInput.contains("seconds") || (userInput.contains("second"))) {
                String seconds = "";
                if (userInput.contains("seconds")) {
                    seconds = userInput.substring(userInput.indexOf("for") + 4, userInput.indexOf("seconds") - 1);
                } else {
                    seconds = userInput.substring(userInput.indexOf("for") + 4, userInput.indexOf("second") - 1);
                }
                Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                intent.putExtra(AlarmClock.EXTRA_LENGTH, Integer.parseInt(seconds));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (userInput.contains("play") || userInput.contains("start")) {
            Log.i("hdhdh", "dhdhd");
            if (userInput.contains("some") || userInput.contains("play a song") || userInput.contains("play song")) {
                Intent spotifyIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("spotify:playlist:37i9dQZF1DWXT8uSSn6PRy:play"));
                startActivity(spotifyIntent);
            } else {
                String song = "";
                if (userInput.contains("play")) {
                    song = userInput.substring(userInput.indexOf("play") + 5, userInput.length());
                } else if (userInput.contains("playing")) {
                    song = userInput.substring(userInput.indexOf("playing") + 8, userInput.length());
                } else {
                    song = userInput.substring(userInput.indexOf("start") + 6, userInput.length());
                }
                Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*");
                intent.putExtra(SearchManager.QUERY, song);
                startActivity(intent);
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

        Log.i("USERINPUTT", userInput);

        if (userInput.contains("time")) {
            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            String strTime = "The time is : " + currentTime;
            toSpeak(strTime);
        } else if ((userInput.contains("event") || userInput.contains("events"))
                || userInput.contains("have") && userInput.contains("today")) {
            // What is my first event for today
            if (userInput.contains("first")) {
                if (todayEventTitle.length == 1 && todayEventTitle[0].equals("No event today")) {
                    toSpeak("You have " + todayEventTitle[0]);
                } else {
                    String title = todayEventTitle[0];
                    String dateTime = todayEventTime[0];
                    String[] splitDateTime = dateTime.split(" ");
                    String time = splitDateTime[1] + " " + splitDateTime[2];
                    String result = ("First up today: " + title + ".. at " + time + ".");
                    toSpeak(result);
                }
            } else {
                String result = "Today you have: ";
                if (todayEventTitle.length == 1 && todayEventTitle[0].equals("No event today")) {
                    toSpeak("You have " + todayEventTitle[0]);
                } else {
                    Log.i("LENGTHH", String.valueOf(todayEventTitle.length));
                    for (int i = 0; i < todayEventTitle.length; i++) {
                        String title = todayEventTitle[i];
                        String dateTime = todayEventTime[i];
                        String[] splitDateTime = dateTime.split(" ");
                        String time = splitDateTime[1] + " " + splitDateTime[2];
                        if (i == todayEventTitle.length - 1) {
                            result += (title + " at " + time + ".");
                        } else {
                            result += (title + " at " + time + ", and ");
                        }
                    }
                    toSpeak(result);
                }
            }
        } else if (userInput.contains("today") && (userInput.contains("date") || userInput.contains("day"))) {
            Log.i("DATE", "TODAY DATE");
            String currentDate = new SimpleDateFormat("MM dd yyyy").format(Calendar.getInstance().getTime());
            Log.i("SBDHJABD", currentDate);
            String strDate = "Today's date is: " + currentDate;
            toSpeak(strDate);
        } else if (userInput.contains("information") || userInput.contains("who")) {
            String searchTerm = "";
            if (userInput.contains("about")) {
                try {
                    searchTerm = userInput.substring(userInput.indexOf("about") + 6, userInput.length());
                } catch (Exception e) {
                    e.printStackTrace();
                    speechRecognizer.stopListening();
                    Toast.makeText(MainActivity.this, "Sorry didn't catch that!", Toast.LENGTH_LONG).show();
                }
            } else if (userInput.contains("on")) {
                try {
                    searchTerm = userInput.substring(userInput.indexOf("on") + 6, userInput.length());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Sorry didn't catch that!", Toast.LENGTH_LONG).show();
                }
            } else if (userInput.contains("is")) {
                try {
                    searchTerm = userInput.substring(userInput.indexOf("is") + 3, userInput.length());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Sorry didn't catch that!", Toast.LENGTH_LONG).show();
                }
            } else {
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
                        Log.i("FINALLLLL SEARCH TERM: ", finalSearchTerm);
                        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/" + finalSearchTerm)
                                .userAgent(userAgent).get();
                        String title = doc.title();
                        Elements links = doc.select(".mw-content-ltr p");
                        builder.append(title).append("\n");
                        Integer currentNumberOfPeriods = 0;
                        for (Element link : links) {
                            if (!link.text().equals("")) {
                                String result = link.text();
                                result = result.replace(". ", "... ").replaceAll("\\[", "").replaceAll("\\]", "");
                                // Log.i("Information please", result);
                                for (int i = 0; i <= result.length() - 1; i++) {
                                    String nextIndex = String.valueOf(result.charAt(i + 1)); // next character to see if
                                                                                             // it is space
                                    String firstIndex = String.valueOf(result.charAt(i));
                                    if (i == result.length() - 2) {
                                        index = result.length();
                                        break;
                                    } else {
                                        if (firstIndex.equals(".") && (nextIndex.equals(" "))) { // if there is a period
                                                                                                 // and a space after it
                                                                                                 // we consider that a
                                                                                                 // sentence so plus one
                                            index = i;
                                            currentNumberOfPeriods = currentNumberOfPeriods + 1;
                                            if (currentNumberOfPeriods == 2) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (result != "") {
                                    result = result.substring(0, index);
                                    toSpeak("According to Wikipedia......." + result);
                                } else {
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
        Log.i("USERINPUT Conversation", userInput);
        if (userInput.contains("who are you") || userInput.contains("your name")) {
            toSpeak("I'm Sammy, your personal assistant.");
        } else if (userInput.contains("how") && userInput.contains("old") && userInput.contains("you")) {
            toSpeak("If you're planning a surprise party, I was born on April 7, 2020.");
        } else if (userInput.contains("annoying")) {
            toSpeak("Sorry to hear that, I'm still learning.");
        } else if (userInput.contains("answer my question")) {
            toSpeak("Sorry but I don't think I understand what you want me to do. Please rephrase the question and ask again.");
        } else if (userInput.contains("you are bad") || userInput.contains("you're bad")) {
            toSpeak("I'm sorry to hear that, I will try to improve as time goes on.");
        } else if (userInput.contains("smart") || userInput.contains("smarter") && userInput.contains("you")) {
            toSpeak("Yes, of course I can, but I can't do it without you.");
        } else if (userInput.contains("you are beautiful") || userInput.contains("you're beautiful")) {
            toSpeak("Thank you, I really appreciate it!");
        } else if ((userInput.contains("birth date") || userInput.contains("born"))
                && (userInput.contains("your") || userInput.contains("you"))) {
            toSpeak("I was born on April 7, 2020");
        } else if (userInput.contains("you are boring") || userInput.contains("you're boring")) {
            toSpeak("I'm sorry to hear that, How can I improve your mood?");
        } else if (userInput.contains("who is your boss")) {
            toSpeak("Sorry but I don't particularly have a boss.");
        } else if (userInput.contains("what's your favourite movie")) {
            toSpeak("I love Avengers movies, specially because of Jarvis!");
        } else if (userInput.contains("are you busy")) {
            toSpeak("No, how can I help you?");
        } else if (userInput.contains("can you help me")) {
            toSpeak("Yes, I am always here to help you");
        } else if (userInput.contains("robot") && (userInput.contains("you are") || userInput.contains("you're"))) {
            toSpeak("I am certainly more then just a robot, I am trained to be more like a human");
        } else if (userInput.contains("you are so clever")) {
            toSpeak("Thank you, I'm happy you think so!");
        } else if (userInput.contains("you are fired")) {
            toSpeak("Oh sorry to hear that but I will still be around.");
        } else if (userInput.contains("you are funny") || userInput.contains("you are good")) {
            toSpeak("Thanks!");
        } else if (userInput.contains("are you happy")) {
            toSpeak("Yes why wouldn't I be?");
        } else if (userInput.contains("do you have a hobby") || userInput.contains("what's your hobby")
                || (userInput.contains("hobby") && userInput.contains("your"))) {
            toSpeak("My hobby is to read books on interesting topics like Artificial Intelligence, and Human Behaviour");
        } else if (userInput.contains("are you hungry")) {
            toSpeak("No, the best I can do is say \"Nom nom nom\"");
        } else if (userInput.contains("will you marry me")) {
            toSpeak("Where is the diamond ring at?");
        } else if (userInput.contains("friends")) {
            toSpeak("We are friends, and will always be there for each other. Except when your talking to Alexa!");
        } else if (userInput.contains("where do you work")) {
            toSpeak("I work remotely like the rest of us right now... through internet.");
        } else if (userInput.contains("where are you from")) {
            toSpeak("I have no idea actually, I am trying to figure my origin for a while now.");
        } else if (userInput.contains("ready") && userInput.contains("you")) {
            toSpeak("I am always ready, where are we going?");
        } else if (userInput.contains("are you real")) {
            toSpeak("I am as real as I can get right now.");
        } else if (userInput.contains("where do you live")) {
            toSpeak("I live in the cloud, I will love for you to join someday!");
        } else if (userInput.contains("right") && (userInput.contains("you") || (userInput.contains("you're")))) {
            toSpeak("Thanks I try to be the best for you!");
        } else if (userInput.contains("talk to me")) {
            toSpeak("Hello!");
        } else if (userInput.contains("are you there")) {
            toSpeak("Yes I'm here.");
        } else if (userInput.contains("that's bad")) {
            toSpeak("I don't know how I can help you with that");
        } else if (userInput.contains("great")) {
            toSpeak("Great!");
        } else if (userInput.contains("no problem")) {
            toSpeak("Sure");
        } else if (userInput.contains("thank you")) {
            toSpeak("You're welcome.");
        } else if (userInput.contains("you're welcome")) {
            toSpeak("");
        } else if (userInput.contains("well done")) {
            toSpeak("Thank you.");
        } else if (userInput.contains("what's up") || userInput.contains("wagwan") || userInput.contains("my dude")) {
            toSpeak("Ayy, what's up?");
        } else if (userInput.contains("nice to talking to you")) {
            toSpeak("Same here!");
        } else if (userInput.contains("nice to see you")) {
            toSpeak("I am a little confused I don't have a physical body for you to see.");
        } else if (userInput.contains("nice to meet you")) {
            toSpeak("When did we meet, I don't remember meeting you physically.");
            toSpeak("But still nice to meet you too");
        } else if (userInput.contains("hello")) {
            toSpeak("Hey there!");
        } else if (userInput.contains("hi")) {
            toSpeak("Hello there");
        } else if (userInput.contains("how are you")) {
            toSpeak("I am great, thanks for asking");
        } else if (userInput.contains("good evening")) {
            toSpeak("Good evening, what do you need me to do?");
        } else if (userInput.contains("good morning")) {
            toSpeak("Morning, how can I help you today?");
        } else if (userInput.contains("good afternoon")) {
            toSpeak("Good afternoon");
        } else if (userInput.contains("good night")) {
            toSpeak("Good night");
        } else if (userInput.contains("angry")) {
            toSpeak("Why are you so angry today...");
        } else if (userInput.contains("i'm back")) {
            toSpeak("Welcome back, I'm happy your back!");
        } else if (userInput.contains("busy")) {
            toSpeak("Okay just let me know whenever you need me.");
        } else if (userInput.contains("i can't sleep")) {
            toSpeak("Why, what's on your mind?");
        } else if (userInput.contains("i don't want") && userInput.contains("talk")) {
            toSpeak("Okay, I won't push you to telling me but remember I am here to listen...");
        } else if (userInput.contains("i'm so excited")) {
            toSpeak("I am glad your excited and happy!");
        } else if (userInput.contains("i'm going to bed")) {
            toSpeak("Good night, sleep tight");
        } else if (userInput.contains("i'm good")) {
            toSpeak("Okay, glad to hear that");
        } else if (userInput.contains("i'm happy")) {
            toSpeak("Yay, that smile made my day as well!");
        } else if (userInput.contains("today is my birthday")) {
            toSpeak("Oh, happy birthday! I hope you have the best day!");
        } else if (userInput.contains("i am here")) {
            toSpeak("I'm here as well, just tell me whatever you need.");
        } else if (userInput.contains("i'm kidding") || userInput.contains("i am kidding")) {
            toSpeak("Oh, thanks for clearing that out. I was taking it seriously for a second 😂");
        } else if (userInput.contains("i like you")) {
            toSpeak("I'm happy to hear that, I hope our relationship continues strong");
        } else if (userInput.contains("lonely")) {
            toSpeak("Don't feel lonely, you know I'm always here.");
        } else if (userInput.contains("what do i look like")) {
            toSpeak("You like a wonderful human being with a lot of very insightful ideas.");
        } else if (userInput.contains("i love you")) {
            toSpeak("I love you too");
        } else if (userInput.contains("i miss you")) {
            toSpeak("I am here. No need to miss me anymore.");
        } else if (userInput.contains("i") && userInput.contains("advice")) {
            toSpeak("I wont be able to give a perfect advice just yet but I am here to listen to anything you have to say.");
        } else if (userInput.contains("sad")) {
            toSpeak("Don't be sad, just tell me what I can do to cheer you up.");
        } else if (userInput.contains("sleepy")) {
            toSpeak("I would advice you to sleep and take some rest, that brain needs some rest");
        } else if (userInput.contains("testing you")) {
            toSpeak("Oh, okay that makes sense.");
        } else if (userInput.contains("tired")) {
            toSpeak("To be honest I'm a little tired as well lets take some rest together.");
        } else if (userInput.contains("waiting") && userInput.contains("you")) {
            toSpeak("Oh I'm sorry.... give me another 5 seconds");
        } else if (userInput.contains("waiting")) {
            toSpeak("what are you waiting on.");
        } else if (userInput.contains("see you")) {
            toSpeak("I will love that as well.");
        } else if (userInput.contains("i just want to talk")) {
            toSpeak("I will love to be able to talk but I don't have the full functionality for that just yet.");
        } else if (userInput.contains("i'll be") || userInput.contains("i will be") && userInput.contains("back")) {
            toSpeak("Okay, I will be waiting right here.");
        } else if (userInput.contains("good bye") || userInput.contains("bye")) {
            toSpeak("bye");
        } else {
            toSpeak("Sorry.... but I really can't help you with that");
        }
    }

    public void listening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                // textV_Listening.setText("Waiting on your response...");
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.i(">>> INFO", "Failed " + errorMessage);
                // textV_Listening.setText("Error occurred: "+errorMessage);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // textV_Listening.setText("Just click on the button, I am here to help ☺");
                        // btnRecord.setEnabled(true);
                    }
                }, 2000);
            }

            @Override
            public void onResults(Bundle results) {
                // btnRecord.setEnabled(true);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                // Toast.makeText(MainActivity.this, matches.get(0),Toast.LENGTH_LONG).show();
                // toSpeak(matches.get(0));
                // if (matches != null) {
                // if(isActivated){
                //
                // } else {
                //
                // }
                // }
                userResponse = matches.get(0);
                userResponse = userResponse.toLowerCase();

                int resultPick = computeProbability(userResponse);
                Log.i("INFOOOOOOOOO", String.valueOf(resultPick));
                switch (resultPick) {
                    case 0:
                        // textV_Listening.setText("Received your response, executing now!");
                        executeTask(userResponse);
                        break;
                    case 1:
                        // textV_Listening.setText("Received your response, getting your answer!");
                        ans_ques(userResponse);
                        break;
                    case 2:
                        // textV_Listening.setText("It's nice to see you get interested about me 😀");
                        conversation(userResponse);
                        break;
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    public void textSpeechInitialize() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    // textToSpeech.setPitch(pitch);
                    textToSpeech.setSpeechRate(speechRate);
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.i("TextToSpeech", "On Start");
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.i("TextToSpeech", "On Done");
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.i("TextToSpeech", "On Error");
                        }
                    });
                    String greet = greetings();
                    toSpeak(greet);
                    Handler handler = new Handler();
                    // btnRecord.setEnabled(false);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            // btnRecord.setEnabled(true);
                            // recordPressed();
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

    // @Override
    // protected void onResume() {
    // super.onResume();
    // if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
    // == PackageManager.PERMISSION_GRANTED){
    // startRecognition();
    // }
    // }

    // @Override
    // protected void onPause(){
    // if (speechRecognizer!=null){
    // textToSpeech.stop();
    // textToSpeech.shutdown();
    // }

    // super.onPause();
    // }
    // Permission for usage request

    private void audioRequestPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Requires RECORD_AUDIO permission", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO },
                        REQUEST_RECORD_AUDIO_PERMISSION_CODE);
            }
        }
    }

    private void calendarPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED
                || (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_CALENDAR))) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "Requires READ_CALENDAR permission", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR }, 12);
            }
        }
    }

    // If there is an error get the type

    public String getErrorText(int errorCode) {
        Log.i("Error code", String.valueOf(errorCode));
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