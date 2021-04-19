package com.example.aimark2app;

//public class JsoupAsyncTask extends AsyncTask<String, Void, String> {
//    String para = "he";
//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//    }
//    @Override
//    protected String doInBackground(String... voids) {
//        String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.87 Safari/537.36";
//        try {
//            Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/Hydrogen").userAgent(userAgent).get();
//            Elements paragraphs=doc.select(".mw-content-ltr p");
//            Element firstParagraph = paragraphs.first();
//            para += firstParagraph.text();
//            Log.i(">>>WIKI", "Hello" + para);
//            return para;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return "Failed";
//    }
//    @Override
//    protected void onPostExecute(String result) {
//        Log.i(">>>>>>WIKI__INFO", result);
//    }
//}


//public class Something implements TextToSpeech.OnInitListener{
//
//
//    private boolean initialized;
//    private String queuedText;
//    private String TAG = "TTS";
//    private TextToSpeech tts;
//
//
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//
//
//        tts = new TextToSpeech(this /* context */, this /* listener */);
//        tts.setOnUtteranceProgressListener(mProgressListener);
//
//
//        speak("hello world");
//
//    }
//
//
//
//
//    public void speak(String text) {
//
//        if (!initialized) {
//            queuedText = text;
//            return;
//        }
//        queuedText = null;
//
//        setTtsListener(); // no longer creates a new UtteranceProgressListener each time
//        HashMap<String, String> map = new HashMap<String, String>();
//        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
//        tts.speak(text, TextToSpeech.QUEUE_ADD, map);
//    }
//
//
//    private void setTtsListener() {
//
//    }
//
//
//
//
//
//    @Override
//    public void onInit(int status) {
//        if (status == TextToSpeech.SUCCESS) {
//            initialized = true;
//            tts.setLanguage(Locale.ENGLISH);
//
//            if (queuedText != null) {
//                speak(queuedText);
//            }
//        }
//    }
//
//
//
//    private abstract class runnable implements Runnable {
//    }
//
//
//
//
//    private UtteranceProgressListener mProgressListener = new UtteranceProgressListener() {
//        @Override
//        public void onStart(String utteranceId) {
//        } // Do nothing
//
//        @Override
//        public void onError(String utteranceId) {
//        } // Do nothing.
//
//        @Override
//        public void onDone(String utteranceId) {
//
//            new Thread()
//            {
//                public void run()
//                {
//                    Something.this.runOnUiThread(new runnable()
//                    {
//                        public void run()
//                        {
//
//                            Toast.makeText(getBaseContext(), "TTS Completed", Toast.LENGTH_SHORT).show();
//
//                        }
//                    });
//                }
//            }.start();
//
//        }
//    };
//}
