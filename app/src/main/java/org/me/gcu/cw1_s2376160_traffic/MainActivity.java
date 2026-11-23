//
// Name                 TODO: Enter Your Name
// Student ID           TODO: Enter Your Student ID
// Programme of Study   TODO: Enter Your Programme of Study
//

package org.me.gcu.cw1_s2376160_traffic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQ_CODE = 200;

    private Button startButton;
    private Button filterButton;
    private Button mapToggleButton;
    private EditText roadSearchInput;
    private EditText distanceSearchInput;
    private RadioGroup feedToggleGroup;

    private TrafficViewModel viewModel;
    private boolean isTwoPaneLandscape = false;
    private boolean showingMapInPortrait = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isTwoPaneLandscape = (findViewById(R.id.fragContainerSecondary) != null);

        startButton = findViewById(R.id.startButton);
        filterButton = findViewById(R.id.filterButton);
        mapToggleButton = findViewById(R.id.mapToggleButton);
        roadSearchInput = findViewById(R.id.roadSearchInput);
        distanceSearchInput = findViewById(R.id.distanceSearchInput);
        feedToggleGroup = findViewById(R.id.feedToggleGroup);

        startButton.setOnClickListener(this);
        filterButton.setOnClickListener(this);
        mapToggleButton.setOnClickListener(this);

        String clientId = "CLIENT_ID";
        String clientKey = "CLIENT_KEY";
        try {
            Bundle metaData = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (metaData != null) {
                clientId = metaData.getString("org.me.gcu.cw1_s2376160_traffic.DATEX_CLIENT_ID", "");
                clientKey = metaData.getString("org.me.gcu.cw1_s2376160_traffic.DATEX_CLIENT_KEY", "");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data keys: " + e.getMessage());
        }

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.initCredentialsAndStart(clientId, clientKey);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragContainerPrimary, new SituationListFragment())
                    .commit();
            if (isTwoPaneLandscape) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragContainerSecondary, new SituationDetailFragment())
                        .commit();
            }
        }

        viewModel.getSelectedSituation().observe(this, situation -> {
            if (situation != null) {
                int targetContainer = isTwoPaneLandscape
                        ? R.id.fragContainerSecondary : R.id.fragContainerPrimary;
                getSupportFragmentManager().beginTransaction()
                        .replace(targetContainer, new SituationDetailFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        viewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        feedToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioUnplanned) {
                viewModel.switchFeed(TrafficViewModel.UNPLANNED_URL);
            } else if (checkedId == R.id.radioRoadworks) {
                viewModel.switchFeed(TrafficViewModel.ROADWORKS_URL);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.startButton) {
            String activeUrl = (feedToggleGroup.getCheckedRadioButtonId() == R.id.radioRoadworks)
                    ? TrafficViewModel.ROADWORKS_URL : TrafficViewModel.UNPLANNED_URL;
            viewModel.fetchFeedData(activeUrl);
        } else if (id == R.id.filterButton) {
            applyUserFilters();
        } else if (id == R.id.mapToggleButton) {
            toggleMapFragment();
        }
    }

    private void applyUserFilters() {
        String roadQuery = roadSearchInput.getText().toString();
        String radiusStr = distanceSearchInput.getText().toString();
        double radiusKm = -1;
        if (!radiusStr.isEmpty()) {
            try {
                radiusKm = Double.parseDouble(radiusStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid numeric radius in km", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double lat = 55.8611;
        double lng = -4.2502;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm != null) {
                Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    lat = loc.getLatitude();
                    lng = loc.getLongitude();
                }
            }
        } else if (radiusKm > 0) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQ_CODE);
        }

        viewModel.setFilters(roadQuery, radiusKm, lat, lng);
    }

    private void toggleMapFragment() {
        if (isTwoPaneLandscape) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragContainerSecondary, new TrafficMapFragment())
                    .commit();
        } else {
            showingMapInPortrait = !showingMapInPortrait;
            if (showingMapInPortrait) {
                mapToggleButton.setText("Show List");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragContainerPrimary, new TrafficMapFragment())
                        .commit();
            } else {
                mapToggleButton.setText("Show on Map");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragContainerPrimary, new SituationListFragment())
                        .commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            String activeUrl = (feedToggleGroup.getCheckedRadioButtonId() == R.id.radioRoadworks)
                    ? TrafficViewModel.ROADWORKS_URL : TrafficViewModel.UNPLANNED_URL;
            viewModel.fetchFeedData(activeUrl);
            return true;
        } else if (id == R.id.menu_clear_filters) {
            roadSearchInput.setText("");
            distanceSearchInput.setText("");
            viewModel.setFilters("", -1, 55.8611, -4.2502);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.stopAutoRefresh();
    }
}