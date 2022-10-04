package com.example.easyteamupfrontend.ui.notifications.adapter;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.easyteamupfrontend.AutoCompleteEdit;
import com.example.easyteamupfrontend.Event;
import com.example.easyteamupfrontend.R;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.databinding.FragmentHomeBinding;
import com.example.easyteamupfrontend.databinding.HistoryEventClickedModifyModalBinding;
import com.example.easyteamupfrontend.ui.notifications.NotificationsFragment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.AddressComponents;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class EventHistoryAdapter extends ArrayAdapter<Event> {

    private String userId;

    private NotificationsFragment fragment;
    private Context fragmentContext;
    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;
    private RetrofitInterface retrofitInterface;
    private ActivityResultLauncher<Intent> startAutocomplete;

    private EditText due_time_display;
    private AutoCompleteEdit address_edit;
    private String address;
    private LatLng coordinates;

    private LocalDate eventDate;
    private LocalDateTime dueTime;
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("E yyyy-MM-dd HH:mm:ss");


    public EventHistoryAdapter(ActivityResultLauncher<Intent> startAutocomplete, Context context, List<Event> events, String userId, LayoutInflater inflater,
                               ViewGroup container, Bundle savedInstanceState, RetrofitInterface retrofitInterface, Fragment fragment) {
        super(context, 0, events);
        this.startAutocomplete = startAutocomplete;
        this.fragmentContext = context;
        this.userId = userId;
        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;
        this.retrofitInterface = retrofitInterface;
        this.fragment = (NotificationsFragment) fragment;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Event event = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.history_event_card, parent, false);
        }
        /*
            event_item_name
            event_item_description
            event_item_event_date
            event_item_due_date
            event_item_owner
            event_item_location
         */
        ((TextView)convertView.findViewById(R.id.history_event_item_name)).setText(event.getName());
        ((TextView)convertView.findViewById(R.id.history_event_item_description)).setText(event.getDescription());

        if(userId.equals(event.getOwnerId())) {
            ((TextView) convertView.findViewById(R.id.history_event_item_status)).setText("created");
            ((TextView) convertView.findViewById(R.id.history_event_item_status)).setTextColor(Color.BLUE);
        }
        else if(UserSession.joined(event)){
            ((TextView) convertView.findViewById(R.id.history_event_item_status)).setText("joined");
            ((TextView) convertView.findViewById(R.id.history_event_item_status)).setTextColor(Color.GREEN);
        }
        else{
            ((TextView) convertView.findViewById(R.id.history_event_item_status)).setText("invited");
            ((TextView) convertView.findViewById(R.id.history_event_item_status)).setTextColor(Color.RED);

        }

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
        dialog.setContentView(R.layout.history_event_clicked_modal);
        NotificationsFragment fragment = this.fragment;
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_name)).setText(event.getName());
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_description)).setText(event.getDescription());
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_date)).setText("Event Date: "+event.getDate()) ;
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_due_date)).setText("Due Date: "+ (event.dueDatePassed() ? "passed" : event.getDueDate()));
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_owner)).setText("Event Owner: "+event.getOwnerId());
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_location)).setText("Location: "+event.getAddress());
        ((TextView)dialog.findViewById(R.id.history_event_clicked_item_determined_time)).setText("Determined Time: "+event.getDeterminedTime());

        Button btn = dialog.findViewById(R.id.action_button);
        Button btn2 = dialog.findViewById(R.id.reject_button);
        if(userId.equals(event.getOwnerId())) {
            ((TextView) dialog.findViewById(R.id.history_event_clicked_item_status)).setText("This event is created by you");
            ((TextView) dialog.findViewById(R.id.history_event_clicked_item_status)).setTextColor(Color.BLUE);
            btn.setText("Modify");
            btn2.setText("OK");
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    modifyEvent(event, dialog);
                }
            });
            btn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    fragment.refresh();
                }
            });
        }
        else if(UserSession.joined(event)){
            ((TextView) dialog.findViewById(R.id.history_event_clicked_item_status)).setText("You have joined this event");
            ((TextView) dialog.findViewById(R.id.history_event_clicked_item_status)).setTextColor(Color.GREEN);
            btn.setText("Withdraw");
            btn2.setVisibility(View.INVISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    withdrawEvent(event);
                    dialog.dismiss();

                }
            });
        }
        else{
            ((TextView) dialog.findViewById(R.id.history_event_clicked_item_status)).setText("You are invited to join this event");
            ((TextView) dialog.findViewById(R.id.history_event_clicked_item_status)).setTextColor(Color.RED);
            btn.setText("Accept");
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    acceptInvite(dialog, btn, event);
                }
            });

            btn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rejectEvent(event);
                    dialog.dismiss();
                    fragment.refresh();
                }
            });
        }

        dialog.show();
    }

    void modifyEvent(Event event, Dialog parentDialog){
        final Dialog modify_dialog = new Dialog(this.getContext());
        modify_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        modify_dialog.setCancelable(true);
        modify_dialog.setContentView(R.layout.history_event_clicked_modify_modal);
        ((TextView) modify_dialog.findViewById(R.id.history_modify_event_name)).setText(event.getName());
        modify_dialog.show();
        address_edit = modify_dialog.findViewById(R.id.history_modify_address_edit);

        address_edit.setOnClickListener(view -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.LAT_LNG, Place.Field.VIEWPORT);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("US")
                    .setTypeFilter(TypeFilter.ADDRESS)
                    .build(this.getContext());
            UserSession.event = event;
            UserSession.address_edit = address_edit;
            startAutocomplete.launch(intent);

        });


        EditText time_display = modify_dialog.findViewById(R.id.history_modify_time_picker);
        time_display.setInputType(InputType.TYPE_NULL);
        time_display.setFocusable(false);

        time_display.setOnClickListener(view -> {
            DatePickerDialog dialog = new DatePickerDialog(view.getContext());
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);  // date must be after now
            dialog.setOnDateSetListener((datePicker, i, i1, i2) -> {
                eventDate = LocalDate.of(i, i1+1, i2);
                time_display.setText(dateFormatter.format((eventDate)));
                dialog.dismiss();
            });
            dialog.create();
            dialog.show();
        });

        due_time_display = modify_dialog.findViewById(R.id.history_modify_due_time_picker);
        due_time_display.setInputType(InputType.TYPE_NULL);
        due_time_display.setFocusable(false);
        due_time_display.setOnClickListener(view -> {
            final View dialogView = inflater.inflate(R.layout.date_time_picker, container, false);
            AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
            alertDialog.setTitle("Set Event Date and Time");
            DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
            TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);
            datePicker.setMinDate(System.currentTimeMillis() - 1000);
            dialogView.findViewById(R.id.date_time_set).setOnClickListener(view1 -> {
                dueTime = LocalDateTime.of(datePicker.getYear(), datePicker.getMonth()+1, datePicker.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
                due_time_display.setText(dateTimeFormatter.format(dueTime));
                alertDialog.dismiss();
            });
            alertDialog.setView(dialogView);
            alertDialog.show();
        });
        modify_dialog.findViewById(R.id.history_modify_cancel_modify_event_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                modify_dialog.dismiss();
            }
        });

        modify_dialog.findViewById(R.id.history_modify_modify_event_btn).setOnClickListener(view -> {
            // prepare event detail
            System.out.println("modifying");
            HashMap<String, String> map = new HashMap<>();
            map.put("ownerId", userId);
            map.put("name", event.getName());

            String event_description = ((EditText)modify_dialog.findViewById(R.id.history_modify_event_description_edit)).getText().toString();
            if (!event_description.isEmpty()) {
                map.put("description", event_description);
                event.setDescription(event_description);
            }
            else{
                map.put("description", event.getDescription());
            }

            if (((RadioButton) modify_dialog.findViewById(R.id.history_modify_public_rad)).isChecked()) {
                map.put("publicity", "public");
                event.setPublicity("public");
            } else if (((RadioButton) modify_dialog.findViewById(R.id.history_modify_private_rad)).isChecked()) {
                map.put("publicity", "private");
                event.setPublicity("private");
            }
            else{
                map.put("publicity", event.getPublicity());
            }

            if (coordinates != null) {
                map.put("latitude", String.valueOf(coordinates.latitude));
                map.put("longitude", String.valueOf(coordinates.longitude));
                map.put("address", address);
                event.setCoordinate(coordinates);
            }
            else{
                map.put("latitude", String.valueOf(event.getCoordinate().latitude));
                map.put("longitude", String.valueOf(event.getCoordinate().longitude));
                map.put("address", event.getAddress());
            }

            String duration =((EditText) modify_dialog.findViewById(R.id.history_modify_duration_edit)).getText().toString();
            if (duration.isEmpty()) {
                map.put("duration", event.getDuration());
            } else {
                map.put("duration", duration);
                event.setDuration(duration);
            }

            if (eventDate != null) {
                map.put("eventDate", dateFormatter.format(eventDate));
                event.setDate(dateFormatter.format(eventDate));
            } else {
                map.put("eventDate", event.getDate());
            }

            if (dueTime != null) {
                map.put("dueDate", format.format(dueTime));
                event.setDueDate(format.format(dueTime));
            }
            else{
                map.put("dueDate", event.getDueDate());
            }

            Call<Void> res = retrofitInterface.executeModifyEvent(map);
            EventHistoryAdapter adapterItself = this;
            res.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.code() == 201) {
                        Toast.makeText(getContext(), "Modified!", Toast.LENGTH_SHORT).show();
                        adapterItself.notifyDataSetChanged();
                        fragment.refresh();
                        parentDialog.dismiss();
                    } else if (response.code() == 400) {
                        Toast.makeText(getContext(), "Unknown error!", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {

                }
            });
            modify_dialog.dismiss();

        });

    }

    void rejectEvent(Event event){
        HashMap<String, String> map = new HashMap<>();
        map.put("eventName", event.getName());
        map.put("userId", userId);
        map.put("ownerId", event.getOwnerId());
        Call<Void> res = retrofitInterface.executeReject(map);
        EventHistoryAdapter adapterItself = this;
        res.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201) {
                    adapterItself.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Rejected!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 400) {
                    Toast.makeText(getContext(), "Unknown error!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });

    }

    void withdrawEvent(Event event){
        HashMap<String, String> map = new HashMap<>();
        map.put("eventName", event.getName());
        map.put("userId", userId);
        map.put("ownerId", event.getOwnerId());
        Call<Void> res = retrofitInterface.executeWithDraw(map);
        EventHistoryAdapter adapterItself = this;
        NotificationsFragment fragment = this.fragment;
        res.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201) {
                    adapterItself.notifyDataSetChanged();
                    fragment.refresh();
                    Toast.makeText(getContext(), "Withdraw!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 400) {
                    Toast.makeText(getContext(), "Unknown error!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });

    }

    void acceptInvite(Dialog dialog, Button btn, Event event){
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("Select Start Time");
        String[] timeslots = event.getTimeSlots().toArray(new String[0]);
        boolean[] checkedItems = new boolean[timeslots.length];
        ArrayList<String> selected = new ArrayList<>();
        builder.setMultiChoiceItems(timeslots, checkedItems, new OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    selected.add(timeslots[which]);
                } else {
                }
            }
        });
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fragment.refresh();
            }
        });
        alertDialog = builder.create();
        alertDialog.show();
        dialog.dismiss();

        HashMap<String, String> map = new HashMap<>();
        map.put("eventName", event.getName());
        map.put("userId", UserSession.id);
        map.put("ownerId", event.getOwnerId());
        try {
            map.put("timeslots", (new ObjectMapper()).writeValueAsString(selected));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println(map);
        Call<Void> res = retrofitInterface.executeSignupEvent(map);

        res.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201) {
                    Toast.makeText(getContext(), "Joined!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 400) {
                    Toast.makeText(getContext(), "Unknown error!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });

    }

}
