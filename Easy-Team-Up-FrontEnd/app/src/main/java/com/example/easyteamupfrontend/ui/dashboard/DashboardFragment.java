package com.example.easyteamupfrontend.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.easyteamupfrontend.Event;
import com.example.easyteamupfrontend.R;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.ui.dashboard.adapter.EventListAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DashboardFragment extends Fragment{

    private ListView listView;
    private View v;
    private boolean map_mode_on = false;
    private ArrayAdapter<Event> listViewAdapter = null;
    private List<Event> events;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        Button map_mode_switch = v.findViewById(R.id.map_mode_button);
        retrofit = new Retrofit.Builder().baseUrl(UserSession.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
        Fragment fragment = this;
        map_mode_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(getActivity(), MapsActivity.class);
                UserSession.currentFragment = fragment;
                startActivity(in);
            }
        });
        getEvents();
        return this.v;
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
                    System.out.println("joined: " +joined);
                }
                Call<List<Event>> call1 = retrofitInterface.retrieveAllEvents();
                call1.enqueue(new Callback<List<Event>>() {
                    @Override
                    public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                        events = response.body();
                        System.out.println("dash: " +events);
                        events.removeIf(event -> event.getPublicity().equals("private"));
                        events.removeIf(event -> event.getOwnerId().equals(UserSession.id));
                        events.removeIf(event -> UserSession.joined(event));
                        events.removeIf(event -> event.dueDatePassed());
                        System.out.println("filtered: " +events);
                        init();
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

    private void init() {
        listView = v.findViewById(R.id.event_list);
        listView.setAdapter(null);
        if (events != null && events.size() != 0) {
            inflateListView(events);
        }
    }

    public void refresh(){
        getEvents();
    }

    private void inflateListView(List<Event> events) {
        Fragment currentFragment = this;
        listViewAdapter = new EventListAdapter(v.getContext(), events, currentFragment);
        listView.setAdapter(listViewAdapter);
    }
}