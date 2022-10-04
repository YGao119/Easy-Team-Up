package com.example.easyteamupfrontend.ui.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.easyteamupfrontend.AutoCompleteEdit;
import com.example.easyteamupfrontend.R;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.databinding.FragmentHomeBinding;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private final String BASE_URL = UserSession.BASE_URL;

    private EditText event_name_edit;
    private EditText event_description_edit;
    private EditText due_time_display;
    private ImageButton time_slot_btn;
    private ImageButton invitee_text;
    private EditText duration_edit;
    private EditText time_display;
    private ListView invitee_list;
    private ListView time_slot_list;
    private RadioButton public_rad;
    private RadioButton private_rad;
    private Set<String> invitees = new HashSet<>();
    private Set<String> time_slots = new HashSet<>();

    private LocalDate eventDate;
    private LocalDateTime dueTime;
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private AutoCompleteEdit address_edit;
    private String address;
    private LatLng coordinates;
    private final ActivityResultLauncher<Intent> startAutocomplete = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Place place = Autocomplete.getPlaceFromIntent(intent);
                        fillInAddress(place);
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                }
            });


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        event_name_edit = binding.eventNameEdit;
        event_description_edit = binding.eventDescriptionEdit;
        invitee_text = binding.inviteeText;
        due_time_display = binding.dueTimePicker;
        address_edit = binding.addressEdit;
        time_slot_btn = binding.timeSlotBtn;
        duration_edit = binding.durationEdit;
        public_rad = binding.publicRad;
        private_rad = binding.privateRad;

        time_display = binding.timePicker;
        time_display.setInputType(InputType.TYPE_NULL);
        time_display.setFocusable(false);
        time_display.setOnClickListener(view -> {
            DatePickerDialog dialog = new DatePickerDialog(view.getContext());
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);  // date must be after now
            dialog.setOnDateSetListener((datePicker, i, i1, i2) -> {
                eventDate = LocalDate.of(i, i1 + 1, i2);
                time_display.setText(dateFormatter.format((eventDate)));
                dialog.dismiss();
            });
            dialog.create();
            dialog.show();
            hideKeyboard(view);
        });

        due_time_display.setInputType(InputType.TYPE_NULL);
        due_time_display.setFocusable(false);
        due_time_display.setOnClickListener(view -> {
            final View dialogView = inflater.inflate(R.layout.date_time_picker, container, false);
            AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
            alertDialog.setTitle("Set Event Due Date and Time");
            DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
            TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
            datePicker.setMinDate(System.currentTimeMillis() - 1000);
            dialogView.findViewById(R.id.date_time_set).setOnClickListener(view1 -> {
                dueTime = LocalDateTime.of(datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
                due_time_display.setText(dateTimeFormatter.format(dueTime));
                alertDialog.dismiss();
            });
            alertDialog.setView(dialogView);
            alertDialog.show();
            hideKeyboard(view);
        });

        binding.inviteBtn.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Search by user ID");
            View input = inflater.inflate(R.layout.input_dialog, container, false);
            EditText editText = input.findViewById(R.id.invite_edit);
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                String invitee = editText.getText().toString();
                Call<Void> res = retrofitInterface.executeVerifyUserExistence(invitee);
                res.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() == 200) {
                            invitees.add(invitee);
                            invitee_text.setEnabled(true);
                            invitee_text.setImageResource(R.drawable.baseline_expand_more_24);
                            Toast.makeText(getContext(), "Added!", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 404) {
                            Toast.makeText(getContext(), "User doesn't exist!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
            hideKeyboard(view);
        });

        binding.selectTimeBtn.setOnClickListener(view -> {
            TimePickerDialog dialog = new TimePickerDialog(view.getContext(), (timePicker, i, i1) -> {
                time_slots.add((i < 10 ? "0" + i : i) + ":" + (i1 < 10 ? "0" + i1 : i1));
                time_slot_btn.setEnabled(true);
                time_slot_btn.setImageResource(R.drawable.baseline_expand_more_24);
            }, 0, 0, true);
            dialog.create();
            dialog.show();
            hideKeyboard(view);
        });

        time_slot_btn.setOnClickListener(view -> {
            if (time_slots.size() != 0) {
                time_slot_list = new ListView(getContext());
                time_slot_list.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, time_slots.toArray(new String[0])));
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(time_slot_list).setTitle("Selected time slot(s)").setPositiveButton("OK", null).create().show();
            }
            hideKeyboard(view);
        });

        invitee_text.setOnClickListener(view -> {
            if (invitees.size() != 0) {
                invitee_list = new ListView(getContext());
                invitee_list.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, invitees.toArray(new String[0])));
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(invitee_list).setTitle("Invited people list").setPositiveButton("OK", null).create().show();
            }
            hideKeyboard(view);
        });

        address_edit.setOnClickListener(view -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.LAT_LNG, Place.Field.VIEWPORT);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("US")
                    .setTypeFilter(TypeFilter.ADDRESS)
                    .build(this.getContext());
            startAutocomplete.launch(intent);
            hideKeyboard(view);
        });

        binding.createEventBtn.setOnClickListener(view -> {
            // prepare event detail
            HashMap<String, String> map = new HashMap<>();
            String event_name = event_name_edit.getText().toString();
            if (event_name.isEmpty()) {
                Toast.makeText(getContext(), "Must have an event name!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                map.put("name", event_name);
            }

            String event_description = event_description_edit.getText().toString();
            if (event_description.isEmpty()) {
                Toast.makeText(getContext(), "Must have an event description!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                map.put("description", event_description);
            }

            if (public_rad.isChecked()) {
                map.put("publicity", "public");
            } else if (private_rad.isChecked()) {
                map.put("publicity", "private");
            } else {
                Toast.makeText(getContext(), "Must select publicity!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (coordinates != null && address != null) {
                map.put("latitude", String.valueOf(coordinates.latitude));
                map.put("longitude", String.valueOf(coordinates.longitude));
                map.put("address", address);
            } else {
                Toast.makeText(getContext(), "Must select location!", Toast.LENGTH_SHORT).show();
                return;
            }

            String duration = duration_edit.getText().toString();
            if (duration.isEmpty()) {
                Toast.makeText(getContext(), "Must have a duration!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                map.put("duration", duration);
            }

            if (eventDate != null) {
                map.put("eventDate", dateFormatter.format(eventDate));
            } else {
                Toast.makeText(getContext(), "Must select event date!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dueTime != null) {
                map.put("dueDate", dateTimeFormatter.format(dueTime));
            } else {
                Toast.makeText(getContext(), "Must select due time!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (time_slots.isEmpty()) {
                Toast.makeText(getContext(), "Must propose at least one timeslot!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    String str = (new ObjectMapper()).writeValueAsString(time_slots);
                    map.put("timeslots", str);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            if (!invitees.isEmpty()) {
                try {
                    String str = (new ObjectMapper()).writeValueAsString(invitees);
                    map.put("invitees", str);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            map.put("ownerId", UserSession.id);
            Call<Void> res = retrofitInterface.executeCreateEvent(map);
            res.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.code() == 201) {
                        Toast.makeText(getContext(), "Created!", Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 400) {
                        Toast.makeText(getContext(), "Unknown error!", Toast.LENGTH_SHORT).show();
                    }
                    cleanForm();
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                }
            });
            hideKeyboard(view);
        });

        return root;
    }

    private void cleanForm() {
        event_name_edit.getText().clear();
        event_description_edit.getText().clear();
        Objects.requireNonNull(address_edit.getText()).clear();
        address = null;
        coordinates = null;
        time_display.getText().clear();
        eventDate = null;
        duration_edit.getText().clear();
        due_time_display.getText().clear();
        dueTime = null;
        public_rad.setChecked(false);
        private_rad.setChecked(false);
        invitees.clear();
        time_slots.clear();
        invitee_text.setEnabled(false);
        invitee_text.setBackground(null);
        invitee_text.invalidate();
        time_slot_btn.setEnabled(false);
        time_slot_btn.setBackground(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        coordinates = place.getLatLng();
        address = builder.toString();
        address_edit.setText(address);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }
}
