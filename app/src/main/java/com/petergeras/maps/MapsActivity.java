package com.petergeras.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;

    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private Boolean mLocationPermissionGranted = false;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private AutoCompleteTextView mSearchText;

    private PlaceAutoCompleteAdapter mPlaceAutocompleteAdapter;

    private GoogleApiClient mGoogleApiClient;

    private PlaceInfo mPlace;

    private Location mCurrentLocation;

    private LatLngBounds mLatLngBounds;

    public static String mWebsiteUri = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mSearchText = findViewById(R.id.input_search);

        mSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideSoftKeyboard(MapsActivity.this);
                }
            }
        });

        checkGooglePlayServicesAvailable();

        getLocationPermission();

    }



    @SuppressLint("ResourceType")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Sees if user gave permission of user's location
        if (mLocationPermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (mCurrentLocation == null){
                Toast.makeText(this, "Please check that location is on", Toast.LENGTH_LONG).show();
            }


            // Sets a blue dot on the map of your current location
            mMap.setMyLocationEnabled(true);
            // Sets the location button that moves back to your current location
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            // Sets the zoom in and out button true on the map
            mMap.getUiSettings().setZoomControlsEnabled(true);


            // The code below moves the MyLocationButtonEnabled from the default position of the top
            // right of the screen to below the search bar on the right. If this is not done, the
            // MyLocationButtonEnabled would be blocked by the search bar.

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().
                    findFragmentById(R.id.map);
            View mapView = mapFragment.getView();
            if (mapView != null &&
                    mapView.findViewById(1) != null) {
                // Get the button view
                View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                        locationButton.getLayoutParams();
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                layoutParams.setMargins(0, 200, 30, 0);
            }
        }

        // Added 3 markers at the launch of the app. Once the user searches a new place the markers
        // will be cleared.

        LatLng ttt = new LatLng(40.709531, -74.014847);
        mMap.addMarker(new MarkerOptions().position(ttt).title("Turn to Tech"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ttt));


        LatLng chipotle = new LatLng(40.710797, -74.016395);
        mMap.addMarker(new MarkerOptions().position(chipotle).title("Chipotle Mexican Grill"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(chipotle));


        LatLng deli = new LatLng(40.707943, -74.013398);
        mMap.addMarker(new MarkerOptions().position(deli).title("America's Finest Deli"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(deli));

    }



    // Check & Gets permission to use the user's phone location
    private void getLocationPermission() {
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mLocationPermissionGranted = true;
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST);
        }
    }


    // Checks if the user gave permission to use phone location. This is return after
    // ActivityCompat.requestPermissions is made from getLocationPermission()
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0) {
                    // Checks to see if multiple grantResults were given
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    // Initialize our map
                    mLocationPermissionGranted = true;
                }
            }
        }
    }



    public void checkGooglePlayServicesAvailable() {

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                MapsActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //user can make Google Maps requests
            //return true;
        }
        else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // an error occurred with Google Maps but it can be resolved
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(
                    MapsActivity.this, available, ERROR_DIALOG_REQUEST);
        }
        else {
            Toast.makeText(this, "You cannot make map request", Toast.LENGTH_SHORT).show();
        }
        //return false;
    }


    // Gets user's device location
    private void getDeviceLocation() {

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {

                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            mCurrentLocation = (Location) task.getResult();

                            init();

                            // Moves camera to user's current location
                            moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                                    DEFAULT_ZOOM, "My Location");
                        } else {
                            Toast.makeText(MapsActivity.this, "Unable to get current location",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException e) {

        }
    }



    private void init() {

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();



        mSearchText.setOnItemClickListener(mAutocompleteClickListener);


        // When the user searches a place in the search bar, the first results will be closest to
        // the user's location and search outwards to 7.5km range. If the search is outside the
        // 7.5km range then Google Maps will give default results
        mLatLngBounds = getLatLngBoundsFromCircle(mMap.addCircle(new CircleOptions()
                .center(new LatLng(mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude())).radius(7500)));



        mPlaceAutocompleteAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient,
                mLatLngBounds, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);

        // When the user presses search button in the keyboard
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                // Execute our method for searching
                geoLocate();
                return false;
            }
        });

        // Hides soft keyboard when user clicks on the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                hideSoftKeyboard(MapsActivity.this);
            }
        });

        // Hides soft keyboard when user clicks on the map
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                hideSoftKeyboard(MapsActivity.this);
            }
        });
    }


    // Geolocating the search string the user enters into the search bar
    private void geoLocate(){
        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();

        try {
            // List of addresses
            list = geocoder.getFromLocationName(searchString,1);

        }catch (IOException e){

        }
        if (list.size() > 0){
            Address address = list.get(0);
            // Moves camera to the searched addressed
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }

    }


    // Moves Camera to current location
    private void moveCamera(LatLng latLng, float zoom, String title){

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.clear();
            mMap.addMarker(options);
        }
    }


    // Moves Camera to location or place the user is searching for - called after search option selected
    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo){

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.clear();
        CustomInfoWindowAdapter customInfo = new CustomInfoWindowAdapter(MapsActivity.this);
        mMap.setInfoWindowAdapter(customInfo);
        mMap.setOnInfoWindowClickListener(customInfo);


        // If the website uri provides a website then it will be displayed. If the website uri does
        // not have a website uri then the InfoWindow will display "Website not provided" and the
        // setOnInfoWindowClickListener is deactivated. If it was not deactivated then the user
        // would go to the previous website the user searched.
        String website;
        if(placeInfo.getWebsiteUri() != null){
            website = placeInfo.getWebsiteUri().toString();
        }
        else {
            website = "Website not provided";
            mMap.setOnInfoWindowClickListener(null);
        }

        if (placeInfo != null){

            try {
                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                        "Website: " + website + "\n" +
                        "Price Rating: " + placeInfo.getRating() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);

                mMap.addMarker(options);

            } catch (NullPointerException e) {

            }

        }
        else{
            mMap.addMarker(new MarkerOptions().position(latLng));

        }
    }




    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // The function helps the user predict the address or the location the user is searching for in
            // the search bar.
            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();

            // Submit a request
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            hideSoftKeyboard(MapsActivity.this);
        }
    };

    // When the submit request is successful, then the ResultCallback will get the place or location
    // the user is looking for
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {

            if (!places.getStatus().isSuccess()){
                // Must release places to prevent memory leaks
                places.release();
                return;
            }
            final Place place = places.get(0);

            try {
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                mPlace.setAddress(place.getAddress().toString());
                mPlace.setId(place.getId());
                mPlace.setLatLng(place.getLatLng());
                mPlace.setRating(place.getRating());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                mPlace.setWebsiteUri(place.getWebsiteUri());

                mWebsiteUri = place.getWebsiteUri().toString();


            }
            catch (NullPointerException e){

            }

            // Zooms in to the location and creates the Info Window inside the method below
            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);

            // Must release places to prevent memory leaks
            places.release();
        }
    };


    // Creates a circle. The radius will be used to help search for locations or places around the
    // user's current location
    public static LatLngBounds getLatLngBoundsFromCircle(Circle circle){
        if(circle != null){
            return new LatLngBounds.Builder()
                    .include(SphericalUtil.computeOffset(circle.getCenter(),
                            circle.getRadius() * Math.sqrt(2), 45))
                    .include(SphericalUtil.computeOffset(circle.getCenter(),
                            circle.getRadius() * Math.sqrt(2), 225))
                    .build();
        }
        return null;
    }


    // Hides the keyboard when clicked outside the search bar or keyboard
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                0);
    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
