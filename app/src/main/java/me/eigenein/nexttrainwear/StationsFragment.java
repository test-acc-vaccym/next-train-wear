package me.eigenein.nexttrainwear;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StationsFragment extends Fragment {

    private WearableRecyclerView recyclerView;

    public StationsFragment() {
        // Do nothing.
    }

    public static StationsFragment newInstance() {
        final StationsFragment fragment = new StationsFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
        final LayoutInflater inflater,
        final ViewGroup container,
        final Bundle savedInstanceState
    ) {
        recyclerView = (WearableRecyclerView)inflater.inflate(R.layout.fragment_stations, container, false);
        return recyclerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        recyclerView.setAdapter(new StationsAdapter(
            StationCatalogue.newInstance(getActivity()),
            Preferences.getStations(getActivity())));
    }
}
