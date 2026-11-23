package org.me.gcu.cw1_s2376160_traffic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SituationListFragment extends Fragment {
    private SituationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        TrafficViewModel viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        adapter = new SituationAdapter(viewModel::selectSituation);
        recyclerView.setAdapter(adapter);

        viewModel.getDisplayedSituations().observe(getViewLifecycleOwner(), adapter::updateList);
        return recyclerView;
    }
}