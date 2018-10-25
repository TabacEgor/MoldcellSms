package com.example.tabac.moldcellsms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {
    Button btnSend;
    ImageView imageView;
    TextInputEditText txtInputNumber, txtInputFrom, txtInput, txtInputCaptcha;
    String imgSrc;
    String captcha_sid = "";
    String captcha_token = "";
    String form_build_id = "";
    String form_id = "";

    final String targetURL = "https://www.moldcell.md/sendsms";
    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSend = findViewById(R.id.btnSend);
        imageView = findViewById(R.id.imageView2);
        txtInputNumber = findViewById(R.id.txtInpuntNumber);
        txtInputFrom = findViewById(R.id.txtInputFrom);
        txtInput = findViewById(R.id.txtInput);
        txtInputCaptcha = findViewById(R.id.txtInputCaptcha);
        ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
        handleSSLHandshake();
        getWeb();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!txtInputNumber.getText().toString().isEmpty() && !txtInputFrom.getText().toString().isEmpty()
                        && !txtInput.getText().toString().isEmpty() && !txtInputCaptcha.getText().toString().isEmpty()){
                    RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                    StringRequest postRequest = new StringRequest(Request.Method.POST, targetURL, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            addLog(response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("htmlTAG", String.valueOf(error));
                        }
                    }
                    ) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String>  params = new HashMap<String, String>();
                            params.put("phone", txtInputNumber.getText().toString());
                            params.put("name", txtInputFrom.getText().toString());
                            params.put("message", txtInput.getText().toString());
                            params.put("captcha_sid", captcha_sid);
                            params.put("captcha_token", captcha_token);
                            params.put("captcha_response", txtInputCaptcha.getText().toString());
                            params.put("conditions", "1");
                            params.put("op", "");
                            params.put("form_build_id", form_build_id);
                            params.put("form_id", form_id);

                            return params;
                        }

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String,String> params = new HashMap<String,String>();
                            params.put("Content-Type","application/x-www-form-urlencoded");
                            return params;
                        }
                    };
                    Log.d("TAG",postRequest.getBodyContentType());
                    Log.d("TAG",postRequest.getCacheKey());

                    requestQueue.add(postRequest);

                    getWeb();
                }
            }
        });

    }

    private void addLog(String response) {
        File logRequest = new File(Environment.getExternalStorageDirectory().toString());
        if (!logRequest.exists()){
            try {
                logRequest.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logRequest,true));
            bufferedWriter.append(response);
            bufferedWriter.newLine();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getWeb(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest getRequest = new StringRequest(Request.Method.GET, targetURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        org.jsoup.nodes.Document document = (org.jsoup.nodes.Document) Jsoup.parse(response);
//                        Document document = (Document) Jsoup.parse(response);

                        Elements form = ((org.jsoup.nodes.Document) document).getElementsByClass("websms-form");
                        for (org.jsoup.nodes.Element f : form) {
                            for (org.jsoup.nodes.Element e : f.getElementsByAttributeValue("name", "captcha_sid"))
                                captcha_sid = e.attr("value");
                            for (org.jsoup.nodes.Element e : f.getElementsByAttributeValue("name", "captcha_token"))
                                captcha_token = e.attr("value");
                            for (org.jsoup.nodes.Element e : f.getElementsByAttributeValue("name", "form_build_id"))
                                form_build_id = e.attr("value");
                            for (org.jsoup.nodes.Element e : f.getElementsByAttributeValue("name", "form_id"))
                                form_id = e.attr("value");
                            for (org.jsoup.nodes.Element img : f.getElementsByTag("img"))
                                imgSrc = img.attr("src");
                        }
                        Log.d("smsForm", captcha_sid);
                        Log.d("smsForm", captcha_token);
                        Log.d("smsForm", form_build_id);
                        Log.d("smsForm", form_id);

                        try {
                            URL url = new URL("https://www.moldcell.md"+ imgSrc);
                            new GetCaptcha().execute(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("smsForm", error.toString());
            }
    });
        requestQueue.add(getRequest);
    }
    private class GetCaptcha extends AsyncTask<URL, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(URL... urls) {
            int count = urls.length;
            Bitmap bitmap = null;
            for (int i = 0; i < count; i++){
                try {
                    bitmap = BitmapFactory.decodeStream(urls[i].openConnection().getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (isCancelled()) break;
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }

}
