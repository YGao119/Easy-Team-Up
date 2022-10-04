package com.example.easyteamupfrontend.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.databinding.FragmentProfileBinding;
import com.squareup.picasso.Picasso;
import com.example.easyteamupfrontend.R;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        retrofit = new Retrofit.Builder().baseUrl(UserSession.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        binding.profileUserId.setText(UserSession.id);
        binding.profileAge.setText(UserSession.age);
        binding.profileBio.setText(UserSession.bio);
        Picasso.get().setLoggingEnabled(true);
        if(UserSession.image.length() == 0){
            UserSession.image = "https://pbs.twimg.com/profile_images/792503562907455488/jssaOeev_400x400.jpg";
        }
        if(!UserSession.image.substring(0, 4).equals("http")){
            UserSession.image = "https://" +  UserSession.image;
        }
        Picasso.get().load(UserSession.image).into(binding.profileImg);

        Button edit = binding.editProfile;
        edit.setOnClickListener(view -> {
            LayoutInflater inflater1 = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View edit_view = inflater1.inflate(R.layout.layout_edit, null);
            AlertDialog dialog = new AlertDialog.Builder(view.getContext()).setView(edit_view).setTitle("Edit")
                    .setPositiveButton("Save", null).setNegativeButton("Cancel", ((dialogInterface, which) -> {
                        dialogInterface.dismiss();
                    })).create();

            dialog.setOnShowListener(dialogInterface -> {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view1 -> {
                    EditText edit_age = edit_view.findViewById(R.id.edit_age);
                    EditText edit_bio = edit_view.findViewById(R.id.edit_bio);
                    EditText edit_image = edit_view.findViewById(R.id.edit_image);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("userId", UserSession.id);
                    if(edit_age.getText().toString().length() == 0){
                        map.put("age", UserSession.age);
                    }
                    else{
                        map.put("age", edit_age.getText().toString());
                        UserSession.age = edit_age.getText().toString();
                    }
                    if(edit_bio.getText().toString().length() == 0){
                        map.put("bio", UserSession.bio);
                    }
                    else{
                        map.put("bio", edit_bio.getText().toString());
                        UserSession.bio = edit_bio.getText().toString();
                    }
                    if(edit_image.getText().toString().length() == 0){
                        map.put("image", UserSession.image);
                    }
                    else{
                        map.put("image", edit_image.getText().toString());
                        UserSession.image = edit_image.getText().toString();
                    }
                    Call<Void> res = retrofitInterface.executeEditProfile(map);
                    res.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.code() == 201) {
                                binding.profileUserId.setText(UserSession.id);
                                binding.profileAge.setText(UserSession.age);
                                binding.profileBio.setText(UserSession.bio);
                                Picasso.get().setLoggingEnabled(true);
                                if(UserSession.image.length() == 0){
                                    UserSession.image = "https://pbs.twimg.com/profile_images/792503562907455488/jssaOeev_400x400.jpg";
                                }
                                if(!UserSession.image.substring(0, 4).equals("http")){
                                    UserSession.image = "https://" +  UserSession.image;
                                }
                                Picasso.get().load(UserSession.image).into(binding.profileImg);
                                Toast.makeText(view1.getContext(), "Edit saved!", Toast.LENGTH_SHORT).show();

                            } else if (response.code() == 404) {
                                Toast.makeText(view1.getContext(), "Failed.", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {

                        }
                    });
                });
            });
            dialog.show();
        });
        return binding.getRoot();
    }
}