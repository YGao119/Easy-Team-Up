package com.example.easyteamupfrontend.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.easyteamupfrontend.Event;
import com.example.easyteamupfrontend.R;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.databinding.FragmentNotificationsBinding;
import com.example.easyteamupfrontend.ui.notifications.adapter.EventHistoryAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.AddressComponents;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private Fragment fragment = this;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private final ActivityResultLauncher<Intent> startAutocomplete = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResultCallback<ActivityResult>) result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Place place = Autocomplete.getPlaceFromIntent(intent);
                        fillInAddress(place);
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    // The user canceled the operation.
                }
            });
    private final String BASE_URL = UserSession.BASE_URL;

    private ListView listView;
    private View root;
    private String userId = UserSession.id;

    private List<Event> allEvents = new ArrayList<Event>();
    private List<Event> invitations = new ArrayList<Event>();
    private List<Event> history = new ArrayList<Event>();
    private List<Event> joined = new ArrayList<Event>();
    private ArrayAdapter<Event> listViewAdapter = null;
    LayoutInflater inflater;
    ViewGroup container;
    Bundle savedInstanceState;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);


        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        // display list of history events
        init(inflater, container, savedInstanceState);
        return root;
    }

    public void refresh(){
        init(inflater, container, savedInstanceState);
    }

    private void init(LayoutInflater inflater,
                      ViewGroup container, Bundle savedInstanceState) {
        history = new ArrayList<>();
        listView = root.findViewById(R.id.event_history);
        listView.setAdapter(null);
        Call<List<Event>> call3 = retrofitInterface.retrieveJoinedEvents(UserSession.id);
        call3.enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                invitations = response.body();
                System.out.println("1:"+response.body());
                if(invitations != null){
                    UserSession.joined_events = (ArrayList<Event>) invitations;
                    for(Event e : invitations){
                        history.add(e);
                    }
                }
                Call<List<Event>> call2 = retrofitInterface.retrieveInvitedEvents(UserSession.id);
                call2.enqueue(new Callback<List<Event>>() {
                    @Override
                    public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                        invitations = response.body();
                        System.out.println("2:"+response.body());
                        if(invitations != null){
                            for(Event e: invitations) {
                                System.out.println("invited: " + e);
                                history.add(e);
                            }
                        }
                        Call<List<Event>> call1 = retrofitInterface.retrieveAllEvents();
                        call1.enqueue(new Callback<List<Event>>() {
                            @Override
                            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                                allEvents = response.body();
                                if(allEvents != null) {
                                    for (Event e : allEvents) {
                                        if (e.getOwnerId().equals(UserSession.id)) {
                                            history.add(e);
                                        }
                                    }
                                }
                                Call<List<Map<String, String>>> call0 = retrofitInterface.getDeterminedTimes();
                                call0.enqueue(new Callback<List<Map<String, String>>>() {
                                    @Override
                                    public void onResponse(Call<List<Map<String, String>>> call, Response<List<Map<String, String>>> response) {
                                        List<Map<String, String>> times = response.body();
                                        UserSession.determinedTimes = times;
                                    }

                                    @Override
                                    public void onFailure(Call<List<Map<String, String>>> call, Throwable t) {
                                    }
                                });

                                System.out.println("3:"+history);
                                history = dedupHistory(history);
                                inflateListView(history, userId, inflater, container, savedInstanceState, fragment);
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
            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
            }
        });
    }

    private int getRank(Event event){
        if(userId.equals(event.getOwnerId())) {
            return 0;
        }
        else if(UserSession.joined(event)){
            return 1;
        }
        else{
            return 2;
        }
    }

    private List<Event> dedupHistory(List<Event> history) {
        Set<String> visited = new HashSet<>();
        List<Event> result = new ArrayList<>();
        for(Event e : history){
            if(!visited.contains(e.getName())){
                visited.add(e.getName());
                result.add(e);
            }
        }
        result.sort(new Comparator<Event>() {
            @Override
            public int compare(Event event, Event t1) {
                return Integer.compare(getRank(event), getRank(t1));
            }
        });
        return result;
    }


    private void inflateListView(List<Event> events, String userId, LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState, Fragment fragment) {
        listViewAdapter = new EventHistoryAdapter(startAutocomplete, root.getContext(), events, userId, inflater, container, savedInstanceState, retrofitInterface, fragment);
        listView.setAdapter(listViewAdapter);
    }

    private void fillInAddress(Place place) {
        AddressComponents components = place.getAddressComponents();
        StringBuilder builder = new StringBuilder();

        if (components != null) {
            for (AddressComponent component : components.asList()) {
                String type = component.getTypes().get(0);
                if (type.equals("street_number")) {
                    builder.insert(0, component.getName());
                } else if (type.equals("route")) {
                    builder.append(" ");
                    builder.append(component.getShortName());
                }
            }
        }
        UserSession.event.setCoordinate(place.getLatLng());
        UserSession.event.setAddress(builder.toString());
        UserSession.address_edit.setText(builder.toString());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}