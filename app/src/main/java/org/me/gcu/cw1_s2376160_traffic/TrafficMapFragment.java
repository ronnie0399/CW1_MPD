package org.me.gcu.cw1_s2376160_traffic;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class TrafficMapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private TrafficViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout mapContainer = new FrameLayout(requireContext());
        mapContainer.setId(View.generateViewId());

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(mapContainer.getId(), mapFragment)
                .commitNow();
        mapFragment.getMapAsync(this);

        return mapContainer;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        LatLng scotlandCentre = new LatLng(56.4907, -4.2026);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scotlandCentre, 6.8f));

        viewModel.getDisplayedSituations().observe(getViewLifecycleOwner(), this::renderMarkers);
    }

    private void renderMarkers(ArrayList<TrafficSituation> list) {
        if (googleMap == null || list == null) return;
        googleMap.clear();

        for (TrafficSituation s : list) {
            ArrayList<TrafficSituation.Coordinates> coords = s.getLocations();
            if (!coords.isEmpty()) {
                LatLng startPos = new LatLng(coords.get(0).getLatitude(), coords.get(0).getLongitude());
                googleMap.addMarker(new MarkerOptions()
                        .position(startPos)
                        .title(s.getRoadName() + ": " + s.getRecordType())
                        .snippet(s.getComment()));

                if (coords.size() >= 2) {
                    LatLng endPos = new LatLng(coords.get(1).getLatitude(), coords.get(1).getLongitude());
                    googleMap.addMarker(new MarkerOptions()
                            .position(endPos)
                            .title(s.getRoadName() + " (End)"));
                    googleMap.addPolyline(new PolylineOptions()
                            .add(startPos, endPos)
                            .width(6f)
                            .color(Color.RED));
                }
            }
        }
    }
}