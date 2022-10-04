package com.example.easyteamupfrontend;


import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class NotificationThread extends Thread {
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private List<Map<String, String>> notifications;
    private int reqCode = 1;
    private Thread t;
    private List<Event> events;

    NotificationThread() {
        retrofit = new Retrofit.Builder().baseUrl(UserSession.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
    }

    public void showNotification(Context context, String title, String message, Intent intent, int reqCode) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, reqCode, intent, PendingIntent.FLAG_ONE_SHOT);
        String CHANNEL_ID = "channel_name";// The id of the channel.
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationManager.notify(reqCode, notificationBuilder.build()); // 0 is the request code, it should be unique id

    }

    public void getEvents(){

        Call<List<Event>> call = retrofitInterface.retrieveJoinedEvents(UserSession.id);
        call.enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                ArrayList<Event> joined = (ArrayList<Event>) response.body();
                System.out.println(response.body());
                if(joined != null){
                    UserSession.joined_events = (ArrayList<Event>) joined;
                }
                Call<List<Event>> call1 = retrofitInterface.retrieveAllEvents();
                call1.enqueue(new Callback<List<Event>>() {
                    @Override
                    public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                        events = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<Event>> call, Throwable t) {
                    }
                });
            }
            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
            }
        });


    }

    public void run() {
        while (true){
            Call<List<Map<String, String>>> call = retrofitInterface.getNotifications(UserSession.id);
            call.enqueue(new Callback<List<Map<String, String>>>() {
                @Override
                public void onResponse(Call<List<Map<String, String>>> call, Response<List<Map<String, String>>> response) {
                    notifications = response.body();
                    if(notifications==null || notifications.size() == 0){
                        return;
                    }
                    for(Map<String, String> notification: notifications){
                        System.out.println(notification);
                        sendNotification(notification);
                        getEvents();
                        for(Event e: events){
                            if(e.getOwnerId().equals(UserSession.id) || UserSession.joined(e)) {
                                try {
                                    LocalDateTime dueDateTime = LocalDateTime.parse(e.getDueDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                                    LocalDateTime now = LocalDateTime.now();
                                    long secs = ChronoUnit.MINUTES.between(dueDateTime, now);
                                    if(Math.abs(secs) < 1){
                                        Map<String, String> map = new HashMap<>();
                                        map.put("occasion", "determine");
                                        map.put("eventName", e.getName());
                                        map.put("info", UserSession.determinedTime(e));
                                        sendNotification(map);
                                    }
                                }
                                catch (Exception e1){}
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, String>>> call, Throwable t) {
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotification(Map<String, String> notification) {
        String occasion = notification.get("occasion");
        String eventName = notification.get("eventName");
        String info = notification.get("info");
        String message = "";
        String title = "";

        if(occasion.equals("accept")){
            title = "Event Accepted";
            message = "User: "+info+" has accepted invitation of Event: "+ eventName + ".";
        } else if (occasion.equals("decline")){
            title = "Event Declined";
            message = "User: "+info+" has rejected invitation of Event: "+ eventName + ".";
        } else if (occasion.equals("withdraw")){
            title = "Event Signup";
            message = "User: "+info+" withdraws signup of Event: "+eventName;

        } else if (occasion.equals("signup")){
            title = "Event Withdraw";
            message = "User: "+info+" withdraws from Event: "+eventName;
        } else if (occasion.equals("determine")){
            title = "Event Time Determined";
            message = "Event: "+eventName+" will take place at "+info+".";

        } else if (occasion.equals("change")){
            title = "Event Changed";
            message = "Event: "+eventName+" has been changed, check it out!";

        } else if (occasion.equals("invite")){
            title = "Event Invitation";
            message = "User: "+info+" invited you to join Event: "+eventName;
        }

        showNotification(UserSession.currentContext, title,
            message,
            new Intent(UserSession.currentContext, MainActivity.class),
            reqCode);
    }

    public void start () {
        System.out.println("Starting Notification");
        if (t == null) {
            t = new Thread (this, "Notification");
            t.start ();
        }
    }


}