package com.example.easyteamupfrontend.ui.dashboard.adapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.widget.Toast;

import com.example.easyteamupfrontend.Event;
import com.example.easyteamupfrontend.R;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.ui.dashboard.DashboardFragment;
import com.example.easyteamupfrontend.util.HttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EventListAdapter extends ArrayAdapter<Event> {
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private Fragment currentFragment;

    public EventListAdapter(Context context, List<Event> events, Fragment currentFragment) {
        super(context, 0, events);
        retrofit = new Retrofit.Builder().baseUrl(UserSession.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
        this.currentFragment = currentFragment;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Event event = getItem(position);
        System.out.println(event.getAddress());
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.event_card, parent, false);
        }
        /*
            event_item_name
            event_item_description
            event_item_event_date
            event_item_due_date
            event_item_owner
            event_item_location
         */
        ((TextView)convertView.findViewById(R.id.event_item_name)).setText(event.getName());
        ((TextView)convertView.findViewById(R.id.event_item_description)).setText(event.getDescription());
        /*
        ((TextView)convertView.findViewById(R.id.event_item_event_date)).setText("Event Date: "+event.getDate().toString());
        ((TextView)convertView.findViewById(R.id.event_item_due_date)).setText("Due Date: "+event.getDueDate().toString());
        ((TextView)convertView.findViewById(R.id.event_item_owner)).setText("Event Owner: "+event.getOwnerId());
        ((TextView)convertView.findViewById(R.id.event_item_location)).setText("Location: "+event.getLocation().getLongitude()+", " + event.getLocation().getLatitude());
        */
        convertView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                System.out.println("clicked!");
                showDialog(event);
            }

        });
        return convertView;
    }

    void showDialog(Event event) {
        final Dialog dialog = new Dialog(this.getContext());
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("Select Start Time");
        String[] timeslots = event.getTimeSlots().toArray(new String[0]);
        boolean[] checkedItems = new boolean[timeslots.length];
        ArrayList<String> selected = new ArrayList<>();
        DashboardFragment currentFragment = (DashboardFragment) this.currentFragment;
        ArrayAdapter adapter = this;
        builder.setMultiChoiceItems(timeslots, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    checkedItems[which] = true;
                    selected.add(timeslots[which]);
                }
            }
        });
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
                System.out.println(map1);
                Call<Void> res = retrofitInterface.executeSignupEvent(map1);
                res.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() == 201) {
                            /*
                            fragmentTransaction.detach(currentFragment);
                            fragmentTransaction.attach(currentFragment);
                            fragmentTransaction.commit();*/
                            currentFragment.refresh();
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Sign up successfully!", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400) {
                            Toast.makeText(getContext(), "Failed.", Toast.LENGTH_SHORT).show();
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
