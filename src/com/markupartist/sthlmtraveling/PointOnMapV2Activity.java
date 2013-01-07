package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.util.List;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.markupartist.sthlmtraveling.provider.planner.Stop;

public class PointOnMapV2Activity extends SherlockFragmentActivity
        implements OnMapClickListener, OnInfoWindowClickListener {

    public static String EXTRA_STOP = "com.markupartist.sthlmtraveling.pointonmap.stop";
    public static String EXTRA_HELP_TEXT = "com.markupartist.sthlmtraveling.pointonmap.helptext";
    public static String EXTRA_MARKER_TEXT = "com.markupartist.sthlmtraveling.pointonmap.markertext";

    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;

    private Stop mStop;
    private Marker mMarker;

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, MyApplication.ANALYTICS_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
     }
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Use transparent action bar, fix location of my location btn.

        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.map);

        FlurryAgent.onPageView();
        FlurryAgent.onEvent("Point on map");

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.point_on_map);

        Bundle extras = getIntent().getExtras();
        // TODO: Can we make this as a none member.
        mStop = (Stop) extras.getParcelable(EXTRA_STOP);
        String helpText = extras.getString(EXTRA_HELP_TEXT);
        String markerText = extras.getString(EXTRA_MARKER_TEXT);

        showHelpToast(helpText);

        if (markerText == null) {
            markerText = getString(R.string.tap_to_select_this_point);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (savedInstanceState == null) {
            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);
        } else {
            // Reincarnated activity. The obtained map is the same map instance in the previous
            // activity life cycle. There is no need to reinitialize it.
            mMap = mapFragment.getMap();
        }
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();
    }

    private void showHelpToast(String helpText) {
        if (helpText != null) {
            Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        // Use stops location if present, otherwise set a geo point in 
        // central Stockholm.
        LatLng latLng;
        int zoom;
        if (mStop.getLocation() != null) {
            latLng = new LatLng(
                    mStop.getLocation().getLatitude(), 
                    mStop.getLocation().getLongitude());
            zoom = 16;
        } else {
            latLng = new LatLng(59.325309, 18.069763);
            zoom = 12;
        }

        mMarker = mMap.addMarker(new MarkerOptions()
            .position(latLng)
            .title(getString(R.string.tap_to_select_this_point))
            .visible(true)
            .draggable(true)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        mMarker.showInfoWindow();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.fromLatLngZoom(latLng, zoom)
            ));
    }

    @Override
    public void onMapClick(LatLng position) {
        mMarker.setPosition(position);
        mMarker.showInfoWindow();
    }

    private String getStopName(Location location) {
        Geocoder geocoder = new Geocoder(this);
        String name = "Unkown";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                name = address.getThoroughfare();
            }
        } catch (IOException e) {
            Log.d("Map", "Failed to get name " + e.getMessage());
        }
        return name;
    }

    @Override
    public void onInfoWindowClick(Marker clickedMarker) {
        if (clickedMarker.equals(mMarker)) {
            Toast.makeText(getApplicationContext(),
                    getText(R.string.point_selected), Toast.LENGTH_LONG).show();
            Location location = new Location("sthlmtraveling");
            location.setLatitude(mMarker.getPosition().latitude);
            location.setLongitude(mMarker.getPosition().longitude);
            mStop.setLocation(location);
            mStop.setName(getStopName(mStop.getLocation()));
            setResult(RESULT_OK, (new Intent()).putExtra(EXTRA_STOP, mStop));
            finish();
        }
    }

}
