package com.example.androidexample;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class QueueApplication extends Application {
    private static RequestQueue queue;
    private static String sessionId;
    @Override
    public void onCreate(){
        super.onCreate();
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cm);

        queue = Volley.newRequestQueue(getApplicationContext());
    }
    public static RequestQueue getQueue(){
        return queue;
    }

    public static void setSessionId(String id){
        sessionId = id;
    }

    public static String getSessionId(){
        return sessionId;
    }


}
