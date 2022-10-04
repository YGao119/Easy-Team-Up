package com.example.easyteamupfrontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitInterface {
    @POST("signup")
    Call<Void> executeSignup(@Body HashMap<String, String> map);

    @POST("login")
    Call<LoggedInUser> executeLogin(@Body HashMap<String, String> map);

    @GET("users/{id}")
    Call<Void> executeVerifyUserExistence(@Path("id") String id);

    @POST("events")
    Call<Void> executeCreateEvent(@Body HashMap<String, String> map);

    @GET("events")
    Call<List<Event>> retrieveAllEvents();

    @GET("users/{id}/invited_events")
    Call<List<Event>> retrieveInvitedEvents(@Path("id") String id);

    @GET("users/{id}/joined_events")
    Call<List<Event>> retrieveJoinedEvents(@Path("id") String id);

    @POST("withdraw_event")
    Call<Void> executeWithDraw(@Body HashMap<String, String> map);

    @POST("reject_event")
    Call<Void> executeReject(@Body HashMap<String, String> map);

    @POST("update_event")
    Call<Void> executeModifyEvent(@Body HashMap<String, String> map);

    @POST("signup_event")
    Call<Void> executeSignupEvent(@Body HashMap<String, String> map);

    @GET("determined_times")
    Call<List<Map<String, String>>> getDeterminedTimes();

    @GET("get_notifications/{id}")
    Call<List<Map<String, String>>> getNotifications(@Path("id") String id);

    @POST("add_notification")
    Call<Void> addNotification(@Body HashMap<String, String> map);

    @POST("edit_profile")
    Call<Void> executeEditProfile(@Body HashMap<String, String> map);


}