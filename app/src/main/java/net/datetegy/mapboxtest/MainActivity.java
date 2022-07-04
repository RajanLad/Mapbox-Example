package net.datetegy.mapboxtest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
//import com.mapbox.android.core.location.LocationEngineListener;
//import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.light.Position;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.search.MapboxSearchSdk;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap map;
    private Button startButton,customRoute;
    private PermissionsManager permissionsManager;
//    private LocationLayerPlugin locationLayerPlugin;

    private LocationEngine locationEngine;
    private Location originLocation;
    private Location lastLocation;
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private NavigationMapRoute navigationMapRoute;

    private static final String TAG = "MainActivity";

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 10000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    /**
     * Draw a vector polygon on a map with the Mapbox Android SDK.
     */
    private static final List<List<Point>> POINTS = new ArrayList<>();
    private static final List<Point> OUTER_POINTS = new ArrayList<>();

    private static final List<List<Point>> POINTS_1 = new ArrayList<>();
    private static final List<Point> OUTER_POINTS_1 = new ArrayList<>();

    static {
        OUTER_POINTS.add(Point.fromLngLat(2.35545989710994, 48.83142072262081));
        OUTER_POINTS.add(Point.fromLngLat(2.360480992081867, 48.81602201302896));
        OUTER_POINTS.add(Point.fromLngLat(2.387267007065658, 48.82628618202827));
        OUTER_POINTS.add(Point.fromLngLat(2.3729158690536845, 48.837025993230036));
        POINTS.add(OUTER_POINTS);
    }

    //48.83453819916022, 2.33222792259747
    //48.82281419852089, 2.326014285568315
    //48.81994719859191, 2.341572841279113
    //48.83141421434005, 2.3555168298878466

    static {
        OUTER_POINTS_1.add(Point.fromLngLat(2.33222792259747, 48.83453819916022));
        OUTER_POINTS_1.add(Point.fromLngLat(2.326014285568315, 48.82281419852089));
        OUTER_POINTS_1.add(Point.fromLngLat(2.341572841279113, 48.81994719859191));
        OUTER_POINTS_1.add(Point.fromLngLat(2.3555168298878466, 48.83141421434005));
        POINTS.add(OUTER_POINTS_1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = (MapView)findViewById(R.id.mapviewtest);
        startButton = findViewById(R.id.startButton);
        customRoute = findViewById(R.id.custom_route_button);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        customRoute.setOnClickListener(view -> startActivity(new Intent(MainActivity.this,CustomRouteActivity.class)));

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                NavigationRoute navigationRoute = NavigationRoute.builder(MainActivity.this).accessToken(getString(R.string.access_token)).origin(originPosition).destination(destinationPosition).build();

                navigationRoute.getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        NavigationLauncherOptions  navigationLauncherOptions = NavigationLauncherOptions.builder()
                                .directionsRoute(response.body().routes().get(0))
                                .shouldSimulateRoute(false)
                                .build();
                        NavigationLauncher.startNavigation(MainActivity.this,navigationLauncherOptions);


                        // Print some info about the route
                        DirectionsRoute route = response.body().routes().get(0);
                        Log.d(TAG, "Distance: " + route.distance());

                        // Draw the route on the map

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

                    }
                });




            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(this);
        enableLocation();

        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                style.addSource(new GeoJsonSource("source-id", Polygon.fromLngLats(POINTS)));
                style.addLayerBelow(new FillLayer("layer-id", "source-id").withProperties(
                        fillColor(Color.parseColor("#883bb2d0")),fillColor(Color.parseColor("#88808000"))), "settlement-label"
                );


                enableLocationComponent(style);


            }
        });
    }

    @SuppressLint("WrongConstant")
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = map.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void enableLocation()
    {
        if(PermissionsManager.areLocationPermissionsGranted(this))
        {

            initializeLocationEngine();
            initializeLocationLayer();
        }
        else
        {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine()
    {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);



        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                if(result.getLastLocation()!=null)
                {
                    LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();


                    originLocation = result.getLastLocation();
                    setCameraPosition(result.getLastLocation());


                }
                else
                {

                    LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

                    locationEngine.requestLocationUpdates(request, new LocationEngineCallback<LocationEngineResult>() {
                        @Override
                        public void onSuccess(LocationEngineResult result) {
                            Toast.makeText(MainActivity.this, "requestLocationUpdates", Toast.LENGTH_SHORT).show();
                            originLocation = result.getLastLocation();
                            setCameraPosition(result.getLastLocation());
                        }

                        @Override
                        public void onFailure(@NonNull @NotNull Exception exception) {

                        }
                    }, getMainLooper());
                    //locationEngine.requestLocationUpdates(this);


                }
            }

            @Override
            public void onFailure(@NonNull @NotNull Exception exception) {

            }
        });

    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer()
    {
//        locationLayerPlugin = new LocationLayerPlugin(mapView,map,locationEngine);
//        locationLayerPlugin.setLocationLayerEnabled(true);
//        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
//        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location)
    {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),13.0));
    }

