package com.example.parkingspotsaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etNote;
    private Spinner spinnerLocations;
    private Button btnSaveLocation, btnRetrieveLocation, btnDeleteLocation, btnLogout;
    private List<SavedLocation> savedLocations = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etNote = findViewById(R.id.etNote);
        spinnerLocations = findViewById(R.id.spinnerLocations);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
        btnRetrieveLocation = findViewById(R.id.btnRetrieveLocation);
        btnDeleteLocation = findViewById(R.id.btnDeleteLocation);
        btnLogout = findViewById(R.id.btnLogout);

        // Initialize Spinner adapter
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocations.setAdapter(spinnerAdapter);

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        // Load saved locations
        loadSavedLocations();

        // Save location
        btnSaveLocation.setOnClickListener(view -> saveLocation());

        // Retrieve location
        btnRetrieveLocation.setOnClickListener(view -> retrieveLocation());

        // Delete location
        btnDeleteLocation.setOnClickListener(view -> deleteLocation());

        // Logout
        btnLogout.setOnClickListener(view -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void saveLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();

        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                String userId = mAuth.getUid();
                if (userId != null) {
                    // Get the note from user input
                    String note = etNote.getText().toString().trim();
                    if (note.isEmpty()) {
                        note = "No description";
                    }

                    // Generate a unique ID for the location
                    String locationId = mDatabase.child("locations").child(userId).push().getKey();

                    // Create a SavedLocation object
                    SavedLocation savedLocation = new SavedLocation(locationId, location.getLatitude(), location.getLongitude(), note);

                    // Save to Firebase
                    mDatabase.child("locations").child(userId).child(locationId).setValue(savedLocation).addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Location saved!", Toast.LENGTH_SHORT).show();
                        etNote.setText(""); // Clear note input
                        loadSavedLocations(); // Reload the spinner data
                    }).addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to save location.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(MainActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
                    // Redirect to LoginActivity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            } else {
                Toast.makeText(MainActivity.this, "Unable to get current location.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSavedLocations() {
        String userId = mAuth.getUid();
        if (userId != null) {
            mDatabase.child("locations").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    savedLocations.clear();
                    List<String> notes = new ArrayList<>();

                    for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                        SavedLocation savedLocation = locationSnapshot.getValue(SavedLocation.class);
                        if (savedLocation != null) {
                            savedLocations.add(savedLocation);
                            notes.add(savedLocation.getNote());
                        }
                    }

                    spinnerAdapter.clear();
                    spinnerAdapter.addAll(notes);
                    spinnerAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to load locations.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void retrieveLocation() {
        int position = spinnerLocations.getSelectedItemPosition();
        if (position >= 0 && position < savedLocations.size()) {
            SavedLocation selectedLocation = savedLocations.get(position);

            // Open MapsActivity with selected location
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.putExtra("latitude", selectedLocation.getLatitude());
            intent.putExtra("longitude", selectedLocation.getLongitude());
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "No location selected.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteLocation() {
        int position = spinnerLocations.getSelectedItemPosition();
        if (position >= 0 && position < savedLocations.size()) {
            SavedLocation selectedLocation = savedLocations.get(position);
            String userId = mAuth.getUid();
            if (userId != null) {
                mDatabase.child("locations").child(userId).child(selectedLocation.getId()).removeValue().addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Location deleted.", Toast.LENGTH_SHORT).show();
                    loadSavedLocations(); // Reload the spinner data
                }).addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to delete location.", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            Toast.makeText(MainActivity.this, "No location selected.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveLocation(); // Retry saving location
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}