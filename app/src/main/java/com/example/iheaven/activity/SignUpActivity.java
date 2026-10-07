package com.example.iheaven.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.iheaven.R;
import com.example.iheaven.databinding.ActivitySignInBinding;
import com.example.iheaven.databinding.ActivitySignUpBinding;
import com.example.iheaven.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    private FirebaseAuth firebaseAuth;

    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        binding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.btnCreateAccount.setOnClickListener(view->{
            String name = binding.signupInputName.getEditText().getText().toString();
            String email = binding.signupInputEmail.getEditText().getText().toString();
            String password = binding.signupInputPassword.getEditText().getText().toString();
            String confirmPassword = binding.signupInputPasswordConfirm.getEditText().getText().toString();

            if (name.isEmpty()){
                binding.signupInputName.setError("Name is required");
                binding.signupInputName.requestFocus();
                return;
            }
            if (email.isEmpty()){
                binding.signupInputEmail.setError("Email is required");
                binding.signupInputEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.signupInputEmail.setError("Email is invalid");
                binding.signupInputEmail.requestFocus();
                return;
            }
            if (password.isEmpty()){
                binding.signupInputPassword.setError("Password is required");
                binding.signupInputPassword.requestFocus();
                return;
            }
            if (password.length() < 8){
                binding.signupInputPassword.setError("Password must be at least 8 characters");
                binding.signupInputPassword.requestFocus();
                return;
            }
            if (confirmPassword.isEmpty()){
                binding.signupInputPasswordConfirm.setError("Confirm password is required");
                binding.signupInputPasswordConfirm.requestFocus();
                return;
            }
            if (!confirmPassword.equals(password)){
                binding.signupInputPasswordConfirm.setError("Passwords do not match");
                binding.signupInputPasswordConfirm.requestFocus();
                return;
            }

            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                   if (task.isSuccessful()){
                       String uid = task.getResult().getUser().getUid();

                       User user = User.builder().userId(uid).name(name).email(email).build();

                       firebaseFirestore.collection("users")
                               .document(uid)
                               .set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                   @Override
                                   public void onSuccess(Void unused) {
                                       Toast.makeText(getApplicationContext(), "User registered successfully", Toast.LENGTH_SHORT).show();
                                       Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                                       startActivity(intent);
                                       finish();
                                   }
                               }).addOnFailureListener(new OnFailureListener() {
                                   @Override
                                   public void onFailure(@NonNull Exception e) {

                                   }
                               });
                   }
                }
            });

        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    }
                }
        );


        binding.btnGoogleSignin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });


    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            checkUserInFirestore(firebaseUser);
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        firebaseFirestore.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    } else {
                        createNewUserInFirestore(firebaseUser);
                    }
                });
    }
    private void createNewUserInFirestore(FirebaseUser firebaseUser) {
        User user = User.builder()
                .userId(firebaseUser.getUid())
                .name(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Google User")
                .email(firebaseUser.getEmail())
                .build();

        firebaseFirestore.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(unused -> proceedToMain())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show());
    }
    private void proceedToMain() {
        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        finish();
    }
}