package com.example.easyteamupfrontend.ui.dashboard;

import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easyteamupfrontend.Event;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.databinding.ActivityMapsBinding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.easyteamupfrontend.R;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class EventOnMap implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final int id;

    public EventOnMap(double lat, double lng, String title, String snippet, int id) {
        position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }
}

class CustomRenderer<T extends ClusterItem> extends DefaultClusterRenderer<T> {
    public CustomRenderer(Context context, GoogleMap map, ClusterManager<T> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<T> cluster) {
        //start clustering if at least 2 items overlap
        return cluster.getSize() > 1;
    }
}

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private List<Marker> markers;
    private ClusterManager<EventOnMap> clusterManager;
    private List<Event> events;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        retrofit = new Retrofit.Builder().baseUrl(UserSession.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Button back_button = this.findViewById(R.id.back_button);
        back_button.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                ((DashboardFragment)UserSession.currentFragment).refresh();
                onBackPressed();
            }
        });
        getEvents();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        double latitude = 0;
        double longtitude = 0;
        clusterManager = new ClusterManager<EventOnMap>(this, mMap);
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
        for(int i = 0; i < events.size(); i++){
            Event e = events.get(i);
            LatLng latLng = new LatLng(e.getCoordinate().latitude, e.getCoordinate().latitude);
            latitude += e.getCoordinate().latitude;
            longtitude += e.getCoordinate().longitude;
            MarkerOptions marker = new MarkerOptions().position(latLng).title(e.getName()).snippet(Integer.toString(i));
            //mMap.addMarker(marker);
            clusterManager.addItem(new EventOnMap(e.getCoordinate().latitude, e.getCoordinate().longitude, e.getName(), "", i));
        }
        latitude /= events.size();
        longtitude /= events.size();
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 150,150,0);
        CameraUpdate cu = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(latitude, longtitude), 1, 0, 0));
        mMap.moveCamera(cu);
        clusterManager.setRenderer(new CustomRenderer<EventOnMap>(this, mMap, clusterManager));
        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<EventOnMap>() {
            @Override
            public boolean onClusterItemClick(EventOnMap item) {
                String snippet = item.getSnippet();
                showDialog(events.get(item.getId()), clusterManager);
                return false;
            }
        });
        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<EventOnMap>() {
            @Override
            public boolean onClusterClick(Cluster<EventOnMap> cluster) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        cluster.getPosition(), (float) Math.floor(mMap
                                .getCameraPosition().zoom + 3)), 200,
                        null);
                return true;
            }
        });
    }

    private void getEvents(){
        Call<List<Event>> call = retrofitInterface.retrieveJoinedEvents(UserSession.id);
        OnMapReadyCallback onMapReadyCallback = this;
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
                        mapFragment.getMapAsync(onMapReadyCallback);
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
    void showDialog(Event event, ClusterManager<EventOnMap> clusterManager) {
        System.out.println("show dialog");
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.event_clicked_modal);

        ((TextView)dialog.findViewById(R.id.event_item_name)).setText(event.getName());
        ((TextView)dialog.findViewById(R.id.event_item_description)).setText(event.getDescription());
        ((TextView)dialog.findViewById(R.id.event_item_event_date)).setText("Event Date:\n"+event.getDate());
        ((TextView)dialog.findViewById(R.id.event_item_due_date)).setText("Due Time:\n"+event.getDueDate());
        ((TextView)dialog.findViewById(R.id.event_item_owner)).setText("Event Owner: "+event.getOwnerId());
        ((TextView)dialog.findViewById(R.id.event_item_duration)).setText("Duration: "+event.getDuration() + "mins");
        ((TextView)dialog.findViewById(R.id.event_item_location)).setText("Location: "+event.getAddress());

        Button signup = dialog.findViewById(R.id.signup_event_button);

        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Start Time");
        String[] timeslots = event.getTimeSlots().toArray(new String[0]);
        boolean[] checkedItems = new boolean[timeslots.length];
        ArrayList<String> selected = new ArrayList<>();

        builder.setMultiChoiceItems(timeslots, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    selected.add(timeslots[which]);
                } else {
                }
            }
        });

        MapsActivity context = this;
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                HashMap<String, String> map1 = new HashMap<>();
                map1.put("eventName", event.getName());
                map1.put("userId", UserSession.id);
                map1.put("ownerId", event.getOwnerId());
                try {
                    map1.put("timeslots", (new ObjectMapper()).writeValueAsString(selected));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                Call<Void> res = retrofitInterface.executeSignupEvent(map1);
                res.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() == 201) {
                            clusterManager.clearItems();
                            getEvents();
                            Toast.makeText(context, "Sign up successfully!", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400) {
                            Toast.makeText(context, "Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
        alertDialog = builder.create();
        signup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                alertDialog.show();
                dialog.dismiss();
            }

        });
        dialog.show();
    }

}