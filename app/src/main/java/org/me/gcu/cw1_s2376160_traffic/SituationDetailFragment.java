package org.me.gcu.cw1_s2376160_traffic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class SituationDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        TextView detailText = new TextView(requireContext());
        detailText.setPadding(24, 24, 24, 24);
        detailText.setTextSize(15f);
        scrollView.addView(detailText);

        TrafficViewModel viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        viewModel.getSelectedSituation().observe(getViewLifecycleOwner(), s -> {
            if (s == null) {
                detailText.setText("Select a situation from the list to view full XML details.");
                return;
            }
            StringBuilder coordsBuilder = new StringBuilder();
            for (int i = 0; i < s.getLocations().size(); i++) {
                TrafficSituation.Coordinates c = s.getLocations().get(i);
                coordsBuilder.append(i == 0 ? "Point/From: " : "To: ")
                        .append(c.getLatitude()).append(", ").append(c.getLongitude()).append("\n");
            }
            String details = "Road: " + s.getRoadName() + "\n"
                    + "Situation ID: " + s.getSituationId() + "\n"
                    + "Record Type: " + s.getRecordType() + "\n"
                    + "Management / Sub-Type: " + s.getSpecificManagementType() + "\n"
                    + "Impact: " + (s.getImpactDetails().isEmpty() ? "N/A" : s.getImpactDetails()) + "\n"
                    + "Delay Category: " + (s.getDelayType().isEmpty() ? "Standard" : s.getDelayType()) + "\n"
                    + "Start Time: " + s.getStartTime() + "\n"
                    + "End Time: " + s.getEndTime() + "\n"
                    + "Calculated Duration: " + s.getDurationInDays() + " day(s)\n"
                    + "Duration Left: " + s.getDurationLeftInDays() + " day(s)\n\n"
                    + "Coordinates:\n" + coordsBuilder + "\n"
                    + "Public Comment:\n" + s.getComment();
            detailText.setText(details);
        });

        return scrollView;
    }
}