package com.example.easyteamupfrontend.ui.login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easyteamupfrontend.LoggedInUser;
import com.example.easyteamupfrontend.MainActivity;
import com.example.easyteamupfrontend.R;
import com.example.easyteamupfrontend.RetrofitInterface;
import com.example.easyteamupfrontend.UserSession;
import com.example.easyteamupfrontend.databinding.ActivityLoginBinding;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        retrofit = new Retrofit.Builder().baseUrl(UserSession.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final TextView registerLead = binding.register;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;


        loginButton.setOnClickListener(view -> {
            if (usernameEditText.getText().toString().isEmpty()) {
                Toast.makeText(view.getContext(), "Must enter user ID!", Toast.LENGTH_SHORT).show();
            } else if (passwordEditText.getText().toString().length() < 5) {
                Toast.makeText(view.getContext(), R.string.invalid_password, Toast.LENGTH_SHORT).show();
            } else {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", usernameEditText.getText().toString());
                map.put("password", passwordEditText.getText().toString());
                Call<LoggedInUser> res = retrofitInterface.executeLogin(map);
                res.enqueue(new Callback<LoggedInUser>() {
                    @Override
                    public void onResponse(Call<LoggedInUser> call, Response<LoggedInUser> response) {
                        if (response.code() == 200) {
                            Toast.makeText(view.getContext(), "Successful!", Toast.LENGTH_SHORT).show();
                            // move to MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            UserSession.id = response.body().getId();
                            UserSession.age = String.valueOf(response.body().getAge());
                            UserSession.bio = response.body().getBio();
                            UserSession.image = response.body().getImage();
                            startActivity(intent);
                        } else if (response.code() == 404) {
                            Toast.makeText(view.getContext(), "Please check your ID and password!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoggedInUser> call, Throwable t) {

                    }
                });
            }
        });

        registerLead.setOnClickListener(view -> {
            LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View register_view = inflater.inflate(R.layout.layout_register, null);
            AlertDialog dialog = new AlertDialog.Builder(view.getContext()).setView(register_view).setTitle("Sign Up")
                    .setPositiveButton("Register", null).setNegativeButton("Cancel", ((dialogInterface, which) -> {
                        dialogInterface.dismiss();
                    })).create();

            dialog.setOnShowListener(dialogInterface -> {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view1 -> {
                    EditText register_id = register_view.findViewById(R.id.register_id);
                    EditText register_password = register_view.findViewById(R.id.register_password);
                    EditText register_age = register_view.findViewById(R.id.register_age);
                    EditText register_bio = register_view.findViewById(R.id.register_bio);
                    EditText register_image = register_view.findViewById(R.id.register_image);
                    if (register_id.getText().toString().isEmpty()) {
                        Toast.makeText(view1.getContext(), "Must enter user ID!", Toast.LENGTH_SHORT).show();
                    } else if (register_password.getText().toString().isEmpty()) {
                        Toast.makeText(view1.getContext(), "Must enter password!", Toast.LENGTH_SHORT).show();
                    } else if (register_age.getText().toString().isEmpty()) {
                        Toast.makeText(view1.getContext(), "Must enter age!", Toast.LENGTH_SHORT).show();
                    } else if (register_bio.getText().toString().isEmpty()) {
                        Toast.makeText(view1.getContext(), "Must enter bio!", Toast.LENGTH_SHORT).show();
                    } else if (register_image.getText().toString().isEmpty()) {
                        Toast.makeText(view1.getContext(), "Must enter image url!", Toast.LENGTH_SHORT).show();
                    }
                    HashMap<String, String> map = new HashMap<>();
                    map.put("id", register_id.getText().toString());
                    map.put("password", register_password.getText().toString());
                    map.put("age", register_age.getText().toString());
                    map.put("bio", register_bio.getText().toString());
                    map.put("image", register_image.getText().toString());
                    Call<Void> res = retrofitInterface.executeSignup(map);
                    res.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.code() == 201) {
                                Toast.makeText(view1.getContext(), "Sign up successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else if (response.code() == 400) {
                                Toast.makeText(view1.getContext(), "User ID already exists!\nPlease choose another.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {

                        }
                    });
                });
            });
            dialog.show();
        });

    }
}

