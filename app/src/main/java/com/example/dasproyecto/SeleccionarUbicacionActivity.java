package com.example.dasproyecto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SeleccionarUbicacionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "SelecUbicacionActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap elmapa;
    private FusedLocationProviderClient proveedordelocalizacion;
    private Marker currentMarker;

    private TextView tvDireccionMapa;
    private Button btnConfirmarUbicacion;

    private LatLng selectedLatLng;
    private String selectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccionar_ubicacion);

        tvDireccionMapa = findViewById(R.id.tvDireccionMapa);
        btnConfirmarUbicacion = findViewById(R.id.btnConfirmarUbicacion);
        
        Toolbar toolbar = findViewById(R.id.toolbarMapa);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnConfirmarUbicacion.setOnClickListener(v -> confirmarUbicacion());

        proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentoMapa);
        if (elfragmento != null) {
            elfragmento.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        elmapa = googleMap;

        // Configurar mapa basico (usando constantes de los apuntes)
        elmapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Ajustar el Padding de Google Maps dinámicamente para que la UI nativa 
        // (botón de centrado, logo de Google) no quede tapada por nuestros marcos.
        Toolbar toolbar = findViewById(R.id.toolbarMapa);
        View panelInferior = findViewById(R.id.panelInferiorMapa);
        if (toolbar != null && panelInferior != null) {
            toolbar.post(() -> {
                if (elmapa != null) {
                    // padding(left, top, right, bottom)
                    elmapa.setPadding(0, toolbar.getHeight(), 0, panelInferior.getHeight() + 32); 
                    // +32 píxeles por los margenes del CardView
                }
            });
        }

        // Click en el mapa (usando eventos de los apuntes)
        elmapa.setOnMapClickListener(latLng -> updateMarkerAndAddress(latLng));

        // Pedir permisos y buscar ubicacion (FusedLocation)
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Si tenemos permisos, activamos el boton azul de "Mi Ubicacion" del mapa
        elmapa.setMyLocationEnabled(true);
        
        // Obtenemos la ultima posicion conocida para posicionar la camara de primeras
        proveedordelocalizacion.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        // Movemos la camara segun los apuntes (CameraUpdateFactory.newLatLngZoom)
                        elmapa.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    }
                })
                .addOnFailureListener(this, e -> Log.e(TAG, "Error obtener getLastLocation", e));
    }

    private void updateMarkerAndAddress(LatLng latLng) {
        // Borrar marcador anterior (usando addMarker de apuntes)
        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = elmapa.addMarker(new MarkerOptions().position(latLng).title("Seleccionado"));
        
        // Animamos segun apuntes
        elmapa.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        selectedLatLng = latLng;
        tvDireccionMapa.setText("Buscando dirección...");

        // Usar hilo secundario para obtener la direccion real (String)
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(SeleccionarUbicacionActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        selectedAddress = address.getAddressLine(0);
                        tvDireccionMapa.setText(selectedAddress);
                    } else {
                        selectedAddress = "";
                        tvDireccionMapa.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", latLng.latitude, latLng.longitude));
                    }
                    btnConfirmarUbicacion.setEnabled(true);
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    selectedAddress = "";
                    tvDireccionMapa.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", latLng.latitude, latLng.longitude));
                    btnConfirmarUbicacion.setEnabled(true);
                });
            }
        }).start();
    }

    private void confirmarUbicacion() {
        if (selectedLatLng != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitud", selectedLatLng.latitude);
            resultIntent.putExtra("longitud", selectedLatLng.longitude);
            resultIntent.putExtra("direccion", selectedAddress != null ? selectedAddress : "");
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Por favor selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                // Coordenadas fijas de Bilbao de los apuntes para no dejar la pantalla en el mar
                LatLng defaultLocation = new LatLng(43.26, -2.95);
                elmapa.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
            }
        }
    }
}
