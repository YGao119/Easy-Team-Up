package com.example.easyteamupfrontend.util;

import android.os.StrictMode;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClient {
    public static String BASE_URL = "http://54.193.68.101";
    public static void testHttp() throws IOException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(HttpClient.BASE_URL + "/events")
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();
        System.out.println( response.body().string());
    }
}
