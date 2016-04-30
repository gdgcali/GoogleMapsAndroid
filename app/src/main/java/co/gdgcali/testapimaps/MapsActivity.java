package co.gdgcali.testapimaps;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ServerValue;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        ChildEventListener, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private LatLngBounds.Builder mBounds = new LatLngBounds.Builder();
    private GoogleApiClient googleApiClient;
    private Firebase firebaseClient;
    private LocationRequest locationRequest;

    // Variables
    private static final int SOLICITUDES_PLACE_PICKER = 1;
    private static final String FIREBASE_URL = "https://testapigooglemapsGDG.firebaseio.com/";
    private static final String FIREBASE_ROOT_NODE = "checkouts";


    @Bind(R.id.btnBuscar)
    Button btnEnviar;

    @Bind(R.id.txtPlace)
    EditText txtLugar;

    @Bind(R.id.btnTipoMapa)
    Button btnTipoMapa;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * 10);
        locationRequest.setFastestInterval(1000 * 5);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).
                addApi(Places.PLACE_DETECTION_API).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        googleApiClient.connect();

        Firebase.setAndroidContext(this);
        firebaseClient = new Firebase(FIREBASE_URL);
        firebaseClient.child(FIREBASE_ROOT_NODE).addChildEventListener(this);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.btnTipoMapa)
    public void eventTipoMapa() {

        if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL){
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        else{
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

    }

    @OnClick(R.id.btnBuscar)
    public void eventBuscar(Button boton) {

        String localizacion = txtLugar.getText().toString();

        if (localizacion != null && !localizacion.equals("")) {

            List<Address> lstAddress = null;
            Geocoder geocoder = new Geocoder(this);

            try {

                lstAddress = geocoder.getFromLocationName(localizacion, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }

            Address address = lstAddress.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            addPointToViewPort(latLng, "Sitio Buscado");
            txtLugar.setText("");
        } else {

            try {

                PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                Intent intent = null;
                intent = intentBuilder.build(this);
                startActivityForResult(intent, SOLICITUDES_PLACE_PICKER);

            } catch (GooglePlayServicesRepairableException e) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(), SOLICITUDES_PLACE_PICKER);
            } catch (GooglePlayServicesNotAvailableException e) {
                Toast.makeText(this, "Por favor instale Google Play Services!", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SOLICITUDES_PLACE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);

                Map<String, Object> checkoutData = new HashMap<>();
                checkoutData.put("time", ServerValue.TIMESTAMP);

                firebaseClient.child(FIREBASE_ROOT_NODE).child(place.getId()).setValue(checkoutData);

            } else {

                Toast.makeText(this, "Places Api failed!!", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mMap.setMyLocationEnabled(true);


    }

    @Override
    public void onLocationChanged(Location location) {
        addPointToViewPort(new LatLng(location.getLatitude(), location.getLongitude()), "Aqui Estoy !!!");
    }

    private void addPointToViewPort(LatLng newPoint, String titulo) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(newPoint).title(titulo));
        marker.remove();

        mMap.addMarker(new MarkerOptions().position(newPoint).title(titulo));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(newPoint));
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        String placeId = dataSnapshot.getKey();

        if (placeId != null) {
            Places.GeoDataApi.getPlaceById(googleApiClient, placeId)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {

                        @Override
                        public void onResult(@NonNull PlaceBuffer places) {
                            LatLng location = places.get(0).getLatLng();
                            addPointToViewPort(location, places.get(0).getName().toString());
                            places.release();
                        }
                    });

        }

    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
