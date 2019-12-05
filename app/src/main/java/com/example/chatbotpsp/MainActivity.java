package com.example.chatbotpsp;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.example.chatbotpsp.apibot.ChatterBot;
import com.example.chatbotpsp.apibot.ChatterBotFactory;
import com.example.chatbotpsp.apibot.ChatterBotSession;
import com.example.chatbotpsp.apibot.ChatterBotType;
import com.example.chatbotpsp.apibot.Utils;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements OnInitListener {

    private TextToSpeech mensaje;
    private Button btHablar;

    private EditText etMensaje;
    private TextView tvBot, tvYo;
    private String noTraducido, traducido;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        initEvents();
    }

    private void initComponents() {
        mensaje = new TextToSpeech(this, this);
        btHablar = findViewById(R.id.btHablar);

        etMensaje = findViewById(R.id.etMensaje);
        tvBot = findViewById(R.id.tvBot);
        tvYo = findViewById(R.id.tvYo);
    }

    private void initEvents() {

        btHablar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noTraducido = etMensaje.getText().toString();
                tvYo.setText(noTraducido);
                etMensaje.setText("");
                TraduccionIngles ingles = new TraduccionIngles(noTraducido);
                ingles.execute();
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                inputMethodManager.hideSoftInputFromWindow(etMensaje.getWindowToken(), 0);
            }
        });


    }

    //Para que el bot hable
    @Override
    protected void onDestroy() {
        if (mensaje != null) {
            mensaje.stop();
            mensaje.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mensaje.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.v("onInit", "El idioma no está disponible");
            } else {
                decir();
            }
        } else {
            Log.v("onInit", " TextToSpeech no funciona");
        }
    }

    private void decir() {
        String texto = etMensaje.getText().toString();
        mensaje.speak(texto, TextToSpeech.QUEUE_FLUSH, null);
    }

    //Para hablar con el bot
    private String chat(String s) {
        String bot = "";
        try {
            //creamos al bot y le pasamos el mensaje en s
            ChatterBotFactory factory = new ChatterBotFactory();

            ChatterBot bot1 = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
            ChatterBotSession bot1session = bot1.createSession();

            bot = bot1session.think(s);

            TraduccionEspanol espanol = new TraduccionEspanol(bot);
            espanol.execute();
        } catch (Exception e) {
        }
        return bot;
    }

    private class Chat extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String chat = chat(traducido);
            return chat;
        }
    }

    public String decomposeJson(String json) {
        String translationResult = "Could not get";
        try {
            JSONArray arr = new JSONArray(json);
            JSONObject jObj = arr.getJSONObject(0);
            translationResult = jObj.getString("translations");
            JSONArray arr2 = new JSONArray(translationResult);
            JSONObject jObj2 = arr2.getJSONObject(0);
            translationResult = jObj2.getString("text");
        } catch (JSONException e) {
            translationResult = e.getLocalizedMessage();
        }
        return translationResult;
    }

    private class TraduccionIngles extends AsyncTask<Void, Void, Void> {
        private final Map<String, String> headers;
        private final Map<String, String> vars;
        String mensajeIngles = "Error al traducir al ingles";

        private TraduccionIngles(String mensajeEsp) {
            headers = new LinkedHashMap<String, String>();
            headers.put("Content-type", "application/x-www-form-urlencoded");
            headers.put("User-Agent:", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");

            vars = new HashMap<String, String>();
            vars.put("fromLang", "es");
            vars.put("text", mensajeEsp);
            vars.put("to", "en");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                //traduce al ingles
                mensajeIngles = Utils.performPostCall("https://www.bing.com/ttranslatev3", (HashMap) vars);
            } catch (Exception e) {
                e.printStackTrace();
                Log.v("xyz", "Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            traducido = decomposeJson(mensajeIngles);
            new Chat().execute();
        }

    }

    private class TraduccionEspanol extends AsyncTask<Void, Void, Void> {

        private final Map<String, String> headers;
        private final Map<String, String> vars;
        String mensajeEspanol = "Error";

        private TraduccionEspanol(String mensajeEng) {
            headers = new LinkedHashMap<String, String>();
            headers.put("Content-type", "application/x-www-form-urlencoded");
            headers.put("User-Agent:", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");

            vars = new HashMap<String, String>();
            vars.put("fromLang", "en");
            vars.put("text", mensajeEng);
            vars.put("to", "es");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                mensajeEspanol = Utils.performPostCall("https://www.bing.com/ttranslatev3", (HashMap) vars);
            } catch (Exception e) {
                e.printStackTrace();
                Log.v("xyz", "Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            traducido = decomposeJson(mensajeEspanol);
            tvBot.setText(traducido);
            mensaje.speak(traducido, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

}