//    @Override
//    @SuppressWarnings("MissingPermission")
//    public void onConnected() {
//
//        locationEngine.requestLocationUpdates();
//    }
//
//    @Override
//    public void onLocationChanged(Location location) {
//        if(location!=null)
//        {
//
//            originLocation = location;
//            setCameraPosition(location);
//        }
//    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //TODO
        //present a toast or a dialog
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted)
        {
            enableLocation();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,  int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();
        if(locationEngine!=null) {
            LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

            locationEngine.requestLocationUpdates(request, new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(LocationEngineResult result) {
                    Toast.makeText(MainActivity.this, "requestLocationUpdates", Toast.LENGTH_SHORT).show();
                    originLocation = result.getLastLocation();
                    setCameraPosition(result.getLastLocation());
                }

                @Override
                public void onFailure(@NonNull @NotNull Exception exception) {

                }
            }, getMainLooper());
        }

//        if(locationLayerPlugin!=null)
//            locationLayerPlugin.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationEngine!=null)
            locationEngine.removeLocationUpdates(new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(LocationEngineResult result) {

                }

                @Override
                public void onFailure(@NonNull @NotNull Exception exception) {

                }
            });
//        if(locationLayerPlugin!=null)
//            locationLayerPlugin.onStop();

        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationEngine!=null)
            locationEngine.removeLocationUpdates(new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(LocationEngineResult result) {

                }

                @Override
                public void onFailure(@NonNull @NotNull Exception exception) {

                }
            });
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {
        if(destinationMarker != null)
        {
            map.removeMarker(destinationMarker);
        }
        destinationMarker = map.addMarker(new MarkerOptions().position(latLng));

        destinationPosition = Point.fromLngLat(latLng.getLongitude(),latLng.getLatitude());
        originPosition = Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());

        getRoute(originPosition,destinationPosition);

        startButton.setEnabled(true);
        return false;
    }

    private void getRoute(Point origin,Point destination)
    {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()

                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if(response.body() == null)
                        {
                            Log.e(TAG, "onResponse: No routes found. " );


                            return;
                        }
                        else if(response.body().routes().size() == 0)
                        {
                            Log.e(TAG, "onResponse: No routes found. " );
                            return;
                        }

                        DirectionsRoute currentRoute = response.body().routes().get(0);
                                drawRoute(response.body().routes().get(0));
                        if(navigationMapRoute!=null)
                        {
                            navigationMapRoute.removeRoute();
                        }
                        else
                        {
                            navigationMapRoute = new NavigationMapRoute(null,mapView,map);
                        }

                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "onError : "+t.getMessage() );
                    }
                });
    }



    private void drawRoute(DirectionsRoute route) {
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
        List<Point> coordinates = lineString.coordinates();
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).latitude(),
                    coordinates.get(i).longitude());
        }

        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#880000"))
                .width(5));
    }


}