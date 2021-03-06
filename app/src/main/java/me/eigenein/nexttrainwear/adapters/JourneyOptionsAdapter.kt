package me.eigenein.nexttrainwear.adapters

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import me.eigenein.nexttrainwear.R
import me.eigenein.nexttrainwear.api.JourneyOption
import me.eigenein.nexttrainwear.api.JourneyOptionStatus
import me.eigenein.nexttrainwear.data.Route
import me.eigenein.nexttrainwear.utils.hide
import me.eigenein.nexttrainwear.utils.minus
import me.eigenein.nexttrainwear.utils.show
import java.text.SimpleDateFormat
import java.util.*

/**
 * Used to display possible journey options.
 */
class JourneyOptionsAdapter : RecyclerView.Adapter<JourneyOptionsAdapter.ViewHolder>() {

    private val journeyOptions = arrayListOf<JourneyOption>()

    private var usingLocation = false

    private lateinit var route: Route

    override fun getItemCount(): Int = journeyOptions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_journey_option, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(journeyOptions[position])

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.refreshCountDown()
    }

    operator fun get(position: Int) = journeyOptions[position]

    fun swap(usingLocation: Boolean, route: Route, journeyOptions: Iterable<JourneyOption>) {
        Log.i(LOG_TAG, "Swap")
        this.usingLocation = usingLocation
        this.route = route
        this.journeyOptions.clear()
        this.journeyOptions.addAll(journeyOptions)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val gpsStatusImageView: ImageView = itemView.findViewById(R.id.item_journey_option_gps_status_image_view)
        private val departureStationTextView: TextView = itemView.findViewById(R.id.item_journey_option_departure_station_text)
        private val destinationStationTextView: TextView = itemView.findViewById(R.id.item_journey_option_destination_station_text)
        private val departureTimeTextView: TextView = itemView.findViewById(R.id.item_journey_option_departure_time_text)
        private val arrivalTimeTextView: TextView = itemView.findViewById(R.id.item_journey_option_arrival_time_text)
        private val durationTimeTextView: TextView = itemView.findViewById(R.id.item_journey_option_duration_text)
        private val countdownTextView: TextView = itemView.findViewById(R.id.item_journey_option_countdown_text)
        private val platformTitleTextView: View = itemView.findViewById(R.id.item_journey_option_platform_title)
        private val platformTextView: TextView = itemView.findViewById(R.id.item_journey_option_platform_text)

        private lateinit var journeyOption: JourneyOption

        fun bind(journeyOption: JourneyOption) {
            this.journeyOption = journeyOption

            gpsStatusImageView.visibility = if (usingLocation) View.GONE else View.VISIBLE
            departureStationTextView.text = route.departureStation.longName
            destinationStationTextView.text = route.destinationStation.longName

            val platform = journeyOption.components.getOrNull(0)?.stops?.getOrNull(0)?.platform
            if (platform != null) {
                platformTextView.text = platform
                platformTitleTextView.show()
                platformTextView.show()
            } else {
                platformTitleTextView.hide()
                platformTextView.hide()
            }

            departureTimeTextView.text = CLOCK_TIME_FORMAT.format(journeyOption.actualDepartureTime)
            arrivalTimeTextView.text = CLOCK_TIME_FORMAT.format(journeyOption.actualArrivalTime)
            durationTimeTextView.text = journeyOption.actualDuration
            countdownTextView.setTextColor(if (journeyOption.status != JourneyOptionStatus.DELAYED) WHITE else RED_ACCENT)
        }

        fun refreshCountDown(): Long {
            val millis = journeyOption.actualDepartureTime - Date()
            countdownTextView.text = toClockString(millis)
            return millis
        }

        private fun toClockString(millis: Long): String {
            val (absMillis, sign) = if (millis >= 0) Pair(millis, "") else Pair(-millis, "-")
            val hours = absMillis / 3_600_000
            val minutes = (absMillis % 3_600_000) / 60_000
            val seconds = (absMillis % 60_000) / 1_000
            return if (hours == 0L)
                String.format("%s%d:%02d", sign, minutes, seconds)
            else
                String.format("%s%d:%02d:%02d", sign, hours, minutes, seconds)
        }
    }

    companion object {
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val RED_ACCENT = 0xFFFF8880.toInt()

        private val LOG_TAG = JourneyOptionsAdapter::class.java.simpleName
        private val CLOCK_TIME_FORMAT = SimpleDateFormat("H:mm", Locale.ENGLISH)
    }
}
