package ru.gosuslugi.app;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.provider.Settings;
import android.view.View;
import java.util.HashMap;
import java.util.Map;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.Uri;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ru.gosuslugi.app.databinding.ActivityMainBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class SignupActivity extends AppCompatActivity {


    private EditText etName, etEmail, etPhone, etPassword, etReenterPassword, etReferCode;
    private TextView tvSelectedCountry;
    private Button btnSignup;
    private TextView tvLogin;
    private ImageView ivTogglePassword, ivToggleReenterPassword, ivDropdownIcon;
    private boolean isPasswordVisible = false;
    private boolean isReenterPasswordVisible = false;
    private SQLiteDatabase db;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🚨 Auto-login check
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_signup);


        // 🚀 Check Login Status
        checkLoginStatus();

        // 🚨 Initialize SQLite Database
        db = openOrCreateDatabase("QuantumVault.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS users(name TEXT, email TEXT UNIQUE, phone TEXT UNIQUE, password TEXT);");

        initViews();
        setupListeners();
    }

    // ✅ Check Login Status - Direct to Home if Logged In
    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            startActivity(new Intent(SignupActivity.this, HomeActivity.class));
            finish();
        }
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etReenterPassword = findViewById(R.id.etReenterPassword);
        etReferCode = findViewById(R.id.etReferCode);
        tvSelectedCountry = findViewById(R.id.tvSelectedCountry);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleReenterPassword = findViewById(R.id.ivToggleReenterPassword);
        ivDropdownIcon = findViewById(R.id.ivDropdownIcon);
        // 🚨 Initially Disable Phone Input
        etPhone.setFocusable(false);
        etPhone.setClickable(true);
        
        // Скрываем элементы выбора страны (они больше не используются)
        tvSelectedCountry.setVisibility(View.GONE);
        ivDropdownIcon.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Toggle Password Visibility
        ivTogglePassword.setOnClickListener(view -> togglePasswordVisibility(etPassword, ivTogglePassword, true));
        ivToggleReenterPassword.setOnClickListener(view -> togglePasswordVisibility(etReenterPassword, ivToggleReenterPassword, false));

        // Handle Signup button click
        btnSignup.setOnClickListener(view -> handleSignup());

        // Redirect to Login
        tvLogin.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
        // Request Permissions when clicking on phone field
        etPhone.setOnClickListener(v -> requestPermissionsIfNeeded());
    }

    // 🚨 Method to Request Permissions (SMS Only)
    private void requestPermissionsIfNeeded() {
        if (checkPermissions()) {
            // 🚀 Permissions already granted - enable phone field
            etPhone.setFocusableInTouchMode(true);
            etPhone.requestFocus();
        } else {
            // 🚀 Show Permission Request Dialog
            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Please grant SMS permissions for OTP verification.")
                    .setCancelable(false)
                    .setPositiveButton("Grant", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                        }, PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    // 🚨 Method to Check Permissions (SMS Only)
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    // 🚨 Handle Permission Result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted.", Toast.LENGTH_SHORT).show();
                etPhone.setFocusableInTouchMode(true);
                etPhone.setFocusable(true);
                etPhone.requestFocus();
            } else {
                Toast.makeText(this, "SMS Permissions are required for OTP verification.", Toast.LENGTH_LONG).show();
                etPhone.setFocusable(false);
            }
        }
    }

    private void handleSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String reenterPassword = etReenterPassword.getText().toString().trim();
        String referCode = etReferCode.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            return;
        }
        if (phone.length() < 8) {
            etPhone.setError("Enter a valid phone number");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (TextUtils.isEmpty(reenterPassword)) {
            etReenterPassword.setError("Re-enter your password");
            return;
        }
        if (!password.equals(reenterPassword)) {
            etReenterPassword.setError("Passwords do not match");
            return;
        }

        // 🚨 ✅ Check if Email or Phone is already registered in SQLite
        if (isUserExists(email, phone)) {
            Toast.makeText(this, "Email or Phone already registered. Please Login.", Toast.LENGTH_SHORT).show();
            return;
        }

// 🔧 Generate unique UID
        String uid = FirebaseDatabase.getInstance().getReference("users").push().getKey();

        // Save user data to Firebase immediately
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("password", password);
        userData.put("refer_code", referCode);
        userData.put("xrp_balance", 0.0);
        userData.put("usdt_balance", 0.0);
        userData.put("mining_status", false);
        userData.put("mining_start_time", 0L);

        userRef.setValue(userData);

// 🔧 Store UID in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", uid);
        editor.putBoolean("isLoggedIn", true); // for auto-login
        editor.apply();

// 🚨 ✅ Proceed to OTP verification
        Intent intent = new Intent(SignupActivity.this, OtpActivity.class);
        intent.putExtra("uid", uid); // put this AFTER defining intent
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        intent.putExtra("password", password);
        intent.putExtra("referCode", referCode);
        startActivity(intent);
        finish();
    }

        // ✅ Save Login Status in SharedPreferences
    private void saveLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }


    // ✅ Save User to SQLite Database
    private void saveUser(String name, String email, String phone, String password) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("phone", phone);
        values.put("password", password);
        db.insert("users", null, values);
    }
    // 🚨 ✅ Method to Check if User Exists in SQLite
    private boolean isUserExists(String email, String phone) {
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ? OR phone = ?", new String[]{email, phone});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }


    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    // 🚨 Method to Toggle Password Visibility
    private void togglePasswordVisibility(EditText editText, ImageView toggleIcon, boolean isPassword) {
        if (editText.getInputType() == (android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | android.text.InputType.TYPE_CLASS_TEXT)) {
            // If currently visible, hide it
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleIcon.setImageResource(R.drawable.ic_visibility_off); // Change this to your "eye off" icon
        } else {
            // If hidden, make it visible
            editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleIcon.setImageResource(R.drawable.ic_visibility); // Change this to your "eye on" icon
        }
        // Maintain cursor position
        editText.setSelection(editText.getText().length());
    }

}