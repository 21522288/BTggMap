package com.example.btggmap;

import static androidx.core.content.ContentProviderCompat.requireContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private final static int LOCATION_REQUEST_CODE = 44;
    private LatLng currentPosition;
    private FusedLocationProviderClient fusedLocationClient;
    public static LatLng end = null;

    MarkerOptions markerEnd;
    private FloatingActionButton btnNormal;
    private FloatingActionButton btnStatellite;
    private FloatingActionButton btnHybrid;
    private FloatingActionButton btnTerrain;
    private FloatingActionButton btnNone;
    private SearchView mSearchView;
    private com.google.android.material.floatingactionbutton.FloatingActionButton draw, erase;
    private Polyline polyline;
    private List<Polyline> listLine = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
        draw = findViewById(R.id.draw);
        draw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.color(R.color.blue); // Màu sắc đường nối
                polylineOptions.width(5); // Độ dày đường nối

                for (Marker marker : markers) {
                    polylineOptions.add(marker.getPosition());
                }
                polyline = map.addPolyline(polylineOptions);
                listLine.add(polyline);
                // Di chuyển camera tới vị trí đầu tiên
                map.moveCamera(CameraUpdateFactory.newLatLng(markers.get(0).getPosition()));
            }
        });
        erase = findViewById(R.id.erase);
        erase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Marker marker : markers) {
                    marker.remove();
                }
                markers.clear();

                if (listLine.size()>0) {
                    for (Polyline p : listLine) {
                        p.remove();
                    }
                    listLine.clear();
                }
            }
        });
        btnNormal = findViewById(R.id.fbtn_normal);
        btnNormal.setOnClickListener(v -> {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        });
        btnStatellite = findViewById(R.id.fbtn_satellite);
        btnStatellite.setOnClickListener(v -> {
            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        });
        btnHybrid = findViewById(R.id.fbtn_hybrid);
        btnHybrid.setOnClickListener(v -> {
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        });
        btnTerrain = findViewById(R.id.fbtn_terrain);
        btnTerrain.setOnClickListener(v -> {
            map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        });
        btnNone = findViewById(R.id.fbtn_none);
        btnNone.setOnClickListener(v -> {
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
        });
        mSearchView = findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                String location = mSearchView.getQuery().toString();
                List<Address> addresses = null;

                if (location != null) {
                    Geocoder geocoder = new Geocoder(getBaseContext());

                    try {
                        addresses = geocoder.getFromLocationName(location, 1);

                        if (addresses.size() != 0) {
                            Address address = addresses.get(0);
                            end = new LatLng(address.getLatitude(), address.getLongitude());
                            // add this marker to map
                            map.clear();
                            markerEnd = new MarkerOptions().position(end).title(location);

                           Marker marker = map.addMarker(markerEnd);
                            markers.add(marker);
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(end, 19));
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Không tìm thấy!",Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        ShowToast("Không tìm ra địa điểm: " + e.getMessage());

                    }
                } else {
                    ShowToast("Vui lòng nhập gì đó");
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }
    public void ShowToast(String value) {
        Toast.makeText(getBaseContext(), value, Toast.LENGTH_SHORT).show();
    }
    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getBaseContext());
            if (isPermissionGranted()) {
                getCurrentLocation();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }

            map = googleMap;
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng latLng) {

                    Geocoder geocoder = new Geocoder(getBaseContext());
                    try {
                        // xóa marker cũ
                       // map.clear();
                        end = latLng;
                        // lay thong tin tai diem do
                        ArrayList<Address> addresses = (ArrayList<Address>) geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        ShowSnackbar(addresses.get(0).getAddressLine(0)); // show info address
                        markerEnd = new MarkerOptions().position(latLng).title(addresses.get(0).getAddressLine(0));

                      //  map.addMarker(markerEnd);
                        Marker marker = map.addMarker(markerEnd);
                        markers.add(marker);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e){
                        Toast.makeText(getBaseContext(), "Không xác định được vị trí", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });

        }
    };
    boolean isPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        // ham lay toa do hien tai
        if (isPermissionGranted()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // init lat lng
                        currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                        // create marker
                        MarkerOptions markerOptions = new MarkerOptions().position(currentPosition).title("Current position");

                        Marker marker = map.addMarker(markerOptions);
                        markers.add(marker);
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15));
                        map.getUiSettings().setZoomControlsEnabled(true);
                        map.getUiSettings().setMapToolbarEnabled(true);
                        map.getUiSettings().setMyLocationButtonEnabled(true);
                        map.setMyLocationEnabled(true);
                        map.setBuildingsEnabled(true);
                    }
                }
            });
        }
    }
    public void ShowSnackbar(String value) {
        RelativeLayout layout = findViewById(R.id.base_layout);
        Snackbar snackbar = Snackbar.make(layout, value, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(getBaseContext(), R.color.white))
                .setTextColor(ContextCompat.getColor(getBaseContext(), R.color.black));

        snackbar.show();
    }

}