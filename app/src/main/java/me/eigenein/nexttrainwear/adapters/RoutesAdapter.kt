package me.eigenein.nexttrainwear.adapters

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.support.wearable.view.WearableRecyclerView
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.eigenein.nexttrainwear.Globals
import me.eigenein.nexttrainwear.R
import me.eigenein.nexttrainwear.api.JourneyOptionStatus
import me.eigenein.nexttrainwear.api.JourneyOptionsResponse
import me.eigenein.nexttrainwear.data.Route
import me.eigenein.nexttrainwear.utils.bundle
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Used to display possible routes.
 */
class RoutesAdapter : RecyclerView.Adapter<RoutesAdapter.ViewHolder>() {

    private val routes = ArrayList<Route>()

    private var usingLocation = false

    override fun getItemCount(): Int = routes.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(routes[position])
    override fun onViewRecycled(holder: ViewHolder) = holder.unbind()

    fun swap(usingLocation: Boolean, routes: Iterable<Route>) {
        this.usingLocation = usingLocation
        this.routes.clear()
        this.routes.addAll(routes)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val disposable = CompositeDisposable()
        private val adapter = JourneyOptionsAdapter()
        private val analytics = FirebaseAnalytics.getInstance(itemView.context)

        private val gpsStatusImageView: ImageView = itemView.findViewById(R.id.item_route_gps_status_image_view)
        private val journeyOptionsRecyclerView: WearableRecyclerView = itemView.findViewById(R.id.item_route_recycler_view)
        private val progressView: View = itemView.findViewById(R.id.item_route_progress_layout)
        private val departureTextView: TextView = itemView.findViewById(R.id.item_route_departure_text)
        private val destinationTextView: TextView = itemView.findViewById(R.id.item_route_destination_text)
        private val noTrainsView: View = itemView.findViewById(R.id.fragment_trains_no_trains_layout)
        private val noTrainsTextView: TextView = itemView.findViewById(R.id.fragment_trains_no_trains_text)


        init {
            journeyOptionsRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            journeyOptionsRecyclerView.adapter = adapter
            LinearSnapHelper().attachToRecyclerView(journeyOptionsRecyclerView)
        }

        fun bind(route: Route) {
            val response = Globals.JOURNEY_OPTIONS_RESPONSE_CACHE[route.key]
            if (response != null) {
                Log.d(LOG_TAG, "Cache hit!")
                onResponse(route, response)
            } else {
                Log.d(LOG_TAG, "Cache miss :(")
                showProgressLayout(route)
                planJourney(route)
            }
        }

        fun unbind() {
            disposable.clear()
        }

        private fun planJourney(route: Route) {
            disposable.add(
                Globals.NS_API.trainPlanner(route.departureStation.code, route.destinationStation.code)
                    .retryWhen { it.flatMap {
                        Log.w(LOG_TAG, "Train planner call failed", it)
                        if (it is HttpException || it is SocketTimeoutException || it is UnknownHostException)
                            Observable.timer(RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS)
                        else
                            Observable.error(it)
                    } }
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { onResponse(route, it) }
            )
            analytics.logEvent("call_train_planner", bundle {
                putString("departure_code", route.departureStation.code)
                putString("destination_code", route.destinationStation.code)
                putString("route_key", route.key)
            })
        }

        private fun onResponse(route: Route, response: JourneyOptionsResponse) {
            Globals.JOURNEY_OPTIONS_RESPONSE_CACHE.put(route.key, response, RESPONSE_TTL_MILLIS)

            // Exclude cancelled options.
            val journeyOptions = response.options.filter { it.status !in JourneyOptionStatus.HIDDEN }
            Log.d(LOG_TAG, "Possible options: " + journeyOptions.size)

            // Display journey options.
            progressView.visibility = View.GONE
            if (journeyOptions.isNotEmpty()) {
                adapter.swap(usingLocation, route, journeyOptions)
                noTrainsView.visibility = View.GONE
                journeyOptionsRecyclerView.visibility = View.VISIBLE
            } else {
                @Suppress("DEPRECATION")
                noTrainsTextView.text = Html.fromHtml(itemView.resources.getString(
                    R.string.fragment_trains_no_trains,
                    route.departureStation.longName,
                    route.destinationStation.longName
                ))
                journeyOptionsRecyclerView.visibility = View.GONE
                noTrainsView.visibility = View.VISIBLE
            }
        }

        private fun showProgressLayout(route: Route) {
            journeyOptionsRecyclerView.visibility = View.GONE
            noTrainsView.visibility = View.GONE

            gpsStatusImageView.visibility = if (usingLocation) View.GONE else View.VISIBLE
            departureTextView.text = route.departureStation.longName
            destinationTextView.text = route.destinationStation.longName
            progressView.visibility = View.VISIBLE
        }
    }

    companion object {
        private val LOG_TAG = RoutesAdapter::class.java.simpleName

        private const val RETRY_INTERVAL_SECONDS = 5L
        private const val RESPONSE_TTL_MILLIS = 60000L
    }
}
