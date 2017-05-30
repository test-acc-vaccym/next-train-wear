package me.eigenein.nexttrainwear.fragments

import android.app.Fragment
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.wearable.view.WearableRecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import me.eigenein.nexttrainwear.Preferences
import me.eigenein.nexttrainwear.R
import me.eigenein.nexttrainwear.adapters.RoutesAdapter
import me.eigenein.nexttrainwear.data.Station
import me.eigenein.nexttrainwear.data.Stations
import me.eigenein.nexttrainwear.interfaces.AmbientListenable
import me.eigenein.nexttrainwear.interfaces.AmbientListener

class TrainsFragment : Fragment(), AmbientListener, LocationListener {

    private var apiClient: GoogleApiClient? = null
    private var ambientListenable: AmbientListenable? = null

    private lateinit var progressLayout: View
    private lateinit var destinationsRecyclerView: WearableRecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trains, container, false)

        progressLayout = view.findViewById(R.id.fragment_trains_progress_layout)

        destinationsRecyclerView = view.findViewById(R.id.fragment_trains_recycler_view) as WearableRecyclerView
        destinationsRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        LinearSnapHelper().attachToRecyclerView(destinationsRecyclerView)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        apiClient = GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnectionSuspended(i: Int) = Unit
                override fun onConnected(bundle: Bundle?) = onApiClientConnected()
            })
            .addOnConnectionFailedListener { connectionResult ->
                Log.e(logTag, "Connection failed: " + connectionResult)
                onDepartureStationChanged(null)
            }
            .build()
        ambientListenable = activity as AmbientListenable
    }

    override fun onStart() {
        super.onStart()
        // FIXME: show and hide "detecting location" progress.
        apiClient!!.connect()
        ambientListenable?.ambientListener = this
    }

    override fun onPause() {
        super.onPause()
        apiClient!!.disconnect()
        ambientListenable?.ambientListener = null
    }

    override fun onEnterAmbient() {
        // TODO: try to go with app theme changes.
    }

    override fun onUpdateAmbient() {
        // TODO: try to go with app theme changes.
    }

    override fun onExitAmbient() {
        // TODO: try to go with app theme changes.
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            Log.d(logTag, "Location changed: " + location)
            onDepartureStationChanged(Station.findNearestStation(location))
        } else {
            Log.e(logTag, "No location detected")
            onDepartureStationChanged(null)
        }
    }

    private fun onApiClientConnected() {
        // FIXME: show and hide "detecting location" progress.
        val request = LocationRequest.create().setNumUpdates(1)
        try {
            LocationServices.FusedLocationApi
                .requestLocationUpdates(apiClient, request, this)
                .setResultCallback {
                    if (it.status.isSuccess) {
                        Log.d(logTag, "Location request succeeded")
                    } else {
                        Log.e(logTag, "Location request failed: " + it.status)
                        onDepartureStationChanged(null)
                    }
                }
        } catch (e: SecurityException) {
            Log.e(logTag, "Forbidden to obtain last known location", e)
            onDepartureStationChanged(null)
        }
    }

    /**
     * Updates the fragment based on the departure station.
     */
    private fun onDepartureStationChanged(station: Station?) {
        Log.d(logTag, "Departure station: " + station)

        val departureStation = station
            ?: Stations.stationByCode[Preferences.getLastStationCode(activity)]
            ?: Stations.amsterdamCentraal
        Preferences.setLastStationCode(activity, departureStation.code)
        Log.d(logTag, "Set last station: " + departureStation)

        val destinations = selectDestinations(departureStation)
        Log.d(logTag, "Found destinations: " + destinations.size)

        // Show routes view.
        progressLayout.visibility = View.GONE
        destinationsRecyclerView.visibility = View.VISIBLE
        destinationsRecyclerView.adapter = RoutesAdapter(
            station != null, destinations.map { departureStation.routeTo(it) })
    }

    /**
     * Select destination stations to go to from the specified departure station.
     */
    private fun selectDestinations(departureStation: Station): List<Station> {
        val stationCodes = Preferences.getStations(activity)

        // Select favorite stations sorted by distance.
        val favoriteStations = stationCodes
            .filter { it != departureStation.code }
            .mapNotNull { Stations.stationByCode[it] }
            .sortedBy { departureStation.distanceTo(it.latitude, it.longitude) }

        // Select some other stations sorted by distance from current.
        val allStations = Stations.allStations
            .filter { it.code !in stationCodes && it.code != departureStation.code }
            .sortedBy { departureStation.distanceTo(it.latitude, it.longitude) }
            .take(numberOfNearestStations)

        return favoriteStations + allStations
    }

    companion object {

        private const val numberOfNearestStations = 10 // TODO

        private val logTag = TrainsFragment::class.java.simpleName
    }
}
