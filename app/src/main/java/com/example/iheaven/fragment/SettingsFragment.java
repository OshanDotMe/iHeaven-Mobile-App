package com.example.iheaven.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;
import com.example.iheaven.activity.MainActivity;
import com.example.iheaven.activity.SignInActivity;
import com.example.iheaven.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            uploadProfileImage(result.getData().getData());
                        }
                    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.GONE);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.settingsTitleBar.setPadding(0, systemBars.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) return;
        userId = auth.getCurrentUser().getUid();

        loadUserData();
        setupClickListeners();
    }


    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (binding == null || doc == null) return;

                    String name  = doc.getString("name");
                    String email = doc.getString("email");
                    String mobile = doc.getString("mobile");
                    String photo = doc.getString("profilePicUrl");

                    binding.profileName.setText(
                            name != null ? name : "No name set");
                    binding.profileEmail.setText(
                            email != null ? email :
                                    auth.getCurrentUser().getEmail());

                    binding.currentName.setText(
                            name != null ? name : "Not set");
                    binding.currentEmail.setText(
                            email != null ? email :
                                    auth.getCurrentUser().getEmail());
                    binding.currentMobile.setText(
                            mobile != null ? mobile : "Not set");

                    if (photo != null && !photo.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(
                                    photo, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(
                                    decodedBytes, 0, decodedBytes.length);
                            binding.profileImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Log.e("Settings", "Error loading image", e);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Settings", "Error loading user data", e)
                );
    }

    private void setupClickListeners() {

        binding.changePhotoButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            100);
                    return;
                }
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            100);
                    return;
                }
            }
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        binding.changeNameRow.setOnClickListener(v ->
                showEditDialog(
                        "Full Name",
                        "name",
                        binding.currentName.getText().toString(),
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_FLAG_CAP_WORDS
                )
        );

        binding.changeMobileRow.setOnClickListener(v ->
                showEditDialog(
                        "Mobile Number",
                        "mobile",
                        binding.currentMobile.getText().toString(),
                        InputType.TYPE_CLASS_PHONE
                )
        );

        binding.changeEmailRow.setOnClickListener(v ->
                showEditDialog(
                        "Email",
                        "email",
                        binding.currentEmail.getText().toString(),
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                )
        );

        binding.changePasswordRow.setOnClickListener(v ->
                showChangePasswordDialog()
        );

        binding.logoutRow.setOnClickListener(v ->
                showLogoutConfirmDialog()
        );
    }


    private void showEditDialog(String title, String field,
                                String currentValue, int inputType) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(requireContext());
        builder.setTitle("Change " + title);

        EditText input = new EditText(requireContext());
        input.setInputType(inputType);
        input.setText(currentValue.equals("Not set") ? "" : currentValue);
        input.setSelection(input.getText().length());

        int p = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(p, p, p, p);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (newValue.isEmpty()) {
                Toast.makeText(getContext(),
                        title + " cannot be empty",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            updateField(field, newValue, title);
        });

        builder.setNegativeButton("Cancel",
                (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void updateField(String field, String value, String title) {
        db.collection("users").document(userId)
                .update(field, value)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(),
                            title + " updated successfully",
                            Toast.LENGTH_SHORT).show();
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(),
                            "Failed to update " + title,
                            Toast.LENGTH_SHORT).show();
                    Log.e("Settings", "Error updating " + field, e);
                });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(p, p, p, 0);

        EditText currentPass = new EditText(requireContext());
        currentPass.setHint("Current password");
        currentPass.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPass);

        EditText newPass = new EditText(requireContext());
        newPass.setHint("New password");
        newPass.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPass);

        EditText confirmPass = new EditText(requireContext());
        confirmPass.setHint("Confirm new password");
        confirmPass.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPass);

        builder.setView(layout);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String current = currentPass.getText().toString().trim();
            String newP    = newPass.getText().toString().trim();
            String confirm = confirmPass.getText().toString().trim();

            if (current.isEmpty() || newP.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(getContext(),
                        "All fields required",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newP.equals(confirm)) {
                Toast.makeText(getContext(),
                        "Passwords do not match",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (newP.length() < 6) {
                Toast.makeText(getContext(),
                        "Password must be at least 8 characters",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            reAuthAndChangePassword(current, newP);
        });

        builder.setNegativeButton("Cancel",
                (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void reAuthAndChangePassword(String currentPass,
                                         String newPass) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), currentPass);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid ->
                        user.updatePassword(newPass)
                                .addOnSuccessListener(aVoid2 -> {
                                    if (binding == null) return;
                                    Toast.makeText(getContext(),
                                            "Password changed successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    if (binding == null) return;
                                    Toast.makeText(getContext(),
                                            "Failed to change password",
                                            Toast.LENGTH_SHORT).show();
                                })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Current password is incorrect",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void uploadProfileImage(android.net.Uri imageUri) {
        if (imageUri == null) return;

        try {
            InputStream inputStream = requireContext()
                    .getContentResolver().openInputStream(imageUri);

            if (inputStream == null) {
                Toast.makeText(getContext(),
                        "Could not open image", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                Toast.makeText(getContext(),
                        "Could not decode image", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getContext(),
                    "Uploading image...", Toast.LENGTH_SHORT).show();

            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(
                    imageBytes, Base64.DEFAULT);

            db.collection("users").document(userId)
                    .update("profilePicUrl", base64Image)
                    .addOnSuccessListener(aVoid -> {
                        if (binding == null) return;

                        byte[] decodedBytes = Base64.decode(
                                base64Image, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(
                                decodedBytes, 0, decodedBytes.length);
                        binding.profileImage.setImageBitmap(decodedBitmap);

                        Toast.makeText(getContext(),
                                "Profile photo updated",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Failed to update photo",
                                    Toast.LENGTH_SHORT).show()
                    );

        } catch (Exception e) {
            Log.e("Settings", "Error processing image", e);
            Toast.makeText(getContext(),
                    "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }


    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    auth.signOut();
                    Intent intent = new Intent(requireContext(),
                            MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel",
                        (dialog, which) -> dialog.cancel())
                .show();
    }


    @Override
    public void onResume() {
        super.onResume();
        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.VISIBLE);
        binding = null;
    }
}