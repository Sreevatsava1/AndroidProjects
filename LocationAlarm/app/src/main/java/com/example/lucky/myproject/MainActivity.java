package com.example.lucky.myproject;

import android.*;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<Status>
{
    private LocationRequest mLocationRequest;

    GoogleMap mGoogleMap;
    Geocoder geocoder;
    List<Address> locationList;
    int maxResults = 1;
    Marker marker;
    Circle circle;
    int radius;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private static  int UPDATE_INTERVAL = 500;
    private static  int FASTEST_INTERVAL = 500;
    private static  int DISPLACEMENT = 10;

    DatabaseReference reference;
    GeoFire geoFire;
    Marker mCurrent;
    VerticalSeekBar mSeekBar;
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static  final int PLAY_SERVICES_RESOLUTION_REQUEST = 1208;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMap();

        reference = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(reference);
        mSeekBar = findViewById(R.id.verticalSeekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(progress),2000,null);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setUpLocation();

    }
    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
        } else {
            if(checkPlayServices())
            {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
            else
            {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }
    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            final double latitude = mLastLocation.getLatitude();
            final double longitude = mLastLocation.getLongitude();

            geoFire.setLocation("You", new GeoLocation(latitude, longitude),
                    new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error)
                        {
                            if (mCurrent != null)
                                mCurrent.remove();

                            mCurrent = mGoogleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude,longitude))
                                    .title("Your Location")
                                    .snippet("You are here")
                            );
                            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));

                        }
                    }
            );



            Log.d("VatsaV",String.format("Your location was changed : %f / %f ",latitude,longitude));
        }
        else
            Log.d("VatsaV","Can not get your location");
    }
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync( this);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (checkPlayServices())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (mGoogleMap != null) {
            mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View view = getLayoutInflater().inflate(R.layout.info_window, null);
                    TextView tvlocality = view.findViewById(R.id.tv_locality);
                    TextView tvlat = view.findViewById(R.id.tv_lat);
                    TextView tvlon = view.findViewById(R.id.tv_lon);
                    TextView tvsnip = view.findViewById(R.id.tv_snippet);
                    LatLng ll = marker.getPosition();
                    tvlocality.setText(marker.getTitle());
                    tvlat.setText("Latitude: " + ll.latitude);
                    tvlon.setText("Longitude: " + ll.longitude);
                    tvsnip.setText(marker.getSnippet());
                    return view;
                }
            });
            LatLng d_area = new LatLng(14.447342, 78.235990);
            mGoogleMap.addCircle(new CircleOptions()
                    .center(d_area)
                    .radius(250)
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF)
                    .strokeWidth(5.0f));
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(14.447342,78.235990),0.250f);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    sendNotification("VatsaV",String.format("%s entered the area",key));
                }

                @Override
                public void onKeyExited(String key) {
                    sendNotification("VatsaV",String.format("%s exited the area",key));

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    Log.d("MOVE","%s ur within the area");
                }

                @Override
                public void onGeoQueryReady() {

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    Log.e("Error",""+error);
                }
            });

            mGoogleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    circle.remove();

                }
                @Override
                public void onMarkerDrag(Marker marker) {

                }
                @Override
                public void onMarkerDragEnd(Marker marker) throws NullPointerException {

                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    LatLng ll = marker.getPosition();
                    List<Address> list = null;
                    try {
                        list = geocoder.getFromLocation(ll.latitude, ll.longitude, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assert list != null;

                    if (list.size() == 0) {;
                        Toast.makeText(MainActivity.this, "invalid address", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Address address = list.get(0);
                        if (address.getLocality()==null)
                        {
                            marker.setTitle(" ");
//                            drawCircle(ll);
                            geoFence(ll.latitude,ll.longitude);
                        }

                        else {
                            marker.setTitle(address.getLocality());
//                        marker.showInfoWindow();
//                            drawCircle(ll);
                            geoFence(ll.latitude,ll.longitude);

                        }
                    }
                }
            });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        assert searchManager != null;
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setSubmitButtonEnabled(true);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                CharSequence sequence = searchView.getQuery();
                try {
                    search(sequence);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
//                Toast.makeText(MainActivity.this, "hi1", Toast.LENGTH_SHORT).show();

                return false;
            }
        });

        return true;
    }
    private void search(CharSequence sequence) throws IOException{
        String s = sequence.toString();
        geocoder = new Geocoder(this);

        locationList = geocoder.getFromLocationName(s,maxResults);

        if (locationList.size() == 0){
            Toast.makeText(this, "Invalid address", Toast.LENGTH_SHORT).show();
        }

        else {

            Address address = locationList.get(0);

            String locality = address.getLocality();

            double lat = address.getLatitude();
            double lng = address.getLongitude();
            LatLng latLng = new LatLng(lat,lng);
            goToLocation(lat, lng);
            setMarker(locality,lat,lng);
            drawCircle(latLng);

            geoFence(lat, lng);


            Toast.makeText(MainActivity.this, locality, Toast.LENGTH_SHORT).show();

        }

    }

    private void geoFence(double lat, double lng) {
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(lat,lng),.250f);
//        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(14.4610559,78.2319346),0.5f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendNotification("VatsaV",String.format("%s entered the area",key));
            }

            @Override
            public void onKeyExited(String key) {
                sendNotification("VatsaV",String.format("%s exited the area",key));

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d("MOVE","%s ur within the area");
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("Error",""+error);
            }
        });
    }

    private void setMarker(String locality, double lat, double lng) {
        //Step-14.a here we create a method for removing marker
        if (marker != null){
            removeEverything();
        }

        MarkerOptions options = new MarkerOptions()
                .title(locality)
                .draggable(false)  //Step-16 setting draggable as true
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .position(new LatLng(lat,lng))
                .snippet("Your destination");
        marker = mGoogleMap.addMarker(options);
        //Step-18
        circle = drawCircle(new LatLng(lat,lng));
    }
    private Circle drawCircle(LatLng latLng) {
        CircleOptions options = new CircleOptions()
                .center(latLng)
                .radius(250)
                .fillColor(0x20ff0000)
                .strokeColor(Color.BLUE)
                .strokeWidth(5.0f);
        return  mGoogleMap.addCircle(options);
    }
    private void removeEverything(){
        marker.remove();
        marker=null;
        circle.remove();
        circle=null;
    }
    private void goToLocation(double lat, double lng) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
        mGoogleMap.moveCamera(update);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.mapTypeNone:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;
            case R.id.mapTypeNormal:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.mapTypeHybrid:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case R.id.mapTypeSatellite:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.mapTypeTerrain:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();
    }
    private void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);

    }
    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }
    @Override
    public void onResult(@NonNull Status status) {

    }
    private void sendNotification(String title, String content) {
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_location_on_black_24dp)
                .setContentText(content);
        NotificationManager manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_ALL;

        manager.notify(new Random().nextInt(),notification);

    }
}
