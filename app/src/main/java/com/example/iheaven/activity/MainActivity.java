package com.example.iheaven.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.iheaven.R;
import com.example.iheaven.databinding.ActivityMainBinding;
import com.example.iheaven.fragment.CartFragment;
import com.example.iheaven.fragment.HomeFragment;
import com.example.iheaven.fragment.OrderFragment;
import com.example.iheaven.fragment.ProfileFragment;
import com.example.iheaven.fragment.SearchFragment;
import com.example.iheaven.fragment.SettingsFragment;
import com.example.iheaven.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        saveFCMToken();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.bottom_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.bottom_cart) {
                loadFragment(new CartFragment());
                return true;
            } else if (itemId == R.id.bottom_inbox) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Intent intent = new Intent(this, SignInActivity.class);
                    // these flags clear the back stack
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return false;
                }
                loadFragment(new OrderFragment());
                return true;
            } else if (itemId == R.id.bottom_profile) {
                if (firebaseAuth.getCurrentUser() == null) {
                    startActivity(new Intent(this, SignInActivity.class));
                    finish();
                    return false;
                }
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top + 8,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }



        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseFirestore.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(ds -> {
                        if (ds.exists()) {
                            User user = ds.toObject(User.class);
                            binding.textUser.setText(user.getName());
                        }
                    }).addOnFailureListener(e -> Log.e("Firestore", "Error: " + e.getMessage()));
        }

        setupSearch();


    }

    private SearchFragment searchFragment;

    private void setupSearch() {

        EditText searchInput = findViewById(R.id.text_input_search);
        MaterialButton searchButton = findViewById(R.id.btn_search);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    openOrUpdateSearch(query);
                } else if (query.isEmpty()) {
                    closeSearchFragment();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                openOrUpdateSearch(query);
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    openOrUpdateSearch(query);
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentByTag("SEARCH_FRAGMENT");

            if (currentFragment == null || !currentFragment.isAdded()) {
                searchInput.setText("");
                searchFragment = null;
            }
        });
    }

    private void openOrUpdateSearch(String query) {
        if (searchFragment == null ||
                !searchFragment.isAdded()) {

            searchFragment = new SearchFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, searchFragment, "SEARCH_FRAGMENT")
                    .addToBackStack("SEARCH_FRAGMENT")
                    .commit();
        }

        searchFragment.updateSearch(query);
    }

    private void closeSearchFragment() {
        if (searchFragment != null && searchFragment.isAdded()) {
            getSupportFragmentManager().popBackStack(
                    "SEARCH_FRAGMENT",
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
            );
            searchFragment = null;
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101);
            }
        }
    }

    private void saveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (firebaseAuth.getCurrentUser() == null) return;
                    String userId = firebaseAuth.getCurrentUser().getUid();
                    firebaseFirestore.collection("users")
                            .document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("FCM", "Token saved: " + token));
                });
    }
}